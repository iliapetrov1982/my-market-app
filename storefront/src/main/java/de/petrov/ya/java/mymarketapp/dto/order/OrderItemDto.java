package de.petrov.ya.java.mymarketapp.dto.order;

public record OrderItemDto(
        long id,
        String title,
        long price,
        int count
) {
}
