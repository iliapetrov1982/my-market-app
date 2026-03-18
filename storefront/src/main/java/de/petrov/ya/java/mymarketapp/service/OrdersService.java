package de.petrov.ya.java.mymarketapp.service;

import de.petrov.ya.java.mymarketapp.dto.order.OrderDto;
import de.petrov.ya.java.mymarketapp.dto.order.OrderItemDto;
import de.petrov.ya.java.mymarketapp.entity.order.Order;
import de.petrov.ya.java.mymarketapp.exception.EntityNotFoundException;
import de.petrov.ya.java.mymarketapp.repository.OrderItemRepository;
import de.petrov.ya.java.mymarketapp.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class OrdersService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    public Flux<OrderDto> getOrders() {
        return currentUsername()
                .flatMapMany(username ->
                        orderRepository.findAllByUsernameOrderByIdDesc(username)
                                .flatMap(this::mapToDto)
                );
    }

    public Mono<OrderDto> getOrder(long id) {
        return currentUsername()
                .flatMap(username ->
                        orderRepository.findByIdAndUsername(id, username)
                                .switchIfEmpty(Mono.error(new EntityNotFoundException(
                                        "Order not found: " + id
                                )))
                                .flatMap(this::mapToDto)
                );
    }

    private Mono<OrderDto> mapToDto(Order order) {
        return orderItemRepository.findAllByOrderId(order.getId())
                .map(oi -> new OrderItemDto(
                        oi.getItemId(),
                        oi.getTitle(),
                        oi.getPrice(),
                        oi.getQuantity()
                ))
                .collectList()
                .map(items -> {
                    long total = items.stream()
                            .mapToLong(i -> i.price() * (long) i.count())
                            .sum();
                    return new OrderDto(order.getId(), items, total);
                });
    }

    private static Mono<String> currentUsername() {
        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> ctx.getAuthentication().getName());
    }
}
