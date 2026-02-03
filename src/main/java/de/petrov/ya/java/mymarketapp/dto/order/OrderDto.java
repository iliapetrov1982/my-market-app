package de.petrov.ya.java.mymarketapp.dto.order;

import java.util.List;

public record OrderDto(
        long id,
        List<OrderItemDto> items,
        long totalSum
) {
}
