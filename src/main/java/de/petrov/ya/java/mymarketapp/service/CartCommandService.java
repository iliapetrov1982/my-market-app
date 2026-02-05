package de.petrov.ya.java.mymarketapp.service;

import de.petrov.ya.java.mymarketapp.dto.cart.CartAction;
import de.petrov.ya.java.mymarketapp.entity.CartItem;
import de.petrov.ya.java.mymarketapp.entity.Item;
import de.petrov.ya.java.mymarketapp.repository.CartItemRepository;
import de.petrov.ya.java.mymarketapp.repository.ItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class CartCommandService {

    private final CartItemRepository cartItemRepository;
    private final ItemRepository itemRepository;

    public void apply(long itemId, CartAction action) {
        CartItem ci = cartItemRepository.findById(itemId).orElse(null);

        switch (action) {
            case PLUS -> add(itemId, ci);
            case MINUS -> decrement(ci);
            case DELETE -> delete(ci);
        }
    }

    private void add(long itemId, CartItem ci) {
        if (ci == null) {
            Item itemRef = itemRepository.getReferenceById(itemId);
            cartItemRepository.save(new CartItem(itemRef, 1));
        } else {
            ci.setQuantity(ci.getQuantity() + 1);
        }
    }

    private void decrement(CartItem ci) {
        if (ci == null) return;

        int next = ci.getQuantity() - 1;
        if (next <= 0) {
            cartItemRepository.delete(ci);
        } else {
            ci.setQuantity(next);
        }
    }

    private void delete(CartItem ci) {
        if (ci != null) {
            cartItemRepository.delete(ci);
        }
    }
}

