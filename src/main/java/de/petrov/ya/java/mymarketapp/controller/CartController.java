package de.petrov.ya.java.mymarketapp.controller;

import de.petrov.ya.java.mymarketapp.dto.cart.CartAction;
import de.petrov.ya.java.mymarketapp.service.CartCommandService;
import de.petrov.ya.java.mymarketapp.service.CartViewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Controller
@RequiredArgsConstructor
public class CartController {

    private final CartViewService cartViewService;
    private final CartCommandService cartCommandService;

    @GetMapping("/cart/items")
    public Mono<String> cart(Model model) {
        return cartViewService.getCartPage()
                .doOnNext(page -> {
                    model.addAttribute("items", page.items());
                    model.addAttribute("total", page.total());
                })
                .thenReturn("cart");
    }

    // без consumes — чтобы НЕ ловить 415
    @PostMapping("/cart/items")
    public Mono<String> changeCart(ServerWebExchange exchange) {
        MediaType ct = exchange.getRequest().getHeaders().getContentType();

        Mono<MultiValueMap<String, String>> dataMono =
                (ct != null && MediaType.APPLICATION_FORM_URLENCODED.isCompatibleWith(ct))
                        ? exchange.getFormData()
                        : (ct != null && MediaType.MULTIPART_FORM_DATA.isCompatibleWith(ct))
                        ? exchange.getMultipartData()
                        .map(parts -> {
                            // Multipart -> MultiValueMap<String,String>
                            // берём только текстовые значения (name/value)
                            var out = new org.springframework.util.LinkedMultiValueMap<String, String>();
                            parts.forEach((k, v) -> {
                                if (!v.isEmpty() && v.getFirst() instanceof org.springframework.http.codec.multipart.FormFieldPart f) {
                                    out.add(k, f.value());
                                }
                            });
                            return out;
                        })
                        : exchange.getFormData();

        return dataMono.flatMap(form -> {
            String idStr = trimToNull(form.getFirst("id"));
            String actionStr = trimToNull(form.getFirst("action"));

            if (idStr == null || actionStr == null) {
                return Mono.just("redirect:/cart/items");
            }

            long id = Long.parseLong(idStr);
            CartAction action = CartAction.from(actionStr);

            return cartCommandService.apply(id, action)
                    .thenReturn("redirect:/cart/items");
        });
    }

    private static String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
