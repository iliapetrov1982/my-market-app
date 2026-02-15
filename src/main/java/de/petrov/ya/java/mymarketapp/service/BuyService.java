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

@Service
@RequiredArgsConstructor
public class BuyService {

    private final CartItemRepository cartItemRepository;
    private final ItemRepository itemRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository; // новый
    private final TransactionalOperator tx;

    public Mono<Long> buy() {
        return cartItemRepository.findAll()
                .collectList()
                .flatMap(cartItems -> {
                    if (cartItems.isEmpty()) {
                        return Mono.error(new IllegalStateException("Cart is empty"));
                    }

                    var itemIds = cartItems.stream().map(CartItem::getItemId).toList();

                    return itemRepository.findAllById(itemIds)
                            .collectMap(Item::getId)
                            .flatMap(itemsMap -> {

                                Order order = new Order(0L); // createdAt выставится в конструкторе

                                return orderRepository.save(order)
                                        .flatMap(savedOrder -> {

                                            long orderId = savedOrder.getId();

                                            long totalSum = cartItems.stream()
                                                    .mapToLong(ci -> {
                                                        Item item = itemsMap.get(ci.getItemId());
                                                        return item.getPrice() * (long) ci.getQuantity();
                                                    })
                                                    .sum();

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
                                                    .then(orderRepository.save(updateTotal(savedOrder, totalSum)))
                                                    .then(cartItemRepository.deleteAll())
                                                    .thenReturn(orderId);
                                        });
                            });
                })
                .as(tx::transactional);
    }

    private Order updateTotal(Order order, long total) {
        order.setTotalSum(total);
        if (order.getCreatedAt() == null) {
            order.setCreatedAt(java.time.OffsetDateTime.now());
        }
        return order;
    }
}
