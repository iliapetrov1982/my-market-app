package de.petrov.ya.java.mymarketapp.service;

import de.petrov.ya.java.mymarketapp.dto.page.ItemDto;
import de.petrov.ya.java.mymarketapp.dto.page.ItemsSort;
import de.petrov.ya.java.mymarketapp.dto.page.Paging;
import de.petrov.ya.java.mymarketapp.repository.ItemRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class ItemsService {
    private final ItemRepository itemRepository;

    public ItemsService(ItemRepository itemRepository) {
        this.itemRepository = itemRepository;
    }

    @Transactional(readOnly = true)
    public ItemsPage getItemsPage(String search, ItemsSort sort, int pageNumber, int pageSize) {
        String safeSearch = (search == null) ? "" : search.trim();

        int safePageSize = normalizePageSize(pageSize);
        int safePageNumber = Math.max(1, pageNumber);

        Sort jpaSort = switch (sort) {
            case ALPHA -> Sort.by(Sort.Order.asc("title"), Sort.Order.asc("id"));
            case PRICE -> Sort.by(Sort.Order.asc("price"), Sort.Order.asc("id"));
            case NO -> Sort.by(Sort.Order.asc("id"));
        };

        Pageable pageable = PageRequest.of(safePageNumber - 1, safePageSize, jpaSort);

        Page<ItemDto> page = itemRepository.findShowcase(
                safeSearch.isBlank() ? null : safeSearch,
                pageable
        );

        // Если пользователь запросил страницу сильно дальше конца — отдадим последнюю страницу
        if (page.getTotalPages() > 0 && safePageNumber > page.getTotalPages()) {
            safePageNumber = page.getTotalPages();
            pageable = PageRequest.of(safePageNumber - 1, safePageSize, jpaSort);
            page = itemRepository.findShowcase(safeSearch.isBlank() ? null : safeSearch, pageable);
        }

        Paging paging = new Paging(
                safePageSize,
                safePageNumber,
                page.hasPrevious(),
                page.hasNext()
        );

        return new ItemsPage(
                toRowsOfThree(page.getContent()),
                paging,
                safeSearch,
                sort.name()
        );
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
