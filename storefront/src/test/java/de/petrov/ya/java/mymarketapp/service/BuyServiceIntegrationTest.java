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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Comparator;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

class BuyServiceIntegrationTest extends MyMarketAppApplicationTests {

    private static final String USERNAME = "user1";

    @Autowired private BuyService buyService;
    @Autowired private ItemRepository itemRepository;
    @Autowired private CartItemRepository cartItemRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private OrderItemRepository orderItemRepository;
    @Autowired private DatabaseClient db;
    @MockitoBean  private PaymentsGateway paymentsGateway;

    // -------------------------------------------------------------------------
    // helpers
    // -------------------------------------------------------------------------

    /** Подставляет SecurityContext с USERNAME и вызывает buy() как блокирующий вызов. */
    private Long buyBlocking() {
        var auth = UsernamePasswordAuthenticationToken
                .authenticated(USERNAME, null, List.of());
        return buyService.buy()
                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth))
                .block();
    }

    /** StepVerifier обёртка для buy() с SecurityContext. */
    private StepVerifier.FirstStep<Long> buyVerifier() {
        var auth = UsernamePasswordAuthenticationToken
                .authenticated(USERNAME, null, List.of());
        return StepVerifier.create(
                buyService.buy()
                        .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth))
        );
    }

    @BeforeEach
    void cleanState() {
        when(paymentsGateway.charge(anyLong())).thenReturn(Mono.just(true));

        db.sql("delete from order_items").then().block();
        db.sql("delete from orders").then().block();
        db.sql("delete from cart_items").then().block();
    }

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------

    @Test
    void buy_whenCartIsEmpty_throwsIllegalStateException_andNothingCreated() {
        assertThat(cartItemRepository.count().block(), is(0L));

        buyVerifier()
                .expectErrorMatches(ex ->
                        ex instanceof IllegalStateException
                        && "Cart is empty".equals(ex.getMessage()))
                .verify();

        assertThat("Заказ не должен создаваться", orderRepository.count().block(), is(0L));

        Long orderItemsCount = db.sql("select count(*) as c from order_items")
                .map((row, meta) -> row.get("c", Long.class)).one().block();
        assertThat("Позиции заказа не должны создаваться", orderItemsCount, is(0L));
    }

    @Test
    void buy_whenCartHasItems_createsOrderAndOrderItems_clearsCart_totalSumIsCorrect() {
        List<Item> items = itemRepository.findAll().take(2).collectList().block();
        assertThat("Seed должен содержать хотя бы 2 товара", items, notNullValue());
        assertThat(items.size(), is(2));

        Item i1 = items.get(0);
        Item i2 = items.get(1);
        int q1 = 2, q2 = 3;

        cartItemRepository.saveAll(List.of(
                CartItem.newRow(i1.getId(), q1, USERNAME),
                CartItem.newRow(i2.getId(), q2, USERNAME)
        )).then().block();

        assertThat("Корзина должна быть заполнена перед покупкой",
                cartItemRepository.count().block(), is(2L));

        long expectedTotal = i1.getPrice() * (long) q1 + i2.getPrice() * (long) q2;

        Long orderId = buyBlocking();
        assertThat(orderId, notNullValue());

        assertThat("После buy() корзина должна быть очищена",
                cartItemRepository.count().block(), is(0L));

        Order order = orderRepository.findById(orderId).block();
        assertThat(order, notNullValue());
        assertThat(order.getId(), equalTo(orderId));
        assertThat(order.getCreatedAt(), notNullValue());
        assertThat(order.getTotalSum(), equalTo(expectedTotal));
        assertThat("Заказ должен быть привязан к пользователю",
                order.getUsername(), equalTo(USERNAME));

        List<OrderItem> orderItems = orderItemRepository.findAllByOrderId(orderId)
                .collectList().block();
        assertThat(orderItems, notNullValue());
        assertThat("Должно быть 2 позиции заказа", orderItems.size(), is(2));

        List<OrderItem> sorted = orderItems.stream()
                .sorted(Comparator.comparing(OrderItem::getItemId))
                .toList();

        OrderItem oi1 = sorted.stream()
                .filter(oi -> oi.getItemId().equals(i1.getId())).findFirst()
                .orElseThrow(() -> new AssertionError("OrderItem for item " + i1.getId() + " not found"));
        assertThat(oi1.getTitle(), equalTo(i1.getTitle()));
        assertThat(oi1.getPrice(), equalTo(i1.getPrice()));
        assertThat(oi1.getQuantity(), equalTo(q1));
        assertThat(oi1.getId().getOrderId(), equalTo(orderId));
        assertThat(oi1.getId().getItemId(), equalTo(i1.getId()));

        OrderItem oi2 = sorted.stream()
                .filter(oi -> oi.getItemId().equals(i2.getId())).findFirst()
                .orElseThrow(() -> new AssertionError("OrderItem for item " + i2.getId() + " not found"));
        assertThat(oi2.getTitle(), equalTo(i2.getTitle()));
        assertThat(oi2.getPrice(), equalTo(i2.getPrice()));
        assertThat(oi2.getQuantity(), equalTo(q2));
        assertThat(oi2.getId().getOrderId(), equalTo(orderId));
        assertThat(oi2.getId().getItemId(), equalTo(i2.getId()));
    }

    @Test
    void buy_createsNewOrderEachTime() {
        Item item = itemRepository.findAll().next().block();
        assertThat(item, notNullValue());

        cartItemRepository.save(CartItem.newRow(item.getId(), 1, USERNAME)).block();
        Long id1 = buyBlocking();
        assertThat(id1, notNullValue());

        cartItemRepository.save(CartItem.newRow(item.getId(), 2, USERNAME)).block();
        Long id2 = buyBlocking();
        assertThat(id2, notNullValue());

        assertThat("Должны быть разные заказы", id2, not(equalTo(id1)));
        assertThat("Должно быть 2 заказа", orderRepository.count().block(), is(2L));
        assertThat("Корзина должна быть пуста", cartItemRepository.count().block(), is(0L));

        Long orderItemsCount = db.sql("select count(*) as c from order_items")
                .map((row, meta) -> row.get("c", Long.class)).one().block();
        assertThat("Должно быть 2 строки в order_items", orderItemsCount, is(2L));
    }

    @Test
    void buy_whenPaymentFails_shouldNotCreateOrderAndNotClearCart() {
        when(paymentsGateway.charge(anyLong())).thenReturn(Mono.just(false));

        List<Item> items = itemRepository.findAll().take(2).collectList().block();
        assertThat(items, notNullValue());
        assertThat(items.size(), is(2));

        Item i1 = items.get(0);
        Item i2 = items.get(1);

        cartItemRepository.saveAll(List.of(
                CartItem.newRow(i1.getId(), 1, USERNAME),
                CartItem.newRow(i2.getId(), 2, USERNAME)
        )).then().block();

        assertThat(cartItemRepository.count().block(), is(2L));

        buyVerifier()
                .expectErrorMatches(ex ->
                        ex instanceof IllegalStateException
                        && "Payment failed".equals(ex.getMessage()))
                .verify();

        assertThat("Заказ не должен создаваться при неуспешной оплате",
                orderRepository.count().block(), is(0L));
        assertThat("Корзина не должна очищаться при неуспешной оплате",
                cartItemRepository.count().block(), is(2L));

        Long orderItemsCount = db.sql("select count(*) as c from order_items")
                .map((row, meta) -> row.get("c", Long.class)).one().block();
        assertThat("Позиции заказа не должны создаваться", orderItemsCount, is(0L));
    }
}