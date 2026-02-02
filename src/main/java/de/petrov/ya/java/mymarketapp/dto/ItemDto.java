package de.petrov.ya.java.mymarketapp.dto;

public record ItemDto(
        long id,
        String title,
        String description,
        String imgPath,
        long price,
        int count
) {
    public static ItemDto placeholder() {
        return new ItemDto(-1L, "", "", "", 0L, 0);
    }
}
