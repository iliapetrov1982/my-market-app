package de.petrov.ya.java.mymarketapp.service;

import de.petrov.ya.java.mymarketapp.entity.order.Order;
import de.petrov.ya.java.mymarketapp.entity.order.OrderItem;
import de.petrov.ya.java.mymarketapp.entity.order.OrderItemId;
import de.petrov.ya.java.mymarketapp.repository.CartItemRepository;
import de.petrov.ya.java.mymarketapp.repository.OrderRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class BuyService {

    private final CartItemRepository cartItemRepository;
    private final OrderRepository orderRepository;

    @Transactional
    public long buy() {
        var cartItems = cartItemRepository.findAll();

        if (cartItems.isEmpty()) {
            throw new IllegalStateException("Cart is empty");
        }

        // создаём заказ сразу с totalSum=0 (NOT NULL)
        Order order = new Order();
        order.setCreatedAt(OffsetDateTime.now());
        order.setTotalSum(0L);
        order = orderRepository.save(order);

        long totalSum = 0L;

        // переносим товары: фиксируем snapshot title/price и quantity
        for (var ci : cartItems) {
            var item = ci.getItem();

            OrderItem oi = new OrderItem();
            oi.setId(new OrderItemId(order.getId(), item.getId())); // ✅ теперь тип совпадает
            oi.setOrder(order);
            oi.setItem(item);

            oi.setTitle(item.getTitle());          // ✅ иначе падало по NOT NULL title
            oi.setPrice(item.getPrice());          // ✅ snapshot цены
            oi.setQuantity(ci.getQuantity());

            // добавляем в коллекцию заказа (cascade сохранит order_items)
            order.addItem(oi);

            totalSum += item.getPrice() * (long) ci.getQuantity();
        }

        // обновляем сумму заказа (NOT NULL total_sum)
        order.setTotalSum(totalSum);

        // чистим корзину
        cartItemRepository.deleteAll();

        // из-за @Transactional всё сохранится одним коммитом
        return order.getId();
    }
}
