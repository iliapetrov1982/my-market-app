package de.petrov.ya.java.mymarketapp.controller;

import de.petrov.ya.java.mymarketapp.dto.cart.CartAction;
import de.petrov.ya.java.mymarketapp.service.CartCommandService;
import de.petrov.ya.java.mymarketapp.service.CartViewService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class CartController {

    private final CartViewService cartViewService;
    private final CartCommandService cartCommandService;

    public CartController(CartViewService cartViewService, CartCommandService cartCommandService) {
        this.cartViewService = cartViewService;
        this.cartCommandService = cartCommandService;
    }

    @GetMapping("/cart/items")
    public String cart(Model model) {
        var page = cartViewService.getCartPage();
        model.addAttribute("items", page.items());
        model.addAttribute("total", page.total());
        return "cart";
    }

    @PostMapping("/cart/items")
    public String changeCart(
            @RequestParam("id") long id,
            @RequestParam("action") CartAction action,
            Model model
    ) {
        cartCommandService.apply(id, action);

        // По ТЗ возвращаем шаблон cart (не редирект), и обновлённую модель
        var page = cartViewService.getCartPage();
        model.addAttribute("items", page.items());
        model.addAttribute("total", page.total());
        return "cart";
    }
}
