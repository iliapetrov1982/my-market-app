package de.petrov.ya.java.mymarketapp.service;

import de.petrov.ya.java.mymarketapp.entity.CartItem;
import de.petrov.ya.java.mymarketapp.entity.Item;
import de.petrov.ya.java.mymarketapp.entity.order.Order;
import de.petrov.ya.java.mymarketapp.entity.order.OrderItem;
import de.petrov.ya.java.mymarketapp.repository.CartItemRepository;
import de.petrov.ya.java.mymarketapp.repository.ItemRepository;
import de.petrov.ya.java.mymarketapp.repository.OrderItemRepository;
import de.petrov.ya.java.mymarketapp.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BuyService {

    private final CartItemRepository cartItemRepository;
    private final ItemRepository itemRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final PaymentsGateway paymentsGateway;
    private final TransactionalOperator tx;

    public Mono<Long> buy() {
        return cartItemRepository.findAll()
                .collectList()
                .flatMap(cartItems -> {
                    if (cartItems.isEmpty()) {
                        return Mono.error(new IllegalStateException("Cart is empty"));
                    }

                    var itemIds = cartItems.stream()
                            .map(CartItem::getItemId)
                            .toList();

                    return itemRepository.findAllById(itemIds)
                            .collectList()
                            .flatMap(items -> {
                                Map<Long, Item> itemsMap = items.stream()
                                        .collect(Collectors.toMap(Item::getId, Function.identity()));

                                long totalSum = cartItems.stream()
                                        .mapToLong(ci -> {
                                            Item item = itemsMap.get(ci.getItemId());
                                            if (item == null) {
                                                throw new IllegalStateException("Item not found: " + ci.getItemId());
                                            }
                                            return item.getPrice() * (long) ci.getQuantity();
                                        })
                                        .sum();

                                return paymentsGateway.charge(totalSum)
                                        .flatMap(success -> {
                                            if (!success) {
                                                return Mono.error(new IllegalStateException("Payment failed"));
                                            }

                                            Order order = new Order(totalSum);

                                            return orderRepository.save(order)
                                                    .flatMap(savedOrder -> {
                                                        long orderId = savedOrder.getId();

                                                        Flux<OrderItem> orderItemsFlux = Flux.fromIterable(cartItems)
                                                                .map(ci -> {
                                                                    Item item = itemsMap.get(ci.getItemId());
                                                                    return new OrderItem(
                                                                            orderId,
                                                                            item.getId(),
                                                                            item.getTitle(),
                                                                            item.getPrice(),
                                                                            ci.getQuantity()
                                                                    );
                                                                });

                                                        return orderItemRepository.saveAll(orderItemsFlux)
                                                                .then(cartItemRepository.deleteAll())
                                                                .thenReturn(orderId);
                                                    });
                                        });
                            });
                })
                .as(tx::transactional);
    }
}