package de.petrov.ya.java.mymarketapp.service;

import de.petrov.ya.java.mymarketapp.dto.order.OrderDto;
import de.petrov.ya.java.mymarketapp.dto.order.OrderItemDto;
import de.petrov.ya.java.mymarketapp.repository.OrderRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class OrdersService {

    private final OrderRepository orderRepository;

    public OrdersService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Transactional(readOnly = true)
    public List<OrderDto> getOrders() {
        var orders = orderRepository.findAllWithItems();

        return orders.stream()
                .map(o -> {
                    var items = o.getItems().stream()
                            .map(oi -> new OrderItemDto(
                                    oi.getItem().getId(),
                                    oi.getItem().getTitle(),
                                    oi.getPrice(),
                                    oi.getQuantity()
                            ))
                            .toList();

                    long total = o.getItems().stream()
                            .mapToLong(oi -> (long) oi.getQuantity() * oi.getPrice())
                            .sum();

                    return new OrderDto(o.getId(), items, total);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderDto getOrder(long id) {
        var order = orderRepository.findByIdWithItems(id)
                .orElseThrow(() -> new EntityNotFoundException("Order not found: " + id));

        var items = order.getItems().stream()
                .map(oi -> new OrderItemDto(
                        oi.getItem().getId(),
                        oi.getItem().getTitle(),
                        oi.getPrice(),
                        oi.getQuantity()
                ))
                .toList();

        long total = order.getItems().stream()
                .mapToLong(oi -> (long) oi.getQuantity() * oi.getPrice())
                .sum();

        return new OrderDto(order.getId(), items, total);
    }
}
