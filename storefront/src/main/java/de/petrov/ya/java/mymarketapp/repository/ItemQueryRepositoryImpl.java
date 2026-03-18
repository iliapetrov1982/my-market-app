package de.petrov.ya.java.mymarketapp.repository;

import de.petrov.ya.java.mymarketapp.dto.page.ItemDto;
import de.petrov.ya.java.mymarketapp.dto.page.ItemsSort;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public class ItemQueryRepositoryImpl implements ItemQueryRepository {

    private final DatabaseClient db;

    public ItemQueryRepositoryImpl(DatabaseClient db) {
        this.db = db;
    }

    @Override
    public Flux<ItemDto> findShowcase(String q, ItemsSort sort, int limit, int offset, String username) {
        String qq = (q == null) ? "" : q.trim();

        String orderBy = switch (sort) {
            case ALPHA -> "order by i.title asc, i.id asc";
            case PRICE -> "order by i.price asc, i.id asc";
            case NO -> "order by i.id asc";
        };

        String sql = ("""
        select
          i.id                         as id,
          i.title                      as title,
          i.description                as description,
          i.img_path                   as img_path,
          i.price                      as price,
          coalesce(ci.quantity, 0)     as count
        from items i
        left join cart_items ci on ci.item_id = i.id and ci.username = :username
        where (:q = '' or
               lower(i.title) like lower(concat('%%', :q, '%%')) or
               lower(i.description) like lower(concat('%%', :q, '%%')))
        %s
        limit :limit offset :offset
        """).formatted(orderBy);

        return db.sql(sql)
                .bind("q", qq)
                .bind("username", username)
                .bind("limit", limit)
                .bind("offset", offset)
                .map((row, meta) -> mapItemDto(row))
                .all();
    }

    @Override
    public Mono<Long> countShowcase(String q) {
        String qq = (q == null) ? "" : q.trim();

        String sql = """
        select count(*) as cnt
        from items i
        where (:q = '' or
               lower(i.title) like lower(concat('%', :q, '%')) or
               lower(i.description) like lower(concat('%', :q, '%')))
        """;

        return db.sql(sql)
                .bind("q", qq)
                .map((row, meta) -> {
                    Number n = row.get("cnt", Number.class);
                    return n == null ? 0L : n.longValue();
                })
                .one()
                .defaultIfEmpty(0L);
    }

    @Override
    public Mono<ItemDto> findItemPage(long id, String username) {
        String sql = """
        select
          i.id                         as id,
          i.title                      as title,
          i.description                as description,
          i.img_path                   as img_path,
          i.price                      as price,
          coalesce(ci.quantity, 0)     as count
        from items i
        left join cart_items ci on ci.item_id = i.id and ci.username = :username
        where i.id = :id
        """;

        return db.sql(sql)
                .bind("id", id)
                .bind("username", username)
                .map((row, meta) -> mapItemDto(row))
                .one();
    }

    @Override
    public Flux<ItemDto> findCartItems(String username) {
        String sql = """
        select
          i.id                         as id,
          i.title                      as title,
          i.description                as description,
          i.img_path                   as img_path,
          i.price                      as price,
          ci.quantity                  as count
        from cart_items ci
        join items i on i.id = ci.item_id
        where ci.username = :username
        order by i.id
        """;

        return db.sql(sql)
                .bind("username", username)
                .map((row, meta) -> mapItemDto(row))
                .all();
    }

    // -------------------------------------------------------------------------
    // helpers
    // -------------------------------------------------------------------------

    private static ItemDto mapItemDto(io.r2dbc.spi.Row row) {
        Number countNum = row.get("count", Number.class);
        int count = (countNum == null) ? 0 : countNum.intValue();

        Long itemId = row.get("id", Long.class);
        Long price = row.get("price", Long.class);

        return new ItemDto(
                itemId == null ? -1L : itemId,
                row.get("title", String.class),
                row.get("description", String.class),
                row.get("img_path", String.class),
                price == null ? 0L : price,
                count
        );
    }
}
