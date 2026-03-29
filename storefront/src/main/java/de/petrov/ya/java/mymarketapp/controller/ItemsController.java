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

import java.util.Set;

@Controller
@RequiredArgsConstructor
public class ItemsController {

    private static final int DEFAULT_PAGE_NUMBER = 1;
    private static final int DEFAULT_PAGE_SIZE = 5;

    private static final Set<Integer> ALLOWED_PAGE_SIZES =
            Set.of(2, 5, 10, 20, 50, 100);

    private final ItemsService itemsService;
    private final CartCommandService cartService;

    // =========================
    // GET /items
    // =========================

    @GetMapping({"/", "/items"})
    public Mono<String> items(
            @RequestParam(required = false) String search,
            @RequestParam(required = false, defaultValue = "NO") String sort,
            @RequestParam(required = false, defaultValue = "1") Integer pageNumber,
            @RequestParam(required = false, defaultValue = "5") Integer pageSize,
            Model model
    ) {
        int pn = normalizePageNumber(pageNumber);
        int ps = normalizePageSize(pageSize);

        return itemsService.getItemsPage(search, ItemsSort.from(sort), pn, ps)
                .doOnNext(page -> {
                    model.addAttribute("items", page.items());
                    model.addAttribute("search", page.search());
                    model.addAttribute("sort", page.sort());
                    model.addAttribute("paging", page.paging());
                })
                .thenReturn("items");
    }

    // =========================
    // POST /items
    // =========================

    @PostMapping("/items")
    public Mono<String> changeItemCount(ServerWebExchange exchange) {
        return exchange.getFormData()
                .flatMap(form -> {

                    String search = form.getFirst("search");
                    String sort = trimToNull(form.getFirst("sort"));

                    int pageNumber = normalizePageNumber(
                            parseIntOrNull(form.getFirst("pageNumber"))
                    );

                    int pageSize = normalizePageSize(
                            parseIntOrNull(form.getFirst("pageSize"))
                    );

                    String redirectUrl = buildRedirectUrl(
                            search, sort, pageNumber, pageSize
                    );

                    Long id = parsePositiveLongOrNull(form.getFirst("id"));
                    String actionStr = trimToNull(form.getFirst("action"));

                    if (id == null || actionStr == null) {
                        return redirect(redirectUrl);
                    }

                    return cartService.apply(id, CartAction.from(actionStr))
                            .then(redirect(redirectUrl));
                });
    }

    // =========================
    // GET /items/{id}
    // =========================

    @GetMapping("/items/{id}")
    public Mono<String> item(@PathVariable long id, Model model) {
        if (id <= 0) {
            return redirect("/items");
        }

        return itemsService.getItem(id)
                .doOnNext(dto -> model.addAttribute("item", dto))
                .thenReturn("item");
    }

    // =========================
    // POST /items/{id}
    // =========================

    @PostMapping("/items/{id}")
    public Mono<String> changeItemCount(
            @PathVariable long id,
            ServerWebExchange exchange,
            Model model
    ) {
        if (id <= 0) {
            return redirect("/items");
        }

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

    // =========================
    // Redirect builder
    // =========================

    private static String buildRedirectUrl(
            String search,
            String sort,
            int pageNumber,
            int pageSize
    ) {
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromPath("/items")
                .queryParam("sort", sort == null ? "NO" : sort)
                .queryParam("pageNumber", pageNumber)
                .queryParam("pageSize", pageSize);

        if (search != null) {
            builder.queryParam("search", search);
        }

        return builder.build().toUriString();
    }

    private static Mono<String> redirect(String url) {
        return Mono.just("redirect:" + url);
    }

    // =========================
    // Validation helpers
    // =========================

    private static int normalizePageNumber(Integer pn) {
        return (pn == null || pn < 1)
                ? DEFAULT_PAGE_NUMBER
                : pn;
    }

    private static int normalizePageSize(Integer ps) {
        if (ps == null) return DEFAULT_PAGE_SIZE;
        return ALLOWED_PAGE_SIZES.contains(ps)
                ? ps
                : DEFAULT_PAGE_SIZE;
    }

    private static Integer parseIntOrNull(String s) {
        try {
            if (s == null) return null;
            String t = s.trim();
            if (t.isEmpty()) return null;
            return Integer.parseInt(t);
        } catch (Exception e) {
            return null;
        }
    }

    private static Long parsePositiveLongOrNull(String s) {
        try {
            if (s == null) return null;
            String t = s.trim();
            if (t.isEmpty()) return null;
            long v = Long.parseLong(t);
            return v > 0 ? v : null;
        } catch (Exception e) {
            return null;
        }
    }

    private static String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
