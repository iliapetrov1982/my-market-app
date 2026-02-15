package de.petrov.ya.java.mymarketapp.repository;

import de.petrov.ya.java.mymarketapp.entity.CartItem;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface CartItemRepository extends ReactiveCrudRepository<CartItem, Long> {

}
