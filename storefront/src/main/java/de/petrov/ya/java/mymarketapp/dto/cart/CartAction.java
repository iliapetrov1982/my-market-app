package de.petrov.ya.java.mymarketapp.dto.cart;

public enum CartAction {
    PLUS,
    MINUS,
    DELETE;

    public static CartAction from(String raw) {
        if (raw == null) {
            throw new IllegalArgumentException("action is required");
        }
        return CartAction.valueOf(raw.trim().toUpperCase());
    }
}
