package de.petrov.ya.java.mymarketapp.service;

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
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.reactivestreams.Publisher;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BuyServiceTest {

    private static final String USERNAME = "user1";

    private CartItemRepository cartItemRepository;
    private ItemRepository itemRepository;
    private OrderRepository orderRepository;
    private OrderItemRepository orderItemRepository;
    private PaymentsGateway paymentsGateway;
    private TransactionalOperator tx;
    private BuyService buyService;

    @BeforeEach
    void setUp() {
        cartItemRepository  = mock(CartItemRepository.class);
        itemRepository      = mock(ItemRepository.class);
        orderRepository     = mock(OrderRepository.class);
        orderItemRepository = mock(OrderItemRepository.class);
        paymentsGateway     = mock(PaymentsGateway.class);
        tx                  = mock(TransactionalOperator.class);

        when(tx.transactional(any(Mono.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        buyService = new BuyService(
                cartItemRepository, itemRepository,
                orderRepository, orderItemRepository,
                paymentsGateway, tx);
    }

    // -------------------------------------------------------------------------
    // helper: подставляет SecurityContext с USERNAME
    // -------------------------------------------------------------------------
    private <T> Mono<T> withUser(Mono<T> mono) {
        var auth = UsernamePasswordAuthenticationToken
                .authenticated(USERNAME, null, List.of());
        return mono.contextWrite(
                ReactiveSecurityContextHolder.withAuthentication(auth));
    }

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------

    @Test
    void buy_whenCartIsEmpty_shouldFail_andNotTouchDb() {
        when(cartItemRepository.findAllByUsername(USERNAME)).thenReturn(Flux.empty());

        StepVerifier.create(withUser(buyService.buy()))
                .expectErrorMatches(ex ->
                        ex instanceof IllegalStateException
                        && "Cart is empty".equals(ex.getMessage()))
                .verify();

        verify(cartItemRepository).findAllByUsername(USERNAME);
        verify(itemRepository, never()).findAllById(anyIterable());
        verify(orderRepository, never()).save(any());
        verifyNoInteractions(paymentsGateway);
    }

    @Test
    void buy_happyPath_shouldCreateOrder_clearCart_andReturnOrderId() {
        CartItem ci1 = new CartItem(1L, 2, USERNAME);
        CartItem ci2 = new CartItem(2L, 1, USERNAME);
        when(cartItemRepository.findAllByUsername(USERNAME)).thenReturn(Flux.just(ci1, ci2));

        Item item1 = new Item("Apple", "d1", "img1", 100L); item1.setId(1L);
        Item item2 = new Item("Banana", "d2", "img2", 50L);  item2.setId(2L);
        when(itemRepository.findAllById(anyIterable())).thenReturn(Flux.just(item1, item2));

        long expectedTotal = 100L * 2 + 50L;
        when(paymentsGateway.charge(expectedTotal)).thenReturn(Mono.just(true));

        List<OrderItem> capturedItems = new CopyOnWriteArrayList<>();
        doAnswer(inv -> {
            @SuppressWarnings("unchecked")
            Publisher<OrderItem> pub = (Publisher<OrderItem>) inv.getArgument(0);
            return Flux.from(pub).doOnNext(capturedItems::add).thenMany(Flux.empty());
        }).when(orderItemRepository).saveAll(any());

        AtomicReference<Order> savedOrder = new AtomicReference<>();
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order arg = inv.getArgument(0);
            savedOrder.set(arg);
            arg.setId(42L);
            return Mono.just(arg);
        });

        when(cartItemRepository.deleteAllByUsername(USERNAME)).thenReturn(Mono.empty());

        StepVerifier.create(withUser(buyService.buy()))
                .expectNext(42L)
                .verifyComplete();

        // проверяем привязку заказа к пользователю
        assertThat(savedOrder.get().getUsername()).isEqualTo(USERNAME);
        assertThat(savedOrder.get().getTotalSum()).isEqualTo(expectedTotal);
        assertThat(savedOrder.get().getCreatedAt()).isNotNull()
                .isBeforeOrEqualTo(OffsetDateTime.now());

        // позиции заказа
        assertThat(capturedItems).hasSize(2);
        assertThat(capturedItems).anySatisfy(oi -> {
            assertThat(oi.getItemId()).isEqualTo(1L);
            assertThat(oi.getTitle()).isEqualTo("Apple");
            assertThat(oi.getQuantity()).isEqualTo(2);
        });

        verify(cartItemRepository).deleteAllByUsername(USERNAME);
        verify(tx).transactional(any(Mono.class));
    }

    @Test
    void buy_whenPaymentFails_shouldNotCreateOrder_andNotClearCart() {
        CartItem ci1 = new CartItem(1L, 2, USERNAME);
        CartItem ci2 = new CartItem(2L, 1, USERNAME);
        when(cartItemRepository.findAllByUsername(USERNAME)).thenReturn(Flux.just(ci1, ci2));

        Item item1 = new Item("Apple", "d1", "img1", 100L); item1.setId(1L);
        Item item2 = new Item("Banana", "d2", "img2", 50L);  item2.setId(2L);
        when(itemRepository.findAllById(anyIterable())).thenReturn(Flux.just(item1, item2));

        long expectedTotal = 100L * 2 + 50L;
        when(paymentsGateway.charge(expectedTotal)).thenReturn(Mono.just(false));

        StepVerifier.create(withUser(buyService.buy()))
                .expectErrorMatches(ex ->
                        ex instanceof IllegalStateException
                        && "Payment failed".equals(ex.getMessage()))
                .verify();

        verify(orderRepository, never()).save(any());
        verify(cartItemRepository, never()).deleteAllByUsername(any());
    }
}