package de.petrov.ya.java.mymarketapp.repository;

import de.petrov.ya.java.mymarketapp.entity.Item;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface ItemRepository extends ReactiveCrudRepository<Item, Long> {
}
