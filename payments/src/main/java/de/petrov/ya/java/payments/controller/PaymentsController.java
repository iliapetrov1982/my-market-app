package de.petrov.ya.java.payments.controller;

import de.petrov.ya.java.payments.api.ApiApi;
import de.petrov.ya.java.payments.model.BalanceResponse;
import de.petrov.ya.java.payments.model.ChargeRequest;
import de.petrov.ya.java.payments.model.ChargeResponse;
import de.petrov.ya.java.payments.service.PaymentsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
public class PaymentsController implements ApiApi {

    private final PaymentsService paymentsService;

    @Override
    public Mono<ResponseEntity<BalanceResponse>> getBalance(ServerWebExchange exchange) {
        long balance = paymentsService.getBalance();

        BalanceResponse response = new BalanceResponse();
        response.setAmount(balance);

        return Mono.just(ResponseEntity.ok(response));
    }

    @Override
    public Mono<ResponseEntity<ChargeResponse>> charge(
            Mono<ChargeRequest> chargeRequest,
            ServerWebExchange exchange
    ) {
        return chargeRequest.map(request -> {
            boolean success = paymentsService.charge(request.getAmount());

            ChargeResponse response = new ChargeResponse();
            response.setSuccess(success);
            response.setRemainingAmount(paymentsService.getBalance());
            response.setMessage(success ? "OK" : "Insufficient funds");

            return ResponseEntity.ok(response);
        });
    }
}