package de.petrov.ya.java.mymarketapp.repository;

import de.petrov.ya.java.mymarketapp.dto.page.ItemDto;
import de.petrov.ya.java.mymarketapp.entity.Item;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ItemRepository extends JpaRepository<Item, Long> {

    @Query("""
        select new de.petrov.ya.java.mymarketapp.dto.page.ItemDto(
            i.id, i.title, i.description, i.imgPath, i.price,
            coalesce(ci.quantity, 0)
        )
        from Item i
        left join CartItem ci on ci.item = i
        where (:q is null or :q = ''
            or lower(i.title) like lower(concat('%', :q, '%'))
            or lower(i.description) like lower(concat('%', :q, '%'))
        )
        """)
    Page<ItemDto> findShowcase(@Param("q") String q, Pageable pageable);

    @Query("""
        select new de.petrov.ya.java.mymarketapp.dto.page.ItemDto(
            i.id, i.title, i.description, i.imgPath, i.price,
            coalesce(ci.quantity, 0)
        )
        from Item i
        left join CartItem ci on ci.item = i
        where i.id = :id
        """)
    Optional<ItemDto> findItemPage(@Param("id") long id);

    @Query("""
        select new de.petrov.ya.java.mymarketapp.dto.page.ItemDto(
            i.id, i.title, i.description, i.imgPath, i.price,
            ci.quantity
        )
        from CartItem ci
        join ci.item i
        order by i.id
        """)
    List<ItemDto> findCartItems();
}
