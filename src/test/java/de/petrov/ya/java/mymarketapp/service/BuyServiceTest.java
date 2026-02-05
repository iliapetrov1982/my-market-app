package de.petrov.ya.java.mymarketapp.service;

import de.petrov.ya.java.mymarketapp.entity.CartItem;
import de.petrov.ya.java.mymarketapp.entity.Item;
import de.petrov.ya.java.mymarketapp.entity.order.Order;
import de.petrov.ya.java.mymarketapp.repository.CartItemRepository;
import de.petrov.ya.java.mymarketapp.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BuyServiceTest {

    @Mock
    CartItemRepository cartItemRepository;

    @Mock
    OrderRepository orderRepository;

    @InjectMocks
    BuyService buyService;

    @Captor
    ArgumentCaptor<Order> orderCaptor;

    private static Item item(long id, String title, long price) {
        var item = new Item(
                title,
                "desc-" + id,
                "/images/" + id + ".png",
                price
        );
        item.setId(id);
        return item;
    }

    private static CartItem cartItem(Item item, int qty) {
        // НЕ МОКАЕМ entity. Создаём реальную.
        CartItem ci = new CartItem(item, qty);

        // важно при @MapsId/@Id=item_id
        ci.setItemId(item.getId());

        return ci;
    }

    @Test
    void buy_whenCartIsEmpty_throwsIllegalStateException() {
        when(cartItemRepository.findAll()).thenReturn(List.of());

        var ex = assertThrows(IllegalStateException.class, () -> buyService.buy());
        assertThat(ex.getMessage(), equalTo("Cart is empty"));

        verify(cartItemRepository, times(1)).findAll();
        verifyNoInteractions(orderRepository);
        verify(cartItemRepository, never()).deleteAll();
    }

    @Test
    void buy_whenCartHasItems_createsOrder_calculatesTotalSum_clearsCart_returnsOrderId() {
        Item cap = mockItem(1L, "Cap", 1499L);
        Item mug = mockItem(2L, "Mug", 899L);

        CartItem capItem = new CartItem(cap, 2); // ← ВНЕ when
        CartItem mugItem = new CartItem(mug, 3);

        when(cartItemRepository.findAll())
                .thenReturn(List.of(capItem, mugItem));

        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setId(10L);
            return o;
        });

        long orderId = buyService.buy();

        assertThat(orderId, equalTo(10L));

        verify(orderRepository).save(orderCaptor.capture());
        Order saved = orderCaptor.getValue();

        assertThat(saved.getId(), equalTo(10L));
        assertThat(saved.getCreatedAt(), notNullValue());
        assertThat(saved.getTotalSum(), equalTo(5695L)); // 2998 + 2697

        verify(cartItemRepository).deleteAll();
    }

    private static Item mockItem(long id, String title, long price) {
        Item item = mock(Item.class);
        when(item.getId()).thenReturn(id);
        when(item.getTitle()).thenReturn(title);
        when(item.getPrice()).thenReturn(price);
        return item;
    }

    @Test
    void buy_setsInitialTotalSumZero_beforeRecalculation() {
        Item it = item(1L, "One", 100L);

        when(cartItemRepository.findAll()).thenReturn(List.of(cartItem(it, 1)));

        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            // на момент save (до цикла) totalSum должен быть 0
            assertThat(o.getTotalSum(), equalTo(0L));
            o.setId(1L);
            return o;
        });

        buyService.buy();

        verify(orderRepository, times(1)).save(any(Order.class));
        verify(cartItemRepository, times(1)).deleteAll();
    }
}
