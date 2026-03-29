package de.petrov.ya.java.mymarketapp.service;

import de.petrov.ya.java.mymarketapp.config.RedisConfig;
import de.petrov.ya.java.mymarketapp.dto.page.ItemDto;
import de.petrov.ya.java.mymarketapp.repository.ItemQueryRepository;
import de.petrov.ya.java.mymarketapp.service.cache.ItemCacheService;
import de.petrov.ya.java.mymarketapp.service.cache.RedisItemCacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.data.r2dbc.autoconfigure.DataR2dbcAutoConfiguration;
import org.springframework.boot.data.r2dbc.autoconfigure.DataR2dbcRepositoriesAutoConfiguration;
import org.springframework.boot.liquibase.autoconfigure.LiquibaseAutoConfiguration;
import org.springframework.boot.r2dbc.autoconfigure.R2dbcAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.mockito.Mockito.*;

@Testcontainers
@SpringBootTest(
        classes = ItemsServiceRedisIntegrationTest.TestApp.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
class ItemsServiceRedisIntegrationTest {

    private static final String USERNAME = "user1";

    @Container
    static final GenericContainer<?> redis =
            new GenericContainer<>("redis:7-alpine")
                    .withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
        registry.add("app.cache.item-ttl", () -> "PT10M");
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = {
            LiquibaseAutoConfiguration.class,
            R2dbcAutoConfiguration.class,
            DataR2dbcAutoConfiguration.class,
            DataR2dbcRepositoriesAutoConfiguration.class,
            org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration.class,
            org.springframework.boot.jdbc.autoconfigure.DataSourceTransactionManagerAutoConfiguration.class,
            org.springframework.boot.jdbc.autoconfigure.JdbcTemplateAutoConfiguration.class,
            org.springframework.boot.security.oauth2.client.autoconfigure.reactive.ReactiveOAuth2ClientAutoConfiguration.class,
    })
    @Import({RedisConfig.class, RedisItemCacheService.class, TestConfig.class})
    static class TestApp {
    }

    @org.springframework.boot.test.context.TestConfiguration
    static class TestConfig {

        @Bean
        ItemQueryRepository itemQueryRepository() {
            return Mockito.mock(ItemQueryRepository.class);
        }

        @Bean
        ItemsService itemsService(
                ItemQueryRepository itemQueryRepository,
                ItemCacheService itemCacheService
        ) {
            return new ItemsService(itemQueryRepository, itemCacheService);
        }
    }

    @Autowired private ItemsService itemsService;
    @Autowired private ItemQueryRepository itemQueryRepository;
    @Autowired private ReactiveRedisTemplate<String, ItemDto> itemRedisTemplate;

    @BeforeEach
    void setUp() {
        StepVerifier.create(itemRedisTemplate.delete("cache:item:1"))
                .expectNextMatches(deleted -> deleted >= 0)
                .verifyComplete();

        reset(itemQueryRepository);
    }

    @Test
    void getItem_shouldLoadFromRepositoryOnce_andThenServeFromRedis() {
        long id = 1L;
        ItemDto item = new ItemDto(id, "Coffee", "desc", "/img.png", 100L, 0);

        // findItemPage теперь принимает (id, username)
        when(itemQueryRepository.findItemPage(id, USERNAME))
                .thenReturn(Mono.just(item));

        // оборачиваем вызовы в SecurityContext с USERNAME
        var auth = UsernamePasswordAuthenticationToken
                .authenticated(USERNAME, null, List.of());

        Mono<ItemDto> firstCall = itemsService.getItem(id)
                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth));

        Mono<ItemDto> secondCall = itemsService.getItem(id)
                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth));

        // первый вызов — идёт в репозиторий и кладёт в Redis
        StepVerifier.create(firstCall)
                .expectNext(item)
                .verifyComplete();

        // второй вызов — должен отдать из Redis, не трогая репозиторий
        StepVerifier.create(secondCall)
                .expectNext(item)
                .verifyComplete();

        // репозиторий вызван ровно один раз
        verify(itemQueryRepository, times(1)).findItemPage(id, USERNAME);

        // данные реально лежат в Redis
        StepVerifier.create(itemRedisTemplate.opsForValue().get("cache:item:1"))
                .expectNext(item)
                .verifyComplete();
    }
}
