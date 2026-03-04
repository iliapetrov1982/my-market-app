package de.petrov.ya.java.mymarketapp.dto.page;

public enum ItemsSort {
    NO, ALPHA, PRICE;

    public static ItemsSort from(String raw) {
        if (raw == null || raw.isBlank()) return NO;
        try {
            return ItemsSort.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException e) {
            return NO;
        }
    }
}
