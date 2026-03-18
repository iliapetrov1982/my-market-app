package de.petrov.ya.java.mymarketapp.service;

import de.petrov.ya.java.mymarketapp.dto.page.ItemDto;
import de.petrov.ya.java.mymarketapp.dto.page.ItemsSort;
import de.petrov.ya.java.mymarketapp.dto.page.Paging;
import de.petrov.ya.java.mymarketapp.exception.EntityNotFoundException;
import de.petrov.ya.java.mymarketapp.repository.ItemQueryRepository;
import de.petrov.ya.java.mymarketapp.service.cache.ItemCacheService;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

@Service
public class ItemsService {

    private final ItemQueryRepository itemQueryRepository;
    private final ItemCacheService itemCacheService;

    public ItemsService(
            ItemQueryRepository itemQueryRepository,
            ItemCacheService itemCacheService
    ) {
        this.itemQueryRepository = itemQueryRepository;
        this.itemCacheService = itemCacheService;
    }

    public Mono<ItemsPage> getItemsPage(String search, ItemsSort sort, int pageNumber, int pageSize) {
        String safeSearch = (search == null) ? "" : search.trim();
        int safePageSize = normalizePageSize(pageSize);
        int safePageNumber = Math.max(1, pageNumber);

        Sort uiSort = switch (sort) {
            case ALPHA -> Sort.by(Sort.Order.asc("title"), Sort.Order.asc("id"));
            case PRICE -> Sort.by(Sort.Order.asc("price"), Sort.Order.asc("id"));
            case NO -> Sort.by(Sort.Order.asc("id"));
        };

        var pageable = PageRequest.of(safePageNumber - 1, safePageSize, uiSort);

        return currentUsername()
                .flatMap(username -> loadPage(safeSearch, sort, pageable, username)
                        .flatMap(page -> {
                            int totalPages = page.getTotalPages();
                            if (totalPages > 0 && safePageNumber > totalPages) {
                                var lastPageable = PageRequest.of(totalPages - 1, safePageSize, uiSort);
                                return loadPage(safeSearch, sort, lastPageable, username);
                            }
                            return Mono.just(page);
                        })
                        .map(page -> {
                            Paging paging = new Paging(
                                    safePageSize,
                                    page.getNumber() + 1,
                                    page.hasPrevious(),
                                    page.hasNext()
                            );
                            return new ItemsPage(
                                    toRowsOfThree(page.getContent()),
                                    paging,
                                    safeSearch,
                                    sort.name()
                            );
                        })
                );
    }

    private Mono<PageImpl<ItemDto>> loadPage(String search, ItemsSort sort,
                                             PageRequest pageable, String username) {
        String q = search.isBlank() ? "" : search;
        int limit = pageable.getPageSize();
        int offset = (int) pageable.getOffset();

        Mono<List<ItemDto>> dataMono = itemQueryRepository
                .findShowcase(q, sort, limit, offset, username)
                .collectList();
        Mono<Long> totalMono = itemQueryRepository.countShowcase(q);

        return Mono.zip(dataMono, totalMono)
                .map(t -> new PageImpl<>(t.getT1(), pageable, t.getT2()));
    }

    public Mono<ItemDto> getItem(long id) {
        return currentUsername()
                .flatMap(username -> getCachedItem(id, username,
                        () -> new IllegalArgumentException("Item not found: " + id)));
    }

    public Mono<ItemDto> getItemPage(long id) {
        return currentUsername()
                .flatMap(username -> getCachedItem(id, username,
                        () -> new EntityNotFoundException("Item not found: " + id)));
    }

    private Mono<ItemDto> getCachedItem(long id, String username,
                                        java.util.function.Supplier<? extends RuntimeException> ex) {
        return itemCacheService.get(id)
                .switchIfEmpty(Mono.defer(() ->
                        itemQueryRepository.findItemPage(id, username)
                                .switchIfEmpty(Mono.error(ex.get()))
                                .flatMap(item ->
                                        itemCacheService.put(id, item).thenReturn(item)
                                )
                ));
    }

    // -------------------------------------------------------------------------
    // helpers
    // -------------------------------------------------------------------------

    /**
     * Возвращает username текущего пользователя.
     * Для анонимных запросов (публичные страницы) возвращает пустую строку —
     * в этом случае JOIN с cart_items не даст ни одной строки, что корректно.
     */
    static Mono<String> currentUsername() {
        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> ctx.getAuthentication().getName())
                .defaultIfEmpty("");
    }

    private int normalizePageSize(int pageSize) {
        return switch (pageSize) {
            case 2, 5, 10, 20, 50, 100 -> pageSize;
            default -> 5;
        };
    }

    private List<List<ItemDto>> toRowsOfThree(List<ItemDto> flat) {
        List<List<ItemDto>> rows = new ArrayList<>();
        for (int i = 0; i < flat.size(); i += 3) {
            List<ItemDto> row = new ArrayList<>(3);
            for (int j = 0; j < 3; j++) {
                int idx = i + j;
                row.add(idx < flat.size() ? flat.get(idx) : ItemDto.placeholder());
            }
            rows.add(row);
        }
        return rows;
    }

    public record ItemsPage(
            List<List<ItemDto>> items,
            Paging paging,
            String search,
            String sort
    ) {}
}