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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartCommandServiceTest {

    private static final String USERNAME = "user1";

    @Mock CartItemRepository cartItemRepository;
    @Mock ItemCacheService itemCacheService;
    @InjectMocks CartCommandService cartCommandService;

    // -------------------------------------------------------------------------
    // helpers
    // -------------------------------------------------------------------------

    /** Оборачивает вызов сервиса в реактивный SecurityContext с заданным пользователем. */
    private <T> reactor.core.publisher.Mono<T> withUser(Mono<T> mono) {
        var auth = UsernamePasswordAuthenticationToken
                .authenticated(USERNAME, null, List.of());
        return mono.contextWrite(
                ReactiveSecurityContextHolder.withAuthentication(auth));
    }

    private void stubEvict(long itemId) {
        when(itemCacheService.evict(itemId)).thenReturn(Mono.just(true));
    }

    // -------------------------------------------------------------------------
    // PLUS
    // -------------------------------------------------------------------------

    @Test
    void apply_plus_whenItemExists_shouldIncrementAndEvictCache() {
        long itemId = 1L;
        CartItem existing = new CartItem(itemId, 2, USERNAME);

        when(cartItemRepository.findByItemIdAndUsername(itemId, USERNAME))
                .thenReturn(Mono.just(existing));
        when(cartItemRepository.save(any())).thenReturn(Mono.just(existing));
        stubEvict(itemId);

        StepVerifier.create(withUser(cartCommandService.apply(itemId, CartAction.PLUS)))
                .verifyComplete();

        verify(cartItemRepository).findByItemIdAndUsername(itemId, USERNAME);
        verify(cartItemRepository).save(existing);
        verify(itemCacheService).evict(itemId);
        verifyNoMoreInteractions(cartItemRepository, itemCacheService);
    }

    @Test
    void apply_plus_whenItemMissing_shouldCreateAndEvictCache() {
        long itemId = 1L;

        when(cartItemRepository.findByItemIdAndUsername(itemId, USERNAME))
                .thenReturn(Mono.empty());
        when(cartItemRepository.save(any()))
                .thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        stubEvict(itemId);

        StepVerifier.create(withUser(cartCommandService.apply(itemId, CartAction.PLUS)))
                .verifyComplete();

        verify(cartItemRepository).findByItemIdAndUsername(itemId, USERNAME);
        verify(cartItemRepository).save(any(CartItem.class));
        verify(itemCacheService).evict(itemId);
        verifyNoMoreInteractions(cartItemRepository, itemCacheService);
    }

    // -------------------------------------------------------------------------
    // MINUS
    // -------------------------------------------------------------------------

    @Test
    void apply_minus_whenQuantityBecomesPositive_shouldSaveAndEvictCache() {
        long itemId = 1L;
        CartItem existing = new CartItem(itemId, 2, USERNAME);

        when(cartItemRepository.findByItemIdAndUsername(itemId, USERNAME))
                .thenReturn(Mono.just(existing));
        when(cartItemRepository.save(any()))
                .thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        stubEvict(itemId);

        StepVerifier.create(withUser(cartCommandService.apply(itemId, CartAction.MINUS)))
                .verifyComplete();

        verify(cartItemRepository).save(existing);
        verify(itemCacheService).evict(itemId);
    }

    @Test
    void apply_minus_whenQuantityBecomesZero_shouldDeleteAndEvictCache() {
        long itemId = 1L;
        CartItem existing = new CartItem(itemId, 1, USERNAME);

        when(cartItemRepository.findByItemIdAndUsername(itemId, USERNAME))
                .thenReturn(Mono.just(existing));
        when(cartItemRepository.delete(existing)).thenReturn(Mono.empty());
        stubEvict(itemId);

        StepVerifier.create(withUser(cartCommandService.apply(itemId, CartAction.MINUS)))
                .verifyComplete();

        verify(cartItemRepository).delete(existing);
        verify(itemCacheService).evict(itemId);
    }

    @Test
    void apply_minus_whenItemMissing_shouldDoNothing() {
        long itemId = 1L;

        when(cartItemRepository.findByItemIdAndUsername(itemId, USERNAME))
                .thenReturn(Mono.empty());

        StepVerifier.create(withUser(cartCommandService.apply(itemId, CartAction.MINUS)))
                .verifyComplete();

        verify(cartItemRepository).findByItemIdAndUsername(itemId, USERNAME);
        verifyNoMoreInteractions(cartItemRepository);
        verifyNoInteractions(itemCacheService);
    }

    // -------------------------------------------------------------------------
    // DELETE
    // -------------------------------------------------------------------------

    @Test
    void apply_delete_whenItemExists_shouldDeleteAndEvictCache() {
        long itemId = 1L;
        CartItem existing = new CartItem(itemId, 3, USERNAME);

        when(cartItemRepository.findByItemIdAndUsername(itemId, USERNAME))
                .thenReturn(Mono.just(existing));
        when(cartItemRepository.delete(existing)).thenReturn(Mono.empty());
        stubEvict(itemId);

        StepVerifier.create(withUser(cartCommandService.apply(itemId, CartAction.DELETE)))
                .verifyComplete();

        verify(cartItemRepository).delete(existing);
        verify(itemCacheService).evict(itemId);
    }

    @Test
    void apply_delete_whenItemMissing_shouldDoNothing() {
        long itemId = 1L;

        when(cartItemRepository.findByItemIdAndUsername(itemId, USERNAME))
                .thenReturn(Mono.empty());

        StepVerifier.create(withUser(cartCommandService.apply(itemId, CartAction.DELETE)))
                .verifyComplete();

        verify(cartItemRepository).findByItemIdAndUsername(itemId, USERNAME);
        verifyNoMoreInteractions(cartItemRepository);
        verifyNoInteractions(itemCacheService);
    }
}