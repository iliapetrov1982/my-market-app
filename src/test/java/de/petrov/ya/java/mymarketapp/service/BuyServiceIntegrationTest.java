package de.petrov.ya.java.mymarketapp.service;

import de.petrov.ya.java.mymarketapp.MyMarketAppApplicationTests;
import de.petrov.ya.java.mymarketapp.entity.CartItem;
import de.petrov.ya.java.mymarketapp.entity.Item;
import de.petrov.ya.java.mymarketapp.entity.order.Order;
import de.petrov.ya.java.mymarketapp.entity.order.OrderItem;
import de.petrov.ya.java.mymarketapp.repository.CartItemRepository;
import de.petrov.ya.java.mymarketapp.repository.ItemRepository;
import de.petrov.ya.java.mymarketapp.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

import java.util.Comparator;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BuyServiceIntegrationTest extends MyMarketAppApplicationTests {

    @Autowired
    BuyService buyService;

    @Autowired
    ItemRepository itemRepository;

    @Autowired
    CartItemRepository cartItemRepository;

    @Autowired
    OrderRepository orderRepository;

    @BeforeEach
    void cleanState() {
        // тесты не должны зависеть от порядка запуска
        cartItemRepository.deleteAll();
        // orders/order_items лучше чистить для изоляции
        orderRepository.deleteAll();
    }

    @Test
    void buy_whenCartIsEmpty_throwsIllegalStateException_andNothingCreated() {
        assertThat(cartItemRepository.count(), is(0L));

        var ex = assertThrows(IllegalStateException.class, () -> buyService.buy());
        assertThat(ex.getMessage(), equalTo("Cart is empty"));

        assertThat("Заказ не должен создаваться", orderRepository.count(), is(0L));
    }

    @Test
    void buy_whenCartHasItems_createsOrderAndOrderItems_clearsCart_totalSumIsCorrect() {
        // arrange: берём 2 товара из seed
        List<Item> items = itemRepository.findAll(PageRequest.of(0, 2)).getContent();
        assertThat("Seed должен содержать хотя бы 2 товара", items.size(), is(2));

        Item i1 = items.get(0);
        Item i2 = items.get(1);

        int q1 = 2;
        int q2 = 3;

        cartItemRepository.saveAll(List.of(
                new CartItem(i1, q1),
                new CartItem(i2, q2)
        ));

        assertThat("Корзина должна быть заполнена перед покупкой", cartItemRepository.count(), is(2L));

        long expectedTotal = i1.getPrice() * (long) q1 + i2.getPrice() * (long) q2;

        // act
        long orderId = buyService.buy();

        // assert: корзина очищена
        assertThat("После buy() корзина должна быть очищена", cartItemRepository.count(), is(0L));

        // assert: заказ существует
        Order order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new AssertionError("Order not found by id: " + orderId));

        assertThat(order.getId(), equalTo(orderId));
        assertThat(order.getCreatedAt(), notNullValue());
        assertThat(order.getTotalSum(), equalTo(expectedTotal));

        // assert: позиции заказа
        assertThat(order.getItems(), notNullValue());
        assertThat("Должно быть 2 позиции заказа", order.getItems().size(), is(2));

        // стабильнее сравнивать в отсортированном виде
        List<OrderItem> orderItems = order.getItems().stream()
                .sorted(Comparator.comparing(oi -> oi.getItem().getId()))
                .toList();

        // для item 1
        OrderItem oi1 = orderItems.stream()
                .filter(oi -> oi.getItem().getId().equals(i1.getId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("OrderItem for item " + i1.getId() + " not found"));

        assertThat(oi1.getTitle(), equalTo(i1.getTitle()));
        assertThat(oi1.getPrice(), equalTo(i1.getPrice()));
        assertThat(oi1.getQuantity(), equalTo(q1));
        assertThat("PK(orderId,itemId) должен содержать orderId", oi1.getId().getOrderId(), equalTo(orderId));
        assertThat("PK(orderId,itemId) должен содержать itemId", oi1.getId().getItemId(), equalTo(i1.getId()));

        // для item 2
        OrderItem oi2 = orderItems.stream()
                .filter(oi -> oi.getItem().getId().equals(i2.getId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("OrderItem for item " + i2.getId() + " not found"));

        assertThat(oi2.getTitle(), equalTo(i2.getTitle()));
        assertThat(oi2.getPrice(), equalTo(i2.getPrice()));
        assertThat(oi2.getQuantity(), equalTo(q2));
        assertThat(oi2.getId().getOrderId(), equalTo(orderId));
        assertThat(oi2.getId().getItemId(), equalTo(i2.getId()));
    }

    @Test
    void buy_createsNewOrderEachTime() {
        // arrange: один товар из seed
        Item i = itemRepository.findAll(PageRequest.of(0, 1)).getContent().getFirst();

        cartItemRepository.save(new CartItem(i, 1));
        long id1 = buyService.buy();

        cartItemRepository.save(new CartItem(i, 2));
        long id2 = buyService.buy();

        assertThat("Должны быть разные заказы", id2, not(equalTo(id1)));
        assertThat("Должно быть 2 заказа", orderRepository.count(), is(2L));
        assertThat("Корзина должна быть пуста", cartItemRepository.count(), is(0L));
    }
}
