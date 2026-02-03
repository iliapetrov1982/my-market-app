package de.petrov.ya.java.mymarketapp.dto.page;

public record Paging(
        int pageSize,
        int pageNumber,
        boolean hasPrevious,
        boolean hasNext
) {}
