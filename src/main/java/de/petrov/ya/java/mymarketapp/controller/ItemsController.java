package de.petrov.ya.java.mymarketapp.controller;

import de.petrov.ya.java.mymarketapp.dto.ItemsSort;
import de.petrov.ya.java.mymarketapp.service.ItemsService;

import org.springframework.ui.Model;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class ItemsController {
    private final ItemsService itemsService;

    public ItemsController(ItemsService itemsService) {
        this.itemsService = itemsService;
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

        return "items";
    }
}
