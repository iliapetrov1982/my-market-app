package de.petrov.ya.java.mymarketapp.controller;

import de.petrov.ya.java.mymarketapp.service.CartViewService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class CartController {

    private final CartViewService cartViewService;

    public CartController(CartViewService cartViewService) {
        this.cartViewService = cartViewService;
    }

    @GetMapping("/cart/items")
    public String cart(Model model) {
        var page = cartViewService.getCartPage();
        model.addAttribute("items", page.items());
        model.addAttribute("total", page.total());
        return "cart";
    }
}
