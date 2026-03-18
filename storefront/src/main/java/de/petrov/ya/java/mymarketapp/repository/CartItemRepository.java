package de.petrov.ya.java.mymarketapp.repository;

import de.petrov.ya.java.mymarketapp.entity.CartItem;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface CartItemRepository extends ReactiveCrudRepository<CartItem, Long> {

    Flux<CartItem> findAllByUsername(String username);

    @Query("SELECT * FROM cart_items WHERE item_id = :itemId AND username = :username")
    Mono<CartItem> findByItemIdAndUsername(long itemId, String username);

    Mono<Void> deleteAllByUsername(String username);
}
