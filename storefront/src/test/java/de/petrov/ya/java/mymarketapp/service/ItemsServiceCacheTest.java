package de.petrov.ya.java.mymarketapp.service;

import de.petrov.ya.java.mymarketapp.dto.page.ItemDto;
import de.petrov.ya.java.mymarketapp.exception.EntityNotFoundException;
import de.petrov.ya.java.mymarketapp.repository.ItemQueryRepository;
import de.petrov.ya.java.mymarketapp.service.cache.ItemCacheService;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.mockito.Mockito.*;

class ItemsServiceCacheTest {

    private static final String USERNAME = "user1";

    private final ItemQueryRepository itemQueryRepository = mock(ItemQueryRepository.class);
    private final ItemCacheService itemCacheService = mock(ItemCacheService.class);
    private final ItemsService itemsService = new ItemsService(itemQueryRepository, itemCacheService);

    private <T> Mono<T> withUser(Mono<T> mono) {
        var auth = UsernamePasswordAuthenticationToken
                .authenticated(USERNAME, null, List.of());
        return mono.contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth));
    }

    @Test
    void getItem_cacheHit_shouldReturnFromCache() {
        long id = 1L;
        ItemDto item = new ItemDto(id, "Coffee", "desc", "/img.png", 100L, 0);

        when(itemCacheService.get(id)).thenReturn(Mono.just(item));

        StepVerifier.create(withUser(itemsService.getItem(id)))
                .expectNext(item)
                .verifyComplete();

        verify(itemCacheService).get(id);
        verifyNoInteractions(itemQueryRepository);
        verify(itemCacheService, never()).put(anyLong(), any());
    }

    @Test
    void getItem_cacheMiss_shouldLoadFromRepositoryAndPutIntoCache() {
        long id = 1L;
        ItemDto item = new ItemDto(id, "Coffee", "desc", "/img.png", 100L, 0);

        when(itemCacheService.get(id)).thenReturn(Mono.empty());
        when(itemQueryRepository.findItemPage(id, USERNAME)).thenReturn(Mono.just(item));
        when(itemCacheService.put(id, item)).thenReturn(Mono.just(true));

        StepVerifier.create(withUser(itemsService.getItem(id)))
                .expectNext(item)
                .verifyComplete();

        verify(itemCacheService).get(id);
        verify(itemQueryRepository).findItemPage(id, USERNAME);
        verify(itemCacheService).put(id, item);
    }

    @Test
    void getItemPage_cacheMissAndNotFound_shouldReturnEntityNotFoundException() {
        long id = 42L;

        when(itemCacheService.get(id)).thenReturn(Mono.empty());
        when(itemQueryRepository.findItemPage(id, USERNAME)).thenReturn(Mono.empty());

        StepVerifier.create(withUser(itemsService.getItemPage(id)))
                .expectErrorSatisfies(error -> {
                    assert error instanceof EntityNotFoundException;
                    assert error.getMessage().equals("Item not found: 42");
                })
                .verify();

        verify(itemCacheService).get(id);
        verify(itemQueryRepository).findItemPage(id, USERNAME);
        verify(itemCacheService, never()).put(anyLong(), any());
    }
}