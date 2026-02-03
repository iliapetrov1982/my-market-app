package de.petrov.ya.java.mymarketapp.repository;

import de.petrov.ya.java.mymarketapp.entity.order.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    @Query("""
        select distinct o
        from Order o
        left join fetch o.items oi
        left join fetch oi.item i
        order by o.id desc
        """)
    List<Order> findAllWithItems();

    @Query("""
        select o
        from Order o
        left join fetch o.items oi
        left join fetch oi.item i
        where o.id = :id
        """)
    Optional<Order> findByIdWithItems(long id);
}
