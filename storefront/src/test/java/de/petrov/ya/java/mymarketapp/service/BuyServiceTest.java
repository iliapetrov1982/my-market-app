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
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BuyServiceTest {

    private CartItemRepository cartItemRepository;
    private ItemRepository itemRepository;
    private OrderRepository orderRepository;
    private OrderItemRepository orderItemRepository;
    private PaymentsGateway paymentsGateway;
    private TransactionalOperator tx;

    private BuyService buyService;

    @BeforeEach
    void setUp() {
        cartItemRepository = mock(CartItemRepository.class);
        itemRepository = mock(ItemRepository.class);
        orderRepository = mock(OrderRepository.class);
        orderItemRepository = mock(OrderItemRepository.class);
        paymentsGateway = mock(PaymentsGateway.class);
        tx = mock(TransactionalOperator.class);

        when(tx.transactional(any(Mono.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        buyService = new BuyService(
                cartItemRepository,
                itemRepository,
                orderRepository,
                orderItemRepository,
                paymentsGateway,
                tx
        );
    }

    @Test
    void buy_whenCartIsEmpty_shouldFail_andNotTouchDb() {
        when(cartItemRepository.findAll()).thenReturn(Flux.empty());

        StepVerifier.create(buyService.buy())
                .expectErrorMatches(ex ->
                        ex instanceof IllegalStateException
                        && "Cart is empty".equals(ex.getMessage())
                )
                .verify();

        verify(cartItemRepository, times(1)).findAll();
        verify(itemRepository, never()).findAllById(anyIterable());
        verify(orderRepository, never()).save(any(Order.class));
        verify(orderItemRepository, never()).saveAll(any());
        verify(cartItemRepository, never()).deleteAll();
        verifyNoInteractions(paymentsGateway);
        verify(tx, times(1)).transactional(any(Mono.class));
    }

    @Test
    void buy_happyPath_shouldCreateOrderItems_updateTotal_clearCart_andReturnOrderId() {
        CartItem ci1 = new CartItem(1L, 2);
        CartItem ci2 = new CartItem(2L, 1);
        when(cartItemRepository.findAll()).thenReturn(Flux.just(ci1, ci2));

        Item item1 = new Item("Apple", "d1", "img1", 100L);
        item1.setId(1L);

        Item item2 = new Item("Banana", "d2", "img2", 50L);
        item2.setId(2L);

        when(itemRepository.findAllById(anyIterable()))
                .thenReturn(Flux.just(item1, item2));

        long expectedTotal = 100L * 2 + 50L;
        when(paymentsGateway.charge(expectedTotal)).thenReturn(Mono.just(true));

        java.util.List<OrderItem> capturedOrderItems =
                new java.util.concurrent.CopyOnWriteArrayList<>();

        doAnswer(inv -> {
            @SuppressWarnings("unchecked")
            Publisher<OrderItem> pub = (Publisher<OrderItem>) inv.getArgument(0);

            return Flux.from(pub)
                    .doOnNext(capturedOrderItems::add)
                    .thenMany(Flux.empty());
        }).when(orderItemRepository).saveAll(any());

        java.util.concurrent.atomic.AtomicReference<Order> savedOrderRef =
                new java.util.concurrent.atomic.AtomicReference<>();

        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order arg = inv.getArgument(0);

            Order snap = new Order(arg.getId());
            snap.setCreatedAt(arg.getCreatedAt());
            snap.setTotalSum(arg.getTotalSum());
            savedOrderRef.set(snap);

            arg.setId(42L);
            return Mono.just(arg);
        });

        when(cartItemRepository.deleteAll()).thenReturn(Mono.empty());

        StepVerifier.create(buyService.buy())
                .expectNext(42L)
                .verifyComplete();

        assertThat(capturedOrderItems).hasSize(2);

        OrderItem oi1 = capturedOrderItems.stream()
                .filter(oi -> Long.valueOf(1L).equals(oi.getItemId()))
                .findFirst()
                .orElseThrow();

        assertThat(oi1.getOrderId()).isEqualTo(42L);
        assertThat(oi1.getTitle()).isEqualTo("Apple");
        assertThat(oi1.getPrice()).isEqualTo(100L);
        assertThat(oi1.getQuantity()).isEqualTo(2);

        OrderItem oi2 = capturedOrderItems.stream()
                .filter(oi -> Long.valueOf(2L).equals(oi.getItemId()))
                .findFirst()
                .orElseThrow();

        assertThat(oi2.getOrderId()).isEqualTo(42L);
        assertThat(oi2.getTitle()).isEqualTo("Banana");
        assertThat(oi2.getPrice()).isEqualTo(50L);
        assertThat(oi2.getQuantity()).isEqualTo(1);

        Order savedOrder = savedOrderRef.get();
        assertThat(savedOrder).isNotNull();
        assertThat(savedOrder.getTotalSum()).isEqualTo(expectedTotal);
        assertThat(savedOrder.getCreatedAt()).isNotNull();
        assertThat(savedOrder.getCreatedAt()).isBeforeOrEqualTo(OffsetDateTime.now());

        verify(cartItemRepository, times(1)).findAll();
        verify(itemRepository, times(1)).findAllById(anyIterable());
        verify(paymentsGateway, times(1)).charge(expectedTotal);
        verify(orderRepository, times(1)).save(any(Order.class));
        verify(orderItemRepository, times(1)).saveAll(any());
        verify(cartItemRepository, times(1)).deleteAll();
        verify(tx, times(1)).transactional(any(Mono.class));
    }

    @Test
    void buy_whenPaymentFails_shouldNotCreateOrderAndNotClearCart() {
        CartItem ci1 = new CartItem(1L, 2);
        CartItem ci2 = new CartItem(2L, 1);
        when(cartItemRepository.findAll()).thenReturn(Flux.just(ci1, ci2));

        Item item1 = new Item("Apple", "d1", "img1", 100L);
        item1.setId(1L);

        Item item2 = new Item("Banana", "d2", "img2", 50L);
        item2.setId(2L);

        when(itemRepository.findAllById(anyIterable()))
                .thenReturn(Flux.just(item1, item2));

        long expectedTotal = 100L * 2 + 50L;
        when(paymentsGateway.charge(expectedTotal)).thenReturn(Mono.just(false));

        StepVerifier.create(buyService.buy())
                .expectErrorMatches(ex ->
                        ex instanceof IllegalStateException
                        && "Payment failed".equals(ex.getMessage())
                )
                .verify();

        verify(cartItemRepository, times(1)).findAll();
        verify(itemRepository, times(1)).findAllById(anyIterable());
        verify(paymentsGateway, times(1)).charge(expectedTotal);

        verify(orderRepository, never()).save(any(Order.class));
        verify(orderItemRepository, never()).saveAll(any());
        verify(cartItemRepository, never()).deleteAll();

        verify(tx, times(1)).transactional(any(Mono.class));
    }
}