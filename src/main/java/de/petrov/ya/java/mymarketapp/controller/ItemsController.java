package de.petrov.ya.java.mymarketapp.controller;

import de.petrov.ya.java.mymarketapp.dto.cart.CartAction;
import de.petrov.ya.java.mymarketapp.dto.page.ItemsSort;
import de.petrov.ya.java.mymarketapp.service.CartCommandService;
import de.petrov.ya.java.mymarketapp.service.ItemsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

@Controller
@RequiredArgsConstructor
public class ItemsController {

    private final ItemsService itemsService;
    private final CartCommandService cartService;

    @GetMapping({"/", "/items"})
    public Mono<String> items(
            @RequestParam(required = false) String search,
            @RequestParam(required = false, defaultValue = "NO") String sort,
            @RequestParam(required = false, defaultValue = "1") Integer pageNumber,
            @RequestParam(required = false, defaultValue = "5") Integer pageSize,
            Model model
    ) {
        int pn = (pageNumber == null) ? 1 : pageNumber;
        int ps = (pageSize == null) ? 5 : pageSize;

        return itemsService.getItemsPage(search, ItemsSort.from(sort), pn, ps)
                .doOnNext(page -> {
                    model.addAttribute("items", page.items());
                    model.addAttribute("search", page.search());
                    model.addAttribute("sort", page.sort());
                    model.addAttribute("paging", page.paging());
                })
                .thenReturn("items");
    }

    /**
     * ТЗ: POST /items?id=&search=&sort=&pageNumber=&pageSize=&action=
     * Реально кнопки из HTML шлют это как form-urlencoded body — читаем formData руками (WebFlux-safe).
     */
    @PostMapping("/items")
    public Mono<String> changeItemCount(ServerWebExchange exchange) {
        return exchange.getFormData()
                .flatMap(form -> {
                    String idStr = trimToNull(form.getFirst("id"));
                    String actionStr = trimToNull(form.getFirst("action"));

                    String search = form.getFirst("search");
                    String sort = trimToNull(form.getFirst("sort"));
                    Integer pageNumber = parseIntOrDefault(form.getFirst("pageNumber"), 1);
                    Integer pageSize = parseIntOrDefault(form.getFirst("pageSize"), 5);

                    String redirectUrl = UriComponentsBuilder.fromPath("/items")
                            .queryParam("search", search)
                            .queryParam("sort", (sort == null ? "NO" : sort))
                            .queryParam("pageNumber", pageNumber)
                            .queryParam("pageSize", pageSize)
                            .build()
                            .toUriString();

                    if (idStr == null || actionStr == null) {
                        return Mono.just("redirect:" + redirectUrl);
                    }

                    long id = Long.parseLong(idStr);
                    return cartService.apply(id, CartAction.from(actionStr))
                            .thenReturn("redirect:" + redirectUrl);
                });
    }


    @GetMapping("/items/{id}")
    public Mono<String> item(@PathVariable long id, Model model) {
        return itemsService.getItem(id)
                .doOnNext(dto -> model.addAttribute("item", dto))
                .thenReturn("item");
    }

    @PostMapping("/items/{id}")
    public Mono<String> changeItemCount(@PathVariable long id, ServerWebExchange exchange, Model model) {
        return exchange.getFormData()
                .flatMap(form -> {
                    String actionStr = trimToNull(form.getFirst("action"));
                    if (actionStr == null) {
                        return itemsService.getItemPage(id)
                                .doOnNext(dto -> model.addAttribute("item", dto))
                                .thenReturn("item");
                    }
                    return cartService.apply(id, CartAction.from(actionStr))
                            .then(itemsService.getItemPage(id))
                            .doOnNext(dto -> model.addAttribute("item", dto))
                            .thenReturn("item");
                });
    }

    private static String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static Integer parseIntOrDefault(String s, int def) {
        try {
            if (s == null) return def;
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return def;
        }
    }
}
