package de.petrov.ya.java.mymarketapp;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager;
import org.springframework.web.reactive.function.client.WebClient;

import static org.mockito.Mockito.mock;

@Import({
        TestcontainersConfiguration.class,
        MyMarketAppApplicationTests.TestConfig.class
})
@SpringBootTest(
        classes = MyMarketAppApplication.class,
        properties = {
                "payments.base-url=http://localhost:8081",
                "spring.security.oauth2.client.registration.payments-client.client-id=test-client",
                "spring.security.oauth2.client.registration.payments-client.client-secret=test-secret",
                "spring.security.oauth2.client.registration.payments-client.authorization-grant-type=client_credentials",
                "spring.security.oauth2.client.provider.payments-client.token-uri=http://localhost:9999/token"
        }
)
public class MyMarketAppApplicationTests {

    @Test
    void contextLoads() {
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        WebClient webClient() {
            return WebClient.builder().build();
        }

        /**
         * Мок менеджера OAuth2-клиентов — реальный Keycloak не нужен в тестах.
         * PaymentsClientConfig использует этот бин для получения токена.
         */
        @Bean
        ReactiveOAuth2AuthorizedClientManager reactiveOAuth2AuthorizedClientManager() {
            return mock(ReactiveOAuth2AuthorizedClientManager.class);
        }
    }
}