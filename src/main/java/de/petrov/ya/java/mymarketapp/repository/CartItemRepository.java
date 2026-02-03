package de.petrov.ya.java.mymarketapp.repository;

import de.petrov.ya.java.mymarketapp.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartItemRepository extends JpaRepository<CartItem, Long>{

}
