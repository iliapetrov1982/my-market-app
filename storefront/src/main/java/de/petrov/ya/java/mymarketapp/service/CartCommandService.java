package de.petrov.ya.java.mymarketapp.service;

import de.petrov.ya.java.mymarketapp.dto.cart.CartAction;
import de.petrov.ya.java.mymarketapp.entity.CartItem;
import de.petrov.ya.java.mymarketapp.repository.CartItemRepository;
import de.petrov.ya.java.mymarketapp.service.cache.ItemCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class CartCommandService {

    private final CartItemRepository cartItemRepository;
    private final ItemCacheService itemCacheService;

    public Mono<Void> apply(long itemId, CartAction action) {
        return currentUsername()
                .flatMap(username -> applyForUser(itemId, action, username));
    }

    private Mono<Void> applyForUser(long itemId, CartAction action, String username) {
        return cartItemRepository.findByItemIdAndUsername(itemId, username)
                .flatMap(ci -> switch (action) {
                    case PLUS   -> incrementExisting(ci).thenReturn(true);
                    case MINUS  -> decrementExisting(ci).thenReturn(true);
                    case DELETE -> cartItemRepository.delete(ci).thenReturn(true);
                })
                .switchIfEmpty(switch (action) {
                    case PLUS          -> createNew(itemId, username).thenReturn(true);
                    case MINUS, DELETE -> Mono.just(false);
                })
                .flatMap(changed -> changed ? evict(itemId) : Mono.empty())
                .then();
    }

    private Mono<CartItem> createNew(long itemId, String username) {
        return cartItemRepository.save(CartItem.newRow(itemId, 1, username));
    }

    private Mono<CartItem> incrementExisting(CartItem ci) {
        ci.setQuantity(ci.getQuantity() + 1);
        return cartItemRepository.save(ci);
    }

    private Mono<Void> decrementExisting(CartItem ci) {
        int next = ci.getQuantity() - 1;
        if (next <= 0) {
            return cartItemRepository.delete(ci);
        }
        ci.setQuantity(next);
        return cartItemRepository.save(ci).then();
    }

    private Mono<Void> evict(long itemId) {
        return itemCacheService.evict(itemId).then();
    }

    private static Mono<String> currentUsername() {
        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> ctx.getAuthentication().getName());
    }
}
