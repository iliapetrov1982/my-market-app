package de.petrov.ya.java.mymarketapp.repository;

import de.petrov.ya.java.mymarketapp.entity.order.Order;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface OrderRepository extends ReactiveCrudRepository<Order, Long> {

    Flux<Order> findAllByUsernameOrderByIdDesc(String username);

    Mono<Order> findByIdAndUsername(long id, String username);
}
