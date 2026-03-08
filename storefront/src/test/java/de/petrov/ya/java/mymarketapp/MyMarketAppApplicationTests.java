package de.petrov.ya.java.mymarketapp;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.web.reactive.function.client.WebClient;

@Import({
        TestcontainersConfiguration.class,
        MyMarketAppApplicationTests.TestConfig.class
})
@SpringBootTest(
        classes = MyMarketAppApplication.class,
        properties = {
                "payments.base-url=http://localhost:8081"
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
    }
}