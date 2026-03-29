package de.petrov.ya.java.mymarketapp.controller;

import de.petrov.ya.java.mymarketapp.service.BuyService;
import de.petrov.ya.java.mymarketapp.service.CartViewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import reactor.core.publisher.Mono;

@Controller
@RequiredArgsConstructor
public class BuyController {

    private final BuyService buyService;
    private final CartViewService cartViewService;

    @PostMapping("/buy")
    public Mono<String> buy(Model model) {
        return buyService.buy()
                .thenReturn("redirect:/orders")
                .onErrorResume(IllegalStateException.class, ex ->
                        cartViewService.getCartPage()
                                .doOnNext(page -> {
                                    model.addAttribute("items", page.items());
                                    model.addAttribute("total", page.total());
                                    model.addAttribute("balance", page.balance());
                                    model.addAttribute("errorMessage", mapErrorMessage(ex));
                                })
                                .thenReturn("cart")
                );
    }

    private String mapErrorMessage(IllegalStateException ex) {
        return switch (ex.getMessage()) {
            case "Cart is empty" -> "Корзина пуста.";
            case "Payment failed" -> "Недостаточно средств для оплаты.";
            default -> ex.getMessage();
        };
    }
}