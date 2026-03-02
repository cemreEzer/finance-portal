package com.finance.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Spring Security yapılandırması.
 * Keycloak JWT tokenlarını doğrular, CORS ayarlarını yönetir.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Value("${finance.cors.allowed-origins:http://localhost:3000}")
    private String allowedOrigins;

    private final JwtAuthConverter jwtAuthConverter;

    public SecurityConfig(JwtAuthConverter jwtAuthConverter) {
        this.jwtAuthConverter = jwtAuthConverter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // ── CORS ─────────────────────────────────────────────
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // ── CSRF devre dışı (Stateless JWT) ──────────────────
                .csrf(csrf -> csrf.disable())

                // ── Oturum yönetimi: Stateless ───────────────────────
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // ── Yetkilendirme Kuralları ──────────────────────────
                .authorizeHttpRequests(auth -> auth
                        // Public endpoint'ler – kimlik doğrulama gerektirmez
                        .requestMatchers(HttpMethod.GET, "/api/market/currencies/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/news/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/historical/**").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()

                        // Korumalı endpoint'ler
                        .requestMatchers(HttpMethod.POST, "/api/market/currencies/refresh").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/news/refresh").hasRole("ADMIN")
                        .requestMatchers("/api/portfolios/**").authenticated()
                        .requestMatchers("/api/users/**").authenticated()

                        // Diğer tüm istekler kimlik doğrulama gerektirir
                        .anyRequest().authenticated())

                // ── OAuth2 Resource Server (JWT) ─────────────────────
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthConverter)));

        return http.build();
    }

    /**
     * CORS yapılandırması – Frontend (localhost:3000) ile uyumlu.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.asList(allowedOrigins.split(",")));
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Authorization", "Content-Type"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
