package de.petrov.ya.java.mymarketapp.controller;

import de.petrov.ya.java.mymarketapp.service.BuyService;
import de.petrov.ya.java.mymarketapp.service.CartCommandService;
import de.petrov.ya.java.mymarketapp.service.CartViewService;
import de.petrov.ya.java.mymarketapp.service.ItemsService;
import de.petrov.ya.java.mymarketapp.service.OrdersService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UserDetailsRepositoryReactiveAuthenticationManager;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.RedirectServerAuthenticationEntryPoint;
import org.springframework.security.web.server.authentication.RedirectServerAuthenticationFailureHandler;
import org.springframework.security.web.server.authentication.logout.RedirectServerLogoutSuccessHandler;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.net.URI;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;

@WebFluxTest(controllers = {
        ItemsController.class,
        CartController.class,
        BuyController.class,
        OrdersController.class
})
@Import(SecurityAccessControlTest.TestSecurityConfig.class)
class SecurityAccessControlTest {

    /**
     * Минимальный SecurityConfig для тестов: те же правила доступа что и в
     * SecurityConfig, но без oauth2Client(). formLogin подключён с in-memory
     * UserDetailsService чтобы authenticationManager не был null.
     */
    @EnableWebFluxSecurity
    static class TestSecurityConfig {

        @Bean
        @SuppressWarnings("deprecation")
        MapReactiveUserDetailsService userDetailsService() {
            return new MapReactiveUserDetailsService(
                    User.withUsername("user1")
                            .password("user1pass")
                            .passwordEncoder(NoOpPasswordEncoder.getInstance()::encode)
                            .roles("USER")
                            .build()
            );
        }

        @Bean
        ReactiveAuthenticationManager authenticationManager(
                MapReactiveUserDetailsService uds) {
            return new UserDetailsRepositoryReactiveAuthenticationManager(uds);
        }

        @Bean
        SecurityWebFilterChain filterChain(ServerHttpSecurity http,
                                           ReactiveAuthenticationManager authManager) {
            RedirectServerLogoutSuccessHandler logoutSuccess =
                    new RedirectServerLogoutSuccessHandler();
            logoutSuccess.setLogoutSuccessUrl(URI.create("/"));

            return http
                    .authorizeExchange(ex -> ex
                            .pathMatchers("/css/**", "/images/**", "/js/**", "/favicon.ico").permitAll()
                            .pathMatchers(HttpMethod.GET, "/", "/items", "/items/**").permitAll()
                            .pathMatchers(HttpMethod.POST, "/items", "/items/**").authenticated()
                            .pathMatchers("/cart/**", "/buy", "/orders/**").authenticated()
                            .anyExchange().authenticated()
                    )
                    .formLogin(form -> form
                            .authenticationManager(authManager)
                            .loginPage("/login")
                            .authenticationFailureHandler(
                                    new RedirectServerAuthenticationFailureHandler("/login?error")
                            )
                    )
                    .logout(logout -> logout
                            .logoutUrl("/logout")
                            .logoutSuccessHandler(logoutSuccess)
                    )
                    .csrf(ServerHttpSecurity.CsrfSpec::disable)
                    .exceptionHandling(ex -> ex
                            .authenticationEntryPoint(
                                    new RedirectServerAuthenticationEntryPoint("/login")
                            )
                    )
                    .build();
        }
    }

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean private ItemsService itemsService;
    @MockitoBean private CartCommandService cartCommandService;
    @MockitoBean private CartViewService cartViewService;
    @MockitoBean private BuyService buyService;
    @MockitoBean private OrdersService ordersService;

    // -------------------------------------------------------------------------
    // Публичные страницы — доступны без логина (нет редиректа на /login)
    // -------------------------------------------------------------------------

    @Test
    void items_anonymousUser_shouldNotBeRedirectedToLogin() {
        webTestClient.get().uri("/items")
                .exchange()
                .expectStatus().value(status ->
                        assertThat(status, not(equalTo(302))));
    }

    @Test
    void itemDetail_anonymousUser_shouldNotBeRedirectedToLogin() {
        webTestClient.get().uri("/items/1")
                .exchange()
                .expectStatus().value(status ->
                        assertThat(status, not(equalTo(302))));
    }

    // -------------------------------------------------------------------------
    // Защищённые страницы — анонимный пользователь получает редирект на /login
    // -------------------------------------------------------------------------

    @Test
    void cart_anonymousUser_shouldRedirectToLogin() {
        webTestClient.get().uri("/cart/items")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueMatches("Location", ".*/login");
    }

    @Test
    void buy_anonymousUser_shouldRedirectToLogin() {
        webTestClient.post().uri("/buy")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueMatches("Location", ".*/login");
    }

    @Test
    void orders_anonymousUser_shouldRedirectToLogin() {
        webTestClient.get().uri("/orders")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueMatches("Location", ".*/login");
    }

    @Test
    void orderDetail_anonymousUser_shouldRedirectToLogin() {
        webTestClient.get().uri("/orders/1")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueMatches("Location", ".*/login");
    }

    @Test
    void addToCartFromItemPage_anonymousUser_shouldRedirectToLogin() {
        webTestClient.post().uri("/items/1")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueMatches("Location", ".*/login");
    }

    // -------------------------------------------------------------------------
    // Защищённые страницы — авторизованный пользователь проходит
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(username = "user1", roles = "USER")
    void cart_authenticatedUser_shouldNotBeRedirectedToLogin() {
        webTestClient.get().uri("/cart/items")
                .exchange()
                .expectStatus().value(status ->
                        assertThat(status, not(equalTo(302))));
    }

    @Test
    @WithMockUser(username = "user1", roles = "USER")
    void orders_authenticatedUser_shouldNotBeRedirectedToLogin() {
        webTestClient.get().uri("/orders")
                .exchange()
                .expectStatus().value(status ->
                        assertThat(status, not(equalTo(302))));
    }
}