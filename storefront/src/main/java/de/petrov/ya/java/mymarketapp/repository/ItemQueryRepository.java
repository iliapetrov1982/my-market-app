package de.petrov.ya.java.mymarketapp.repository;

import de.petrov.ya.java.mymarketapp.dto.page.ItemDto;
import de.petrov.ya.java.mymarketapp.dto.page.ItemsSort;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ItemQueryRepository {

    Flux<ItemDto> findShowcase(String q, ItemsSort sort, int limit, int offset, String username);

    Mono<Long> countShowcase(String q);

    Mono<ItemDto> findItemPage(long id, String username);

    Flux<ItemDto> findCartItems(String username);
}
