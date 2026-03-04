package de.petrov.ya.java.mymarketapp.controller;

import de.petrov.ya.java.mymarketapp.service.OrdersService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@Controller
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrdersController {

    private final OrdersService ordersService;

    @GetMapping("")
    public Mono<String> orders(Model model) {
        return ordersService.getOrders()
                .collectList()
                .doOnNext(list -> model.addAttribute("orders", list))
                .thenReturn("orders");
    }

    @GetMapping("/{id}")
    public Mono<String> order(
            @PathVariable long id,
            @RequestParam(defaultValue = "false") boolean newOrder,
            Model model
    ) {
        return ordersService.getOrder(id)
                .doOnNext(dto -> {
                    model.addAttribute("order", dto);
                    model.addAttribute("newOrder", newOrder);
                })
                .thenReturn("order");
    }
}
