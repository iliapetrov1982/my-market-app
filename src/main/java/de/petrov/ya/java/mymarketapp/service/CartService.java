package de.petrov.ya.java.mymarketapp.service;

import de.petrov.ya.java.mymarketapp.dto.cart.CartAction;
import de.petrov.ya.java.mymarketapp.entity.CartItem;
import de.petrov.ya.java.mymarketapp.entity.Item;
import de.petrov.ya.java.mymarketapp.repository.CartItemRepository;
import de.petrov.ya.java.mymarketapp.repository.ItemRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CartService {

    private final CartItemRepository cartItemRepository;
    private final ItemRepository itemRepository;

    public CartService(CartItemRepository cartItemRepository, ItemRepository itemRepository) {
        this.cartItemRepository = cartItemRepository;
        this.itemRepository = itemRepository;
    }

    @Transactional
    public void changeQuantity(long itemId, CartAction action) {
        // убеждаемся, что товар существует (+ получаем entity для CartItem)
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Item not found: " + itemId));

        CartItem ci = cartItemRepository.findById(itemId).orElse(null);

        if (action == CartAction.PLUS) {
            if (ci == null) {
                ci = new CartItem();
                ci.setItem(item);      // важно для @MapsId
                ci.setQuantity(1);
            } else {
                ci.setQuantity(ci.getQuantity() + 1);
            }
            cartItemRepository.save(ci);
            return;
        }

        // MINUS
        if (ci == null) {
            return; // нечего уменьшать
        }

        int newQty = ci.getQuantity() - 1;
        if (newQty <= 0) {
            cartItemRepository.delete(ci);
        } else {
            ci.setQuantity(newQty);
            cartItemRepository.save(ci);
        }
    }
}
