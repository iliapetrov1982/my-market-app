package de.petrov.ya.java.payments;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=https://mock-jwks"
})
class PaymentsApplicationTests {

    @Test
    void contextLoads() {
    }
}

