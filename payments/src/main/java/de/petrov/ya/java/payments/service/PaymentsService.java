package de.petrov.ya.java.payments.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Сервис платежей с балансами, привязанными к конкретному пользователю.

 * Каждый пользователь получает начальный баланс при первом обращении.
 * Данные хранятся в памяти — при перезапуске сбрасываются.
 */
@Service
public class PaymentsService {

    private final long initialAmount;
    private final ConcurrentHashMap<String, AtomicLong> balances = new ConcurrentHashMap<>();

    public PaymentsService(
            @Value("${payments.balance.initial-amount:100000}") long initialAmount
    ) {
        this.initialAmount = initialAmount;
    }

    public long getBalance(String username) {
        return balanceFor(username).get();
    }

    public synchronized boolean charge(String username, long amount) {
        if (amount <= 0) {
            return false;
        }

        AtomicLong balance = balanceFor(username);
        long current = balance.get();

        if (current < amount) {
            return false;
        }

        balance.addAndGet(-amount);
        return true;
    }

    private AtomicLong balanceFor(String username) {
        return balances.computeIfAbsent(username, k -> new AtomicLong(initialAmount));
    }
}
