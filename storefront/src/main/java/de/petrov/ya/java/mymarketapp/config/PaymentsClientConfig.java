package de.petrov.ya.java.mymarketapp.config;

import de.petrov.ya.java.mymarketapp.payments.api.PaymentsApi;
import de.petrov.ya.java.mymarketapp.payments.invoker.ApiClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class PaymentsClientConfig {

    /**
     * WebClient с OAuth2-фильтром: перед каждым запросом в payments
     * автоматически получает токен у Keycloak по Client Credentials Flow
     * и добавляет заголовок Authorization: Bearer <token>.
     */
    @Bean
    WebClient paymentsWebClient(ReactiveOAuth2AuthorizedClientManager clientManager) {
        ServerOAuth2AuthorizedClientExchangeFilterFunction oauth =
                new ServerOAuth2AuthorizedClientExchangeFilterFunction(clientManager);

        // указываем какую регистрацию использовать по умолчанию
        oauth.setDefaultClientRegistrationId("payments-client");

        return WebClient.builder()
                .filter(oauth)
                .build();
    }

    @Bean
    ApiClient paymentsApiClient(
            WebClient paymentsWebClient,
            @Value("${payments.base-url}") String paymentsBaseUrl
    ) {
        ApiClient apiClient = new ApiClient(paymentsWebClient);
        apiClient.setBasePath(paymentsBaseUrl);
        return apiClient;
    }

    @Bean
    PaymentsApi paymentsApi(ApiClient paymentsApiClient) {
        return new PaymentsApi(paymentsApiClient);
    }
}