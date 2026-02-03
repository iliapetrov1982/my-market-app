package de.petrov.ya.java.mymarketapp.controller;

import de.petrov.ya.java.mymarketapp.service.BuyService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class BuyController {

    private final BuyService buyService;

    public BuyController(BuyService buyService) {
        this.buyService = buyService;
    }

    @PostMapping("/buy")
    public String buy() {
        long orderId = buyService.buy();
        return "redirect:/orders/" + orderId + "?newOrder=true";
    }
}
