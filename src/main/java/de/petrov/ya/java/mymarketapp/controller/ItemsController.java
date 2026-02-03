package de.petrov.ya.java.mymarketapp.controller;

import de.petrov.ya.java.mymarketapp.dto.cart.CartAction;
import de.petrov.ya.java.mymarketapp.dto.page.ItemDto;
import de.petrov.ya.java.mymarketapp.dto.page.ItemsSort;
import de.petrov.ya.java.mymarketapp.service.CartService;
import de.petrov.ya.java.mymarketapp.service.ItemsService;

import org.springframework.ui.Model;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ItemsController {
    private final ItemsService itemsService;
    private final CartService cartService;

    public ItemsController(
            ItemsService itemsService,
            CartService cartService
    ) {
        this.itemsService = itemsService;
        this.cartService = cartService;
    }

    @GetMapping({"/", "/items"})
    public String items(
            @RequestParam(required = false) String search,
            @RequestParam(required = false, defaultValue = "NO") String sort,
            @RequestParam(required = false, defaultValue = "1") Integer pageNumber,
            @RequestParam(required = false, defaultValue = "5") Integer pageSize,
            Model model
    ) {
        var page = itemsService.getItemsPage(
                search,
                ItemsSort.from(sort),
                pageNumber == null ? 1 : pageNumber,
                pageSize == null ? 5 : pageSize
        );

        model.addAttribute("items", page.items());
        model.addAttribute("search", page.search());
        model.addAttribute("sort", page.sort());
        model.addAttribute("paging", page.paging());

        return "items"; // returns items.html
    }

    @PostMapping("/items")
    public String changeItemCount(
            @RequestParam("id") long id,
            @RequestParam(required = false) String search,
            @RequestParam(required = false, defaultValue = "NO") String sort,
            @RequestParam(required = false, defaultValue = "1") Integer pageNumber,
            @RequestParam(required = false, defaultValue = "5") Integer pageSize,
            @RequestParam("action") String action,
            RedirectAttributes ra
    ) {
        cartService.changeQuantity(id, CartAction.from(action));

        ra.addAttribute("search", search);
        ra.addAttribute("sort", sort);
        ra.addAttribute("pageNumber", pageNumber == null ? 1 : pageNumber);
        ra.addAttribute("pageSize", pageSize == null ? 5 : pageSize);

        return "redirect:/items";
    }

    @GetMapping("/items/{id}")
    public String item(@PathVariable long id, Model model) {
        ItemDto dto = itemsService.getItem(id);
        model.addAttribute("item", dto);
        return "item";
    }
}
