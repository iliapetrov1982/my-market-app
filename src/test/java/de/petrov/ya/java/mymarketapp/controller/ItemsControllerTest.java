package de.petrov.ya.java.mymarketapp.controller;

import de.petrov.ya.java.mymarketapp.dto.cart.CartAction;
import de.petrov.ya.java.mymarketapp.dto.page.ItemDto;
import de.petrov.ya.java.mymarketapp.dto.page.ItemsSort;
import de.petrov.ya.java.mymarketapp.dto.page.Paging;
import de.petrov.ya.java.mymarketapp.service.CartCommandService;
import de.petrov.ya.java.mymarketapp.service.ItemsService;

import java.net.URI;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@WebFluxTest(ItemsController.class)
class ItemsControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private ItemsService itemsService;

    @MockitoBean
    private CartCommandService cartService;

    private static ItemDto dto(long id, int count) {
        return new ItemDto(
                id,
                "T" + id,
                "D" + id,
                "/images/" + id + ".png",
                1000L + id,
                count
        );
    }

    @Test
    void getItems_defaultParams_rendersItemsView_andPutsModelAttributes() {
        ItemsService.ItemsPage page = new ItemsService.ItemsPage(
                List.of(List.of(dto(1, 0), dto(2, 1), ItemDto.placeholder())),
                new Paging(5, 1, false, true),
                "",
                "NO"
        );

        when(itemsService.getItemsPage(any(), any(), anyInt(), anyInt()))
                .thenReturn(reactor.core.publisher.Mono.just(page));

        webTestClient.get()
                .uri("/items")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith("text/html");

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

        verifyNoInteractions(cartService);
    }

    @Test
    void getItems_withParams_passesThemToService_andRendersItems() {
        ItemsService.ItemsPage page = new ItemsService.ItemsPage(
                List.of(List.of(dto(1, 0), dto(2, 0), dto(3, 0))),
                new Paging(10, 2, true, true),
                "coffee",
                "PRICE"
        );

        when(itemsService.getItemsPage(any(), any(), anyInt(), anyInt()))
                .thenReturn(reactor.core.publisher.Mono.just(page));

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/items")
                        .queryParam("search", " coffee ")
                        .queryParam("sort", "PRICE")
                        .queryParam("pageNumber", "2")
                        .queryParam("pageSize", "10")
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith("text/html");

        verify(itemsService, times(1)).getItemsPage(
                org.mockito.ArgumentMatchers.eq(" coffee "),
                org.mockito.ArgumentMatchers.eq(ItemsSort.PRICE),
                org.mockito.ArgumentMatchers.eq(2),
                org.mockito.ArgumentMatchers.eq(10)
        );

        verifyNoInteractions(cartService);
    }

    @Test
    void postItems_changesQuantity_andRedirectsPreservingParams() {
        when(cartService.apply(5L, CartAction.PLUS)).thenReturn(Mono.empty());

        var result = webTestClient.post()
                .uri("/items")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("id", "5")
                        .with("action", "PLUS")
                        .with("search", "q")
                        .with("sort", "ALPHA")
                        .with("pageNumber", "3")
                        .with("pageSize", "20"))
                .exchange()
                .expectStatus().is3xxRedirection()
                .returnResult(Void.class);

        URI location = result.getResponseHeaders().getLocation();
        assertThat("Location header must be present", location, notNullValue());

        UriComponents uc = UriComponentsBuilder.fromUri(location).build();
        assertThat("redirect path", uc.getPath(), equalTo("/items"));

        MultiValueMap<String, String> params = uc.getQueryParams();
        assertThat(params.getFirst("search"), equalTo("q"));
        assertThat(params.getFirst("sort"), equalTo("ALPHA"));
        assertThat(params.getFirst("pageNumber"), equalTo("3"));
        assertThat(params.getFirst("pageSize"), equalTo("20"));

        verify(cartService, times(1)).apply(5L, CartAction.PLUS);
        verifyNoInteractions(itemsService);
    }

    @Test
    void postItems_whenPageNumberOrPageSizeMissing_setsDefaultsInRedirect() {
        when(cartService.apply(7L, CartAction.MINUS)).thenReturn(Mono.empty());

        var result = webTestClient.post()
                .uri("/items")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("id", "7")
                        .with("action", "MINUS")
                        .with("search", "q")
                        .with("sort", "NO"))
                .exchange()
                .expectStatus().is3xxRedirection()
                .returnResult(Void.class);

        URI location = result.getResponseHeaders().getLocation();
        assertThat("Location header must be present", location, notNullValue());

        UriComponents uc = UriComponentsBuilder.fromUri(location).build();
        assertThat("redirect path", uc.getPath(), equalTo("/items"));

        MultiValueMap<String, String> params = uc.getQueryParams();
        assertThat(params.getFirst("search"), equalTo("q"));
        assertThat(params.getFirst("sort"), equalTo("NO"));
        assertThat(params.getFirst("pageNumber"), equalTo("1"));
        assertThat(params.getFirst("pageSize"), equalTo("5"));

        verify(cartService, times(1)).apply(7L, CartAction.MINUS);
        verifyNoInteractions(itemsService);
    }

    @Test
    void getItem_rendersItemView_andAddsItemToModel() {
        when(itemsService.getItem(10L)).thenReturn(reactor.core.publisher.Mono.just(dto(10, 0)));

        webTestClient.get()
                .uri("/items/10")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith("text/html");

        verify(itemsService, times(1)).getItem(10L);
        verifyNoInteractions(cartService);
    }

    @Test
    void postItem_changesQuantity_andRendersItemView_withUpdatedCount() {
        when(cartService.apply(10L, CartAction.PLUS)).thenReturn(reactor.core.publisher.Mono.empty());
        when(itemsService.getItemPage(10L)).thenReturn(reactor.core.publisher.Mono.just(dto(10, 3)));

        webTestClient.post()
                .uri("/items/10")
                .bodyValue("action=PLUS")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith("text/html");

        verify(cartService, times(1)).apply(10L, CartAction.PLUS);
        verify(itemsService, times(1)).getItemPage(10L);
    }
}
