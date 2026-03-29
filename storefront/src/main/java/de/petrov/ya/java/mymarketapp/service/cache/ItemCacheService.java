package de.petrov.ya.java.mymarketapp.service.cache;

import de.petrov.ya.java.mymarketapp.dto.page.ItemDto;
import reactor.core.publisher.Mono;

public interface ItemCacheService {

    Mono<ItemDto> get(long itemId);

    Mono<Boolean> put(long itemId, ItemDto item);

    Mono<Boolean> evict(long itemId);
}