package de.petrov.ya.java.mymarketapp.service.cache;

import de.petrov.ya.java.mymarketapp.dto.page.ItemDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Service
public class RedisItemCacheService implements ItemCacheService {

    private static final String KEY_PREFIX = "cache:item:";

    private final ReactiveRedisTemplate<String, ItemDto> itemRedisTemplate;
    private final Duration itemTtl;

    public RedisItemCacheService(
            ReactiveRedisTemplate<String, ItemDto> itemRedisTemplate,
            @Value("${app.cache.item-ttl:PT10M}") Duration itemTtl
    ) {
        this.itemRedisTemplate = itemRedisTemplate;
        this.itemTtl = itemTtl;
    }

    @Override
    public Mono<ItemDto> get(long itemId) {
        return itemRedisTemplate.opsForValue().get(key(itemId));
    }

    @Override
    public Mono<Boolean> put(long itemId, ItemDto item) {
        return itemRedisTemplate.opsForValue().set(key(itemId), item, itemTtl);
    }

    @Override
    public Mono<Boolean> evict(long itemId) {
        return itemRedisTemplate.delete(key(itemId)).map(deleted -> deleted > 0);
    }

    private String key(long itemId) {
        return KEY_PREFIX + itemId;
    }
}