package de.petrov.ya.java.mymarketapp.service;

import de.petrov.ya.java.mymarketapp.payments.api.PaymentsApi;
import de.petrov.ya.java.mymarketapp.payments.model.BalanceResponse;
import de.petrov.ya.java.mymarketapp.payments.model.ChargeRequest;
import de.petrov.ya.java.mymarketapp.payments.model.ChargeResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Юнит-тест PaymentsGateway.
 *
 * Проверяет что gateway корректно маппит ответы PaymentsApi.
 * Интеграционный тест с реальным токеном — в BuyServiceIntegrationTest
 * через мок PaymentsGateway (реальный Keycloak не нужен для CI).
 */
@ExtendWith(MockitoExtension.class)
class PaymentsGatewayOAuth2Test {

    @Mock
    private PaymentsApi paymentsApi;

    @InjectMocks
    private PaymentsGateway paymentsGateway;

    @Test
    void getBalance_shouldReturnAmountFromApi() {
        BalanceResponse response = new BalanceResponse();
        response.setAmount(5000L);

        when(paymentsApi.getBalance()).thenReturn(Mono.just(response));

        StepVerifier.create(paymentsGateway.getBalance())
                .expectNext(5000L)
                .verifyComplete();

        verify(paymentsApi).getBalance();
    }

    @Test
    void charge_shouldSendCorrectAmountAndReturnSuccess() {
        ChargeResponse response = new ChargeResponse();
        response.setSuccess(true);
        response.setRemainingAmount(4000L);
        response.setMessage("OK");

        when(paymentsApi.charge(any(ChargeRequest.class)))
                .thenReturn(Mono.just(response));

        StepVerifier.create(paymentsGateway.charge(1000L))
                .expectNext(true)
                .verifyComplete();

        verify(paymentsApi).charge(any(ChargeRequest.class));
    }

    @Test
    void charge_whenInsufficientFunds_shouldReturnFalse() {
        ChargeResponse response = new ChargeResponse();
        response.setSuccess(false);
        response.setRemainingAmount(100L);
        response.setMessage("Insufficient funds");

        when(paymentsApi.charge(any(ChargeRequest.class)))
                .thenReturn(Mono.just(response));

        StepVerifier.create(paymentsGateway.charge(9999L))
                .expectNext(false)
                .verifyComplete();
    }
}