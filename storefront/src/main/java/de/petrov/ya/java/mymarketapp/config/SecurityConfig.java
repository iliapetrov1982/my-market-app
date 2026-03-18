package de.petrov.ya.java.mymarketapp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.RedirectServerAuthenticationEntryPoint;
import org.springframework.security.web.server.authentication.RedirectServerAuthenticationFailureHandler;
import org.springframework.security.web.server.authentication.logout.RedirectServerLogoutSuccessHandler;

import java.net.URI;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    /**
     * Правила доступа к URL-ам:
     * - статика, каталог товаров — публичный доступ;
     * - корзина, покупка, заказы — только для авторизованных;
     * - всё остальное — тоже требует авторизации.
     *
     * При попытке зайти на закрытую страницу без логина —
     * редирект на стандартную форму /login.
     */
    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        RedirectServerLogoutSuccessHandler logoutSuccessHandler =
                new RedirectServerLogoutSuccessHandler();
        logoutSuccessHandler.setLogoutSuccessUrl(URI.create("/"));

        return http
                .authorizeExchange(exchanges -> exchanges
                        // статические ресурсы
                        .pathMatchers("/css/**", "/images/**", "/js/**", "/favicon.ico").permitAll()
                        // каталог товаров — публичный
                        .pathMatchers(HttpMethod.GET, "/", "/items", "/items/**").permitAll()
                        // добавить в корзину со страницы товара — только авторизованным
                        .pathMatchers(HttpMethod.POST, "/items", "/items/**").authenticated()
                        // корзина, покупка, заказы — только авторизованным
                        .pathMatchers("/cart/**", "/buy", "/orders/**").authenticated()
                        // всё остальное — авторизованным
                        .anyExchange().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .authenticationFailureHandler(
                                new RedirectServerAuthenticationFailureHandler("/login?error")
                        )
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessHandler(logoutSuccessHandler)
                )
                // OAuth2 client нужен для Client Credentials (запросы в payments)
                .oauth2Client(Customizer.withDefaults())
                // CSRF отключаем для упрощения тестов server-side rendered форм
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(
                                new RedirectServerAuthenticationEntryPoint("/login")
                        )
                )
                .build();
    }

    /**
     * In-memory пользователи для разработки и тестов.
     * В продакшене заменить на UserDetailsService, читающий из БД.
     */
    @Bean
    public MapReactiveUserDetailsService userDetailsService(PasswordEncoder encoder) {
        UserDetails user1 = User.builder()
                .username("user1")
                .password(encoder.encode("user1pass"))
                .roles("USER")
                .build();

        UserDetails user2 = User.builder()
                .username("user2")
                .password(encoder.encode("user2pass"))
                .roles("USER")
                .build();

        return new MapReactiveUserDetailsService(user1, user2);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}