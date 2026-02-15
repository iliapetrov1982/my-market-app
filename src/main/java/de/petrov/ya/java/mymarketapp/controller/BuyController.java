package de.petrov.ya.java.mymarketapp.controller;

import de.petrov.ya.java.mymarketapp.service.BuyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import reactor.core.publisher.Mono;

@Controller
@RequiredArgsConstructor
public class BuyController {

    private final BuyService buyService;

    @PostMapping("/buy")
    public Mono<String> buy() {
        return buyService.buy()
                .map(orderId -> "redirect:/orders/" + orderId + "?newOrder=true");
    }
}
