package de.petrov.ya.java.mymarketapp.repository;

import de.petrov.ya.java.mymarketapp.entity.order.OrderItem;
import lombok.RequiredArgsConstructor;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
@RequiredArgsConstructor
public class OrderItemRepository {

    private final DatabaseClient db;

    public Flux<OrderItem> saveAll(Flux<OrderItem> items) {
        return items.flatMap(this::insert);
    }

    private Mono<OrderItem> insert(OrderItem oi) {
        String sql = """
            insert into order_items(order_id, item_id, title, price, quantity)
            values (:orderId, :itemId, :title, :price, :quantity)
            """;

        return db.sql(sql)
                .bind("orderId", oi.getOrderId())
                .bind("itemId", oi.getItemId())
                .bind("title", oi.getTitle())
                .bind("price", oi.getPrice())
                .bind("quantity", oi.getQuantity())
                .then()
                .thenReturn(oi);
    }

    public Flux<OrderItem> findAllByOrderId(Long orderId) {
        String sql = """
            select order_id, item_id, title, price, quantity
            from order_items
            where order_id = :orderId
            order by item_id
            """;

        return db.sql(sql)
                .bind("orderId", orderId)
                .map((row, meta) -> new OrderItem(
                        row.get("order_id", Long.class),
                        row.get("item_id", Long.class),
                        row.get("title", String.class),
                        row.get("price", Long.class),
                        row.get("quantity", Integer.class)
                ))
                .all();
    }
}
