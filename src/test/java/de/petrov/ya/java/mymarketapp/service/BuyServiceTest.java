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

    // --- зависимости BuyService ---
    private CartItemRepository cartItemRepository;
    private ItemRepository itemRepository;
    private OrderRepository orderRepository;
    private OrderItemRepository orderItemRepository;
    private TransactionalOperator tx;

    private BuyService buyService;

    @BeforeEach
    void setUp() {
        cartItemRepository = mock(CartItemRepository.class);
        itemRepository = mock(ItemRepository.class);
        orderRepository = mock(OrderRepository.class);
        orderItemRepository = mock(OrderItemRepository.class);
        tx = mock(TransactionalOperator.class);

        /*
         * BuyService.buy() оборачивает бизнес-логику в:
         *     tx.transactional(mono)
         *
         * Нам НЕ нужно реально тестировать транзакции.
         * Поэтому мы просто возвращаем переданный Mono как есть.
         *
         * То есть transactional(...) в тесте — это "no-op".
         */
        when(tx.transactional(any(Mono.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        buyService = new BuyService(
                cartItemRepository,
                itemRepository,
                orderRepository,
                orderItemRepository,
                tx
        );
    }

    // ================================================================
    // ПУСТАЯ КОРЗИНА
    // ================================================================
    @Test
    void buy_whenCartIsEmpty_shouldFail_andNotTouchDb() {

        // Корзина пустая
        when(cartItemRepository.findAll()).thenReturn(Flux.empty());

        // Проверяем что buy() завершается ошибкой
        StepVerifier.create(buyService.buy())
                .expectErrorMatches(ex ->
                        ex instanceof IllegalStateException
                        && "Cart is empty".equals(ex.getMessage())
                )
                .verify();

        // Проверяем что дальше БД вообще не трогалась
        verify(cartItemRepository, times(1)).findAll();
        verify(itemRepository, never()).findAllById(anyIterable());
        verify(orderRepository, never()).save(any(Order.class));
        verify(orderItemRepository, never()).saveAll(any());
        verify(cartItemRepository, never()).deleteAll();
        verify(tx, times(1)).transactional(any(Mono.class));
    }

    // ================================================================
    // HAPPY PATH
    // ================================================================
    @Test
    void buy_happyPath_shouldCreateOrderItems_updateTotal_clearCart_andReturnOrderId() {

        // ------------------------------------------------------------
        // 1. Готовим корзину
        // ------------------------------------------------------------
        CartItem ci1 = new CartItem(1L, 2); // 2 * 100
        CartItem ci2 = new CartItem(2L, 1); // 1 * 50

        when(cartItemRepository.findAll())
                .thenReturn(Flux.just(ci1, ci2));

        // ------------------------------------------------------------
        // 2. Готовим реальные товары (как будто из БД)
        // ------------------------------------------------------------
        Item item1 = new Item("Apple", "d1", "img1", 100L);
        item1.setId(1L);

        Item item2 = new Item("Banana", "d2", "img2", 50L);
        item2.setId(2L);

        when(itemRepository.findAllById(anyIterable()))
                .thenReturn(Flux.just(item1, item2));

        // ------------------------------------------------------------
        // 3. Захват orderItems, которые будут сохранены
        // ------------------------------------------------------------
        /*
         * saveAll() принимает Publisher<OrderItem>.
         *
         * Нам нужно:
         *  - подписаться на него
         *  - собрать элементы
         *  - вернуть пустой Flux (как будто сохранение прошло)
         *
         * CopyOnWriteArrayList используется потому что:
         *  - Flux может выполняться в другом потоке
         *  - это thread-safe коллекция
         */
        java.util.List<OrderItem> capturedOrderItems =
                new java.util.concurrent.CopyOnWriteArrayList<>();

        doAnswer(inv -> {
            @SuppressWarnings("unchecked")
            Publisher<OrderItem> pub =
                    (Publisher<OrderItem>) inv.getArgument(0);

            return Flux.from(pub)
                    .doOnNext(capturedOrderItems::add)
                    .thenMany(Flux.empty()); // имитируем завершённый saveAll
        }).when(orderItemRepository).saveAll(any());

        // ------------------------------------------------------------
        // 4. Snapshot Order при каждом save()
        // ------------------------------------------------------------
        /*
         * В buy() orderRepository.save() вызывается ДВАЖДЫ:
         *
         *   1) создаётся новый Order(total=0)
         *   2) обновляется totalSum и createdAt
         *
         * Проблема:
         * Order — мутируемый объект.
         * Если мы просто захватим его — оба save() будут ссылаться
         * на один и тот же объект.
         *
         * Поэтому делаем SNAPSHOT — копируем значения в record.
         */

        record OrderSnapshot(Long id, OffsetDateTime createdAt, Long totalSum) {}

        java.util.List<OrderSnapshot> snapshots =
                new java.util.concurrent.CopyOnWriteArrayList<>();

        when(orderRepository.save(any(Order.class)))
                .thenAnswer(inv -> {
                    Order arg = inv.getArgument(0);

                    // фиксируем состояние ДО изменений
                    snapshots.add(
                            new OrderSnapshot(
                                    arg.getId(),
                                    arg.getCreatedAt(),
                                    arg.getTotalSum()
                            )
                    );

                    // имитируем поведение БД:
                    // при первом save() генерируется ID
                    if (arg.getId() == null) {
                        arg.setId(42L);
                        arg.setCreatedAt(null); // заставим updateTotal() установить дату
                    }

                    return Mono.just(arg);
                });

        when(cartItemRepository.deleteAll())
                .thenReturn(Mono.empty());

        // ------------------------------------------------------------
        // 5. Запускаем buy()
        // ------------------------------------------------------------
        StepVerifier.create(buyService.buy())
                .expectNext(42L)
                .verifyComplete();

        // ------------------------------------------------------------
        // 6. Проверяем OrderItems
        // ------------------------------------------------------------
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

        // ------------------------------------------------------------
        // 7. Проверяем save() Order
        // ------------------------------------------------------------
        long expectedTotal = 100L * 2 + 50L * 1; // 250

        assertThat(snapshots).hasSize(2);

        OrderSnapshot first = snapshots.get(0);
        OrderSnapshot second = snapshots.get(1);

        // Первый save — Order(total=0)
        assertThat(first.totalSum()).isEqualTo(0L);

        // Второй save — уже с рассчитанным total
        assertThat(second.id()).isEqualTo(42L);
        assertThat(second.totalSum()).isEqualTo(expectedTotal);
        assertThat(second.createdAt()).isNotNull();
        assertThat(second.createdAt())
                .isBeforeOrEqualTo(OffsetDateTime.now());

        // ------------------------------------------------------------
        // 8. Проверяем взаимодействия
        // ------------------------------------------------------------
        verify(cartItemRepository, times(1)).findAll();
        verify(itemRepository, times(1)).findAllById(anyIterable());
        verify(orderItemRepository, times(1)).saveAll(any());
        verify(cartItemRepository, times(1)).deleteAll();
        verify(tx, times(1)).transactional(any(Mono.class));
    }
}
