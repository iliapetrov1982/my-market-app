package de.petrov.ya.java.payments.controller;

import de.petrov.ya.java.payments.api.ApiApi;
import de.petrov.ya.java.payments.model.BalanceResponse;
import de.petrov.ya.java.payments.model.ChargeRequest;
import de.petrov.ya.java.payments.model.ChargeResponse;
import de.petrov.ya.java.payments.service.PaymentsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
public class PaymentsController implements ApiApi {

    private final PaymentsService paymentsService;

    @Override
    public Mono<ResponseEntity<BalanceResponse>> getBalance(ServerWebExchange exchange) {
        return currentUsername(exchange)
                .map(username -> {
                    long balance = paymentsService.getBalance(username);

                    BalanceResponse response = new BalanceResponse();
                    response.setAmount(balance);

                    return ResponseEntity.ok(response);
                });
    }

    @Override
    public Mono<ResponseEntity<ChargeResponse>> charge(
            Mono<ChargeRequest> chargeRequest,
            ServerWebExchange exchange
    ) {
        return currentUsername(exchange)
                .flatMap(username -> chargeRequest.map(request -> {
                    boolean success = paymentsService.charge(username, request.getAmount());

                    ChargeResponse response = new ChargeResponse();
                    response.setSuccess(success);
                    response.setRemainingAmount(paymentsService.getBalance(username));
                    response.setMessage(success ? "OK" : "Insufficient funds");

                    return ResponseEntity.ok(response);
                }));
    }

    /**
     * Извлекает username из JWT токена через реактивный SecurityContext.
     */
    private static Mono<String> currentUsername(ServerWebExchange exchange) {
        return exchange.getPrincipal()
                .cast(Authentication.class)
                .map(Authentication::getName);
    }
}
