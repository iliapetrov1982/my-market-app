package de.petrov.ya.java.mymarketapp.service;

import de.petrov.ya.java.mymarketapp.dto.page.ItemDto;
import de.petrov.ya.java.mymarketapp.repository.ItemQueryRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
public class CartViewService {

    public record CartPage(List<ItemDto> items, long total) {}

    private final ItemQueryRepository itemQueryRepository;

    public CartViewService(ItemQueryRepository itemQueryRepository) {
        this.itemQueryRepository = itemQueryRepository;
    }

    public Mono<CartPage> getCartPage() {

        return itemQueryRepository.findCartItems()
                .collectList()
                .map(items -> {

                    long total = items.stream()
                            .mapToLong(i -> i.price() * (long) i.count())
                            .sum();

                    return new CartPage(items, total);
                });
    }
}
