package de.petrov.ya.java.mymarketapp.service;

import de.petrov.ya.java.mymarketapp.MyMarketAppApplicationTests;
import de.petrov.ya.java.mymarketapp.entity.CartItem;
import de.petrov.ya.java.mymarketapp.entity.Item;
import de.petrov.ya.java.mymarketapp.entity.order.Order;
import de.petrov.ya.java.mymarketapp.entity.order.OrderItem;
import de.petrov.ya.java.mymarketapp.repository.CartItemRepository;
import de.petrov.ya.java.mymarketapp.repository.ItemRepository;
import de.petrov.ya.java.mymarketapp.repository.OrderItemRepository;
import de.petrov.ya.java.mymarketapp.repository.OrderRepository;
import java.util.Comparator;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.r2dbc.core.DatabaseClient;
import reactor.test.StepVerifier;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

class BuyServiceIntegrationTest extends MyMarketAppApplicationTests {

    @Autowired
    private BuyService buyService;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private DatabaseClient db;

    @BeforeEach
    void cleanState() {
        // FK: order_items -> orders, поэтому сначала чистим order_items
        db.sql("delete from order_items").then().block();
        db.sql("delete from orders").then().block();
        cartItemRepository.deleteAll().block();
    }

    @Test
    void buy_whenCartIsEmpty_throwsIllegalStateException_andNothingCreated() {
        Long cartCount = cartItemRepository.count().block();
        assertThat(cartCount, is(0L));

        StepVerifier.create(buyService.buy())
                .expectErrorMatches(ex ->
                        ex instanceof IllegalStateException
                        && "Cart is empty".equals(ex.getMessage())
                )
                .verify();

        Long ordersCount = orderRepository.count().block();
        assertThat("Заказ не должен создаваться", ordersCount, is(0L));

        Long orderItemsCount = db.sql("select count(*) as c from order_items")
                .map((row, meta) -> row.get("c", Long.class))
                .one()
                .block();
        assertThat("Позиции заказа не должны создаваться", orderItemsCount, is(0L));
    }

    @Test
    void buy_whenCartHasItems_createsOrderAndOrderItems_clearsCart_totalSumIsCorrect() {
        // arrange: берём 2 товара из seed
        List<Item> items = itemRepository.findAll()
                .take(2)
                .collectList()
                .block();

        assertThat("Seed должен содержать хотя бы 2 товара", items, notNullValue());
        assertThat(items.size(), is(2));

        Item i1 = items.get(0);
        Item i2 = items.get(1);

        int q1 = 2;
        int q2 = 3;

        // ВАЖНО: CartItem implements Persistable, и обычный new CartItem(...) имеет isNew=false,
        // поэтому Spring Data делает UPDATE вместо INSERT.
        cartItemRepository.saveAll(List.of(
                CartItem.newRow(i1.getId(), q1),
                CartItem.newRow(i2.getId(), q2)
        )).then().block();

        Long cartBefore = cartItemRepository.count().block();
        assertThat("Корзина должна быть заполнена перед покупкой", cartBefore, is(2L));

        long expectedTotal = i1.getPrice() * (long) q1 + i2.getPrice() * (long) q2;

        // act
        Long orderId = buyService.buy().block();
        assertThat(orderId, notNullValue());

        // assert: корзина очищена
        Long cartAfter = cartItemRepository.count().block();
        assertThat("После buy() корзина должна быть очищена", cartAfter, is(0L));

        // assert: заказ существует
        Order order = orderRepository.findById(orderId).block();
        assertThat(order, notNullValue());
        assertThat(order.getId(), equalTo(orderId));
        assertThat(order.getCreatedAt(), notNullValue());
        assertThat(order.getTotalSum(), equalTo(expectedTotal));

        // assert: позиции заказа (через кастомный репозиторий)
        List<OrderItem> orderItems = orderItemRepository.findAllByOrderId(orderId)
                .collectList()
                .block();

        assertThat(orderItems, notNullValue());
        assertThat("Должно быть 2 позиции заказа", orderItems.size(), is(2));

        // стабильность: сортируем по itemId
        List<OrderItem> sorted = orderItems.stream()
                .sorted(Comparator.comparing(OrderItem::getItemId))
                .toList();

        OrderItem oi1 = sorted.stream()
                .filter(oi -> oi.getItemId().equals(i1.getId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("OrderItem for item " + i1.getId() + " not found"));

        assertThat(oi1.getTitle(), equalTo(i1.getTitle()));
        assertThat(oi1.getPrice(), equalTo(i1.getPrice()));
        assertThat(oi1.getQuantity(), equalTo(q1));
        assertThat(oi1.getId().getOrderId(), equalTo(orderId));
        assertThat(oi1.getId().getItemId(), equalTo(i1.getId()));

        OrderItem oi2 = sorted.stream()
                .filter(oi -> oi.getItemId().equals(i2.getId()))
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
        Item item = itemRepository.findAll()
                .next()
                .block();
        assertThat(item, notNullValue());

        cartItemRepository.save(CartItem.newRow(item.getId(), 1)).block();
        Long id1 = buyService.buy().block();
        assertThat(id1, notNullValue());

        cartItemRepository.save(CartItem.newRow(item.getId(), 2)).block();
        Long id2 = buyService.buy().block();
        assertThat(id2, notNullValue());

        assertThat("Должны быть разные заказы", id2, not(equalTo(id1)));

        Long ordersCount = orderRepository.count().block();
        assertThat("Должно быть 2 заказа", ordersCount, is(2L));

        Long cartCount = cartItemRepository.count().block();
        assertThat("Корзина должна быть пуста", cartCount, is(0L));

        Long orderItemsCount = db.sql("select count(*) as c from order_items")
                .map((row, meta) -> row.get("c", Long.class))
                .one()
                .block();
        assertThat("Должно быть 2 строки в order_items", orderItemsCount, is(2L));
    }
}
