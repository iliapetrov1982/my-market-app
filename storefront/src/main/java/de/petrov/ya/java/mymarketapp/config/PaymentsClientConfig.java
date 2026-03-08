package de.petrov.ya.java.mymarketapp.config;

import de.petrov.ya.java.mymarketapp.payments.api.PaymentsApi;
import de.petrov.ya.java.mymarketapp.payments.invoker.ApiClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class PaymentsClientConfig {

    @Bean
    WebClient paymentsWebClient() {
        return WebClient.builder().build();
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