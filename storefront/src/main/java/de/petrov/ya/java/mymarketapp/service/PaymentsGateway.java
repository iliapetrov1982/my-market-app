package de.petrov.ya.java.mymarketapp.service;

import de.petrov.ya.java.mymarketapp.payments.api.PaymentsApi;
import de.petrov.ya.java.mymarketapp.payments.model.BalanceResponse;
import de.petrov.ya.java.mymarketapp.payments.model.ChargeRequest;
import de.petrov.ya.java.mymarketapp.payments.model.ChargeResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class PaymentsGateway {

    private final PaymentsApi paymentsApi;

    public Mono<Long> getBalance() {
        return paymentsApi.getBalance()
                .map(BalanceResponse::getAmount);
    }

    public Mono<Boolean> charge(long amount) {
        ChargeRequest request = new ChargeRequest();
        request.setAmount(amount);

        return paymentsApi.charge(request)
                .map(ChargeResponse::getSuccess);
    }
}