package de.petrov.ya.java.mymarketapp.service;

import de.petrov.ya.java.mymarketapp.dto.cart.CartAction;
import de.petrov.ya.java.mymarketapp.entity.CartItem;
import de.petrov.ya.java.mymarketapp.repository.CartItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class CartCommandService {

    private final CartItemRepository cartItemRepository;

    public Mono<Void> apply(long itemId, CartAction action) {
        return cartItemRepository.findById(itemId)
                .flatMap(ci -> switch (action) {
                    case PLUS -> incrementExisting(ci);
                    case MINUS -> decrementExisting(ci);
                    case DELETE -> cartItemRepository.delete(ci);
                })
                .switchIfEmpty(switch (action) {
                    case PLUS -> createNew(itemId);
                    case MINUS, DELETE -> Mono.empty(); // нечего делать
                })
                .then();
    }

    private Mono<CartItem> createNew(long itemId) {
        return cartItemRepository.save(CartItem.newRow(itemId, 1));
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
}
