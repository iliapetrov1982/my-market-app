package de.petrov.ya.java.mymarketapp.service;

import de.petrov.ya.java.mymarketapp.dto.cart.CartAction;
import de.petrov.ya.java.mymarketapp.entity.CartItem;
import de.petrov.ya.java.mymarketapp.repository.CartItemRepository;
import de.petrov.ya.java.mymarketapp.repository.ItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CartCommandService {

    private final CartItemRepository cartItemRepository;
    private final ItemRepository itemRepository;

    public CartCommandService(CartItemRepository cartItemRepository, ItemRepository itemRepository) {
        this.cartItemRepository = cartItemRepository;
        this.itemRepository = itemRepository;
    }

    @Transactional
    public void apply(long itemId, CartAction action) {
        CartItem ci = cartItemRepository.findById(itemId).orElse(null);

        switch (action) {
            case PLUS -> {
                if (ci == null) {
                    // создаём запись в корзине
                    var itemRef = itemRepository.getReferenceById(itemId);

                    CartItem created = new CartItem();
                    created.setItem(itemRef);      // @MapsId проставит itemId
                    created.setQuantity(1);

                    cartItemRepository.save(created);
                } else {
                    ci.setQuantity(ci.getQuantity() + 1);
                    // save не обязателен, но можно оставить явно:
                    cartItemRepository.save(ci);
                }
            }

            case MINUS -> {
                if (ci == null) return;

                int next = ci.getQuantity() - 1;
                if (next <= 0) {
                    cartItemRepository.delete(ci);
                } else {
                    ci.setQuantity(next);
                    cartItemRepository.save(ci);
                }
            }

            case DELETE -> {
                if (ci != null) {
                    cartItemRepository.delete(ci);
                }
            }
        }
    }
}
