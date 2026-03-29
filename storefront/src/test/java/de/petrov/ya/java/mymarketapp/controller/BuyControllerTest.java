package de.petrov.ya.java.mymarketapp.controller;

import de.petrov.ya.java.mymarketapp.dto.page.ItemDto;
import de.petrov.ya.java.mymarketapp.service.BuyService;
import de.petrov.ya.java.mymarketapp.service.CartViewService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.mockito.Mockito.when;

@WebFluxTest(controllers = BuyController.class)
@Import(BuyController.class)
class BuyControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private BuyService buyService;

    @MockitoBean
    private CartViewService cartViewService;

    @Test
    void buy_whenSuccessful_shouldRedirectToOrders() {
        when(buyService.buy()).thenReturn(Mono.just(42L));

        webTestClient.post()
                .uri("/buy")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/orders");
    }

    @Test
    void buy_whenPaymentFails_shouldReturnCartPageWithErrorMessage() {
        List<ItemDto> items = List.of(
                new ItemDto(1L, "Coffee", "desc", "/img/1.png", 100L, 2),
                new ItemDto(2L, "Tea", "desc", "/img/2.png", 50L, 1)
        );

        CartViewService.CartPage cartPage =
                new CartViewService.CartPage(items, 250L, 100L);

        when(buyService.buy())
                .thenReturn(Mono.error(new IllegalStateException("Payment failed")));

        when(cartViewService.getCartPage())
                .thenReturn(Mono.just(cartPage));

        webTestClient.post()
                .uri("/buy")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(body -> {
                    org.assertj.core.api.Assertions.assertThat(body)
                            .contains("Недостаточно средств для оплаты.");
                    org.assertj.core.api.Assertions.assertThat(body)
                            .contains("Coffee");
                    org.assertj.core.api.Assertions.assertThat(body)
                            .contains("Tea");
                });
    }

    @Test
    void buy_whenCartIsEmpty_shouldReturnCartPageWithErrorMessage() {
        CartViewService.CartPage cartPage =
                new CartViewService.CartPage(List.of(), 0L, 1000L);

        when(buyService.buy())
                .thenReturn(Mono.error(new IllegalStateException("Cart is empty")));

        when(cartViewService.getCartPage())
                .thenReturn(Mono.just(cartPage));

        webTestClient.post()
                .uri("/buy")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(body -> {
                    org.assertj.core.api.Assertions.assertThat(body)
                            .contains("Корзина пуста.");
                });
    }

    @Test
    void buy_whenUnexpectedIllegalStateException_shouldReturnOriginalMessage() {
        CartViewService.CartPage cartPage =
                new CartViewService.CartPage(List.of(), 0L, 1000L);

        when(buyService.buy())
                .thenReturn(Mono.error(new IllegalStateException("Something went wrong")));

        when(cartViewService.getCartPage())
                .thenReturn(Mono.just(cartPage));

        webTestClient.post()
                .uri("/buy")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(body -> {
                    org.assertj.core.api.Assertions.assertThat(body)
                            .contains("Something went wrong");
                });
    }
}