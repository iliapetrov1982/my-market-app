package de.petrov.ya.java.payments.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicLong;

@Service
@RequiredArgsConstructor
public class PaymentsService {

    private final AtomicLong balance = new AtomicLong(100_000);

    public long getBalance() {
        return balance.get();
    }

    public synchronized boolean charge(long amount) {

        long current = balance.get();

        if (amount <= 0) {
            return false;
        }

        if (current < amount) {
            return false;
        }

        balance.addAndGet(-amount);
        return true;
    }
}