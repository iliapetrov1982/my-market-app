package de.petrov.ya.java.payments.controller;

import de.petrov.ya.java.payments.config.SecurityConfig;
import de.petrov.ya.java.payments.service.PaymentsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@WebFluxTest(controllers = PaymentsController.class)
@Import({SecurityConfig.class, PaymentsControllerSecurityTest.TestConfig.class})
class PaymentsControllerSecurityTest {

    @org.springframework.boot.test.context.TestConfiguration
    static class TestConfig {
        @Bean
        ReactiveJwtDecoder reactiveJwtDecoder() {
            ReactiveJwtDecoder decoder = mock(ReactiveJwtDecoder.class);
            Jwt jwt = Jwt.withTokenValue("mock-token")
                    .header("alg", "RS256")
                    .subject("user1")
                    .issuedAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(3600))
                    .build();
            when(decoder.decode(any())).thenReturn(Mono.just(jwt));
            return decoder;
        }
    }

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private PaymentsService paymentsService;

    // -------------------------------------------------------------------------
    // Без токена — 401
    // -------------------------------------------------------------------------

    @Test
    void getBalance_withoutToken_shouldReturn401() {
        webTestClient.get()
                .uri("/api/payments/balance")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void charge_withoutToken_shouldReturn401() {
        webTestClient.post()
                .uri("/api/payments/charge")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    // -------------------------------------------------------------------------
    // С пользователем через @WithMockUser — проходит
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(username = "user1")
    void getBalance_withAuthenticatedUser_shouldReturn200() {
        when(paymentsService.getBalance(anyString())).thenReturn(50_000L);

        webTestClient.get()
                .uri("/api/payments/balance")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.amount").isEqualTo(50_000);
    }

    @Test
    @WithMockUser(username = "user1")
    void charge_withAuthenticatedUser_shouldReturn200() {
        when(paymentsService.charge(anyString(), anyLong())).thenReturn(true);
        when(paymentsService.getBalance(anyString())).thenReturn(49_000L);

        webTestClient.post()
                .uri("/api/payments/charge")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"amount\": 1000}")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.remainingAmount").isEqualTo(49_000);
    }
}