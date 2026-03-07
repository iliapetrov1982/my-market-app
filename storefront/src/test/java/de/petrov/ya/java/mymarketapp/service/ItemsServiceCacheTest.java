package de.petrov.ya.java.mymarketapp.service;

import de.petrov.ya.java.mymarketapp.dto.page.ItemDto;
import de.petrov.ya.java.mymarketapp.exception.EntityNotFoundException;
import de.petrov.ya.java.mymarketapp.repository.ItemQueryRepository;
import de.petrov.ya.java.mymarketapp.service.cache.ItemCacheService;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.*;

class ItemsServiceCacheTest {

    private final ItemQueryRepository itemQueryRepository = mock(ItemQueryRepository.class);
    private final ItemCacheService itemCacheService = mock(ItemCacheService.class);

    private final ItemsService itemsService = new ItemsService(itemQueryRepository, itemCacheService);

    @Test
    void getItem_cacheHit_shouldReturnFromCache() {
        long id = 1L;
        ItemDto item = new ItemDto(id, "Coffee", "desc", "/img.png", 100L, 0);

        when(itemCacheService.get(id)).thenReturn(Mono.just(item));

        StepVerifier.create(itemsService.getItem(id))
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
        when(itemQueryRepository.findItemPage(id)).thenReturn(Mono.just(item));
        when(itemCacheService.put(id, item)).thenReturn(Mono.just(true));

        StepVerifier.create(itemsService.getItem(id))
                .expectNext(item)
                .verifyComplete();

        verify(itemCacheService).get(id);
        verify(itemQueryRepository).findItemPage(id);
        verify(itemCacheService).put(id, item);
    }

    @Test
    void getItemPage_cacheMissAndNotFound_shouldReturnEntityNotFoundException() {
        long id = 42L;

        when(itemCacheService.get(id)).thenReturn(Mono.empty());
        when(itemQueryRepository.findItemPage(id)).thenReturn(Mono.empty());

        StepVerifier.create(itemsService.getItemPage(id))
                .expectErrorSatisfies(error -> {
                    assert error instanceof EntityNotFoundException;
                    assert error.getMessage().equals("Item not found: 42");
                })
                .verify();

        verify(itemCacheService).get(id);
        verify(itemQueryRepository).findItemPage(id);
        verify(itemCacheService, never()).put(anyLong(), any());
    }
}