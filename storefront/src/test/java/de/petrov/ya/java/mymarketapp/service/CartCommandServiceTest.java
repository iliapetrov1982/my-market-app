package de.petrov.ya.java.mymarketapp.service;

import de.petrov.ya.java.mymarketapp.dto.cart.CartAction;
import de.petrov.ya.java.mymarketapp.entity.CartItem;
import de.petrov.ya.java.mymarketapp.repository.CartItemRepository;
import de.petrov.ya.java.mymarketapp.service.cache.ItemCacheService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class CartCommandServiceTest {

    @Mock
    CartItemRepository cartItemRepository;

    @Mock
    ItemCacheService itemCacheService;

    @InjectMocks
    CartCommandService cartCommandService;

    @Test
    void apply_plus_whenItemExists_shouldIncrementAndEvictCache() {
        long itemId = 1L;
        CartItem existing = new CartItem(itemId, 2);

        when(cartItemRepository.findById(itemId)).thenReturn(Mono.just(existing));
        when(cartItemRepository.save(any(CartItem.class))).thenReturn(Mono.just(existing));
        when(itemCacheService.evict(itemId)).thenReturn(Mono.just(true));

        StepVerifier.create(cartCommandService.apply(itemId, CartAction.PLUS))
                .verifyComplete();

        verify(cartItemRepository).findById(itemId);
        verify(cartItemRepository).save(existing);
        verify(itemCacheService).evict(itemId);
        verifyNoMoreInteractions(cartItemRepository, itemCacheService);
    }

    @Test
    void apply_plus_whenItemMissing_shouldCreateAndEvictCache() {
        long itemId = 1L;

        when(cartItemRepository.findById(itemId)).thenReturn(Mono.empty());
        when(cartItemRepository.save(any(CartItem.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(itemCacheService.evict(itemId)).thenReturn(Mono.just(true));

        StepVerifier.create(cartCommandService.apply(itemId, CartAction.PLUS))
                .verifyComplete();

        verify(cartItemRepository).findById(itemId);
        verify(cartItemRepository).save(any(CartItem.class));
        verify(itemCacheService).evict(itemId);
        verifyNoMoreInteractions(cartItemRepository, itemCacheService);
    }

    @Test
    void apply_minus_whenItemExistsAndQuantityBecomesPositive_shouldSaveAndEvictCache() {
        long itemId = 1L;
        CartItem existing = new CartItem(itemId, 2);

        when(cartItemRepository.findById(itemId)).thenReturn(Mono.just(existing));
        when(cartItemRepository.save(any(CartItem.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(itemCacheService.evict(itemId)).thenReturn(Mono.just(true));

        StepVerifier.create(cartCommandService.apply(itemId, CartAction.MINUS))
                .verifyComplete();

        verify(cartItemRepository).findById(itemId);
        verify(cartItemRepository).save(existing);
        verify(itemCacheService).evict(itemId);
        verifyNoMoreInteractions(cartItemRepository, itemCacheService);
    }

    @Test
    void apply_minus_whenItemExistsAndQuantityBecomesZero_shouldDeleteAndEvictCache() {
        long itemId = 1L;
        CartItem existing = new CartItem(itemId, 1);

        when(cartItemRepository.findById(itemId)).thenReturn(Mono.just(existing));
        when(cartItemRepository.delete(existing)).thenReturn(Mono.empty());
        when(itemCacheService.evict(itemId)).thenReturn(Mono.just(true));

        StepVerifier.create(cartCommandService.apply(itemId, CartAction.MINUS))
                .verifyComplete();

        verify(cartItemRepository).findById(itemId);
        verify(cartItemRepository).delete(existing);
        verify(itemCacheService).evict(itemId);
        verifyNoMoreInteractions(cartItemRepository, itemCacheService);
    }

    @Test
    void apply_delete_whenItemExists_shouldDeleteAndEvictCache() {
        long itemId = 1L;
        CartItem existing = new CartItem(itemId, 3);

        when(cartItemRepository.findById(itemId)).thenReturn(Mono.just(existing));
        when(cartItemRepository.delete(existing)).thenReturn(Mono.empty());
        when(itemCacheService.evict(itemId)).thenReturn(Mono.just(true));

        StepVerifier.create(cartCommandService.apply(itemId, CartAction.DELETE))
                .verifyComplete();

        verify(cartItemRepository).findById(itemId);
        verify(cartItemRepository).delete(existing);
        verify(itemCacheService).evict(itemId);
        verifyNoMoreInteractions(cartItemRepository, itemCacheService);
    }

    @Test
    void apply_minus_whenItemMissing_shouldDoNothingAndNotEvictCache() {
        long itemId = 1L;

        when(cartItemRepository.findById(itemId)).thenReturn(Mono.empty());

        StepVerifier.create(cartCommandService.apply(itemId, CartAction.MINUS))
                .verifyComplete();

        verify(cartItemRepository).findById(itemId);
        verifyNoMoreInteractions(cartItemRepository);
        verifyNoInteractions(itemCacheService);
    }

    @Test
    void apply_delete_whenItemMissing_shouldDoNothingAndNotEvictCache() {
        long itemId = 1L;

        when(cartItemRepository.findById(itemId)).thenReturn(Mono.empty());

        StepVerifier.create(cartCommandService.apply(itemId, CartAction.DELETE))
                .verifyComplete();

        verify(cartItemRepository).findById(itemId);
        verifyNoMoreInteractions(cartItemRepository);
        verifyNoInteractions(itemCacheService);
    }
}