package de.petrov.ya.java.mymarketapp.controller;

import de.petrov.ya.java.mymarketapp.dto.cart.CartAction;
import de.petrov.ya.java.mymarketapp.dto.page.ItemDto;
import de.petrov.ya.java.mymarketapp.dto.page.ItemsSort;
import de.petrov.ya.java.mymarketapp.dto.page.Paging;
import de.petrov.ya.java.mymarketapp.service.CartCommandService;
import de.petrov.ya.java.mymarketapp.service.ItemsService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(ItemsController.class)
class ItemsControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    ItemsService itemsService;

    @MockitoBean
    CartCommandService cartService;

    private static ItemDto dto(long id, int count) {
        return new ItemDto(
                id, "T" + id, "D" + id, "/images/" + id + ".png", 1000L + id, count
        );
    }

    @Test
    void getItems_defaultParams_rendersItemsView_andPutsModelAttributes() throws Exception {
        var page = new ItemsService.ItemsPage(
                List.of(List.of(dto(1, 0), dto(2, 1), ItemDto.placeholder())),
                new Paging(5, 1, false, true),
                "",              // safeSearch
                "NO"             // sort
        );
        when(itemsService.getItemsPage(any(), any(), anyInt(), anyInt())).thenReturn(page);

        mockMvc.perform(get("/items"))
                .andExpect(status().isOk())
                .andExpect(view().name("items"))
                .andExpect(model().attributeExists("items", "search", "sort", "paging"))
                .andExpect(model().attribute("search", equalTo("")))
                .andExpect(model().attribute("sort", equalTo("NO")));

        // Проверяем, что контроллер передал дефолты pageNumber/pageSize = 1/5 и sort=NO
        ArgumentCaptor<String> searchCap = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<ItemsSort> sortCap = ArgumentCaptor.forClass(ItemsSort.class);
        ArgumentCaptor<Integer> pageNumberCap = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<Integer> pageSizeCap = ArgumentCaptor.forClass(Integer.class);

        verify(itemsService, times(1)).getItemsPage(
                searchCap.capture(),
                sortCap.capture(),
                pageNumberCap.capture(),
                pageSizeCap.capture()
        );

        assertThat(searchCap.getValue(), nullValue()); // /items без search => null
        assertThat(sortCap.getValue(), equalTo(ItemsSort.NO));
        assertThat(pageNumberCap.getValue(), equalTo(1));
        assertThat(pageSizeCap.getValue(), equalTo(5));
    }

    @Test
    void getItems_withParams_passesThemToService_andRendersItems() throws Exception {
        var page = new ItemsService.ItemsPage(
                List.of(List.of(dto(1, 0), dto(2, 0), dto(3, 0))),
                new Paging(10, 2, true, true),
                "coffee",
                "PRICE"
        );
        when(itemsService.getItemsPage(any(), any(), anyInt(), anyInt())).thenReturn(page);

        mockMvc.perform(get("/items")
                        .param("search", " coffee ")
                        .param("sort", "PRICE")
                        .param("pageNumber", "2")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(view().name("items"))
                .andExpect(model().attribute("search", equalTo("coffee")))
                .andExpect(model().attribute("sort", equalTo("PRICE")))
                .andExpect(model().attributeExists("items", "paging"));

        verify(itemsService).getItemsPage(eq(" coffee "), eq(ItemsSort.PRICE), eq(2), eq(10));
    }

    @Test
    void postItems_changesQuantity_andRedirectsPreservingParams() throws Exception {
        mockMvc.perform(post("/items")
                        .param("id", "5")
                        .param("action", "PLUS")
                        .param("search", "q")
                        .param("sort", "ALPHA")
                        .param("pageNumber", "3")
                        .param("pageSize", "20"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/items?search=q&sort=ALPHA&pageNumber=3&pageSize=20"));

        verify(cartService).apply(5L, CartAction.PLUS);
        // itemsService тут не должен дергаться
        verifyNoInteractions(itemsService);
    }

    @Test
    void postItems_whenPageNumberOrPageSizeMissing_setsDefaultsInRedirect() throws Exception {
        mockMvc.perform(post("/items")
                        .param("id", "7")
                        .param("action", "MINUS")
                        .param("search", "q")
                        .param("sort", "NO"))
                .andExpect(status().is3xxRedirection())
                // default pageNumber=1, pageSize=5
                .andExpect(redirectedUrl("/items?search=q&sort=NO&pageNumber=1&pageSize=5"));

        verify(cartService).apply(7L, CartAction.MINUS);
    }

    @Test
    void getItem_rendersItemView_andAddsItemToModel() throws Exception {
        when(itemsService.getItem(10L)).thenReturn(dto(10, 0));

        mockMvc.perform(get("/items/10"))
                .andExpect(status().isOk())
                .andExpect(view().name("item"))
                .andExpect(model().attributeExists("item"))
                .andExpect(model().attribute("item", notNullValue()));

        verify(itemsService).getItem(10L);
        verifyNoInteractions(cartService);
    }

    @Test
    void postItem_changesQuantity_andRendersItemView_withUpdatedCount() throws Exception {
        when(itemsService.getItemPage(10L)).thenReturn(dto(10, 3));

        mockMvc.perform(post("/items/10")
                        .param("action", "PLUS"))
                .andExpect(status().isOk())
                .andExpect(view().name("item"))
                .andExpect(model().attributeExists("item"));

        verify(cartService).apply(10L, CartAction.PLUS);
        verify(itemsService).getItemPage(10L);
    }
}