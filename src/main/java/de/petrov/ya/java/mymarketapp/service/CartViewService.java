package de.petrov.ya.java.mymarketapp.service;

import de.petrov.ya.java.mymarketapp.dto.page.ItemDto;
import de.petrov.ya.java.mymarketapp.repository.ItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CartViewService {

    public record CartPage(List<ItemDto> items, long total) {}

    private final ItemRepository itemRepository;

    public CartViewService(ItemRepository itemRepository) {
        this.itemRepository = itemRepository;
    }

    @Transactional(readOnly = true)
    public CartPage getCartPage() {
        List<ItemDto> items = itemRepository.findCartItems();

        long total = items.stream()
                .mapToLong(i -> i.price() * (long) i.count())
                .sum();

        return new CartPage(items, total);
    }
}
