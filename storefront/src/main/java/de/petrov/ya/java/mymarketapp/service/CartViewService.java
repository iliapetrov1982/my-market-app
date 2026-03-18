package de.petrov.ya.java.mymarketapp.service;

import de.petrov.ya.java.mymarketapp.dto.page.ItemDto;
import de.petrov.ya.java.mymarketapp.repository.ItemQueryRepository;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
public class CartViewService {

    public record CartPage(List<ItemDto> items, long total, long balance) {}

    private final ItemQueryRepository itemQueryRepository;
    private final PaymentsGateway paymentsGateway;

    public CartViewService(
            ItemQueryRepository itemQueryRepository,
            PaymentsGateway paymentsGateway
    ) {
        this.itemQueryRepository = itemQueryRepository;
        this.paymentsGateway = paymentsGateway;
    }

    public Mono<CartPage> getCartPage() {
        return currentUsername()
                .flatMap(username -> {
                    Mono<List<ItemDto>> itemsMono =
                            itemQueryRepository.findCartItems(username).collectList();
                    Mono<Long> balanceMono = paymentsGateway.getBalance();

                    return Mono.zip(itemsMono, balanceMono)
                            .map(tuple -> {
                                List<ItemDto> items = tuple.getT1();
                                long balance = tuple.getT2();
                                long total = items.stream()
                                        .mapToLong(i -> i.price() * (long) i.count())
                                        .sum();
                                return new CartPage(items, total, balance);
                            });
                });
    }

    private static Mono<String> currentUsername() {
        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> ctx.getAuthentication().getName());
    }
}
