package com.seopulse.common.config;

import com.seopulse.common.security.JwtAuthenticationConverter;
import com.seopulse.common.security.RestAccessDeniedHandler;
import com.seopulse.common.security.RestAuthenticationEntryPoint;
import jakarta.servlet.DispatcherType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.List;

@EnableWebSecurity
@Configuration
public class SecurityConfig {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${seopulse.cors.allowed-origins:}")
    private List<String> allowedOrigins;

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        http
                // Stateless bearer tokens; the refresh cookie endpoints are
                // protected by SameSite=Strict plus a required custom header.
                .csrf(csrf -> csrf.disable())

                .cors(cors ->
                        cors.configurationSource(corsConfigurationSource())
                )

                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                .exceptionHandling(exception ->
                        exception
                                .authenticationEntryPoint(
                                        new RestAuthenticationEntryPoint()
                                )
                                .accessDeniedHandler(
                                        new RestAccessDeniedHandler()
                                )
                )

                .authorizeHttpRequests(auth ->
                        auth
                                // SSE responses complete on an async dispatch;
                                // the original request was already authorized.
                                .dispatcherTypeMatchers(DispatcherType.ASYNC, DispatcherType.ERROR)
                                .permitAll()

                                // Allow browser CORS preflight requests
                                .requestMatchers(HttpMethod.OPTIONS, "/**")
                                .permitAll()

                                // Auth endpoints that act on the signed-in user
                                .requestMatchers(
                                        "/api/v1/auth/logout-all",
                                        "/api/v1/auth/resend-verification"
                                )
                                .authenticated()

                                // Public authentication endpoints
                                .requestMatchers("/api/v1/auth/**")
                                .permitAll()

                                // Health checks and metrics scraping. In prod the
                                // actuator runs on an internal-only port.
                                .requestMatchers(
                                        "/api/v1/health",
                                        "/actuator/health",
                                        "/actuator/health/**",
                                        "/actuator/info",
                                        "/actuator/prometheus"
                                )
                                .permitAll()

                                // Swagger
                                .requestMatchers(
                                        "/swagger-ui.html",
                                        "/swagger-ui/**",
                                        "/v3/api-docs/**"
                                )
                                .permitAll()

                                // Everything else requires JWT
                                .anyRequest()
                                .authenticated()
                )

                .oauth2ResourceServer(oauth2 ->
                        oauth2
                                .authenticationEntryPoint(new RestAuthenticationEntryPoint())
                                .jwt(jwt ->
                                        jwt.jwtAuthenticationConverter(
                                                new JwtAuthenticationConverter()
                                        )
                                )
                );

        return http.build();
    }

    @Bean
    public JwtDecoder jwtDecoder() {

        SecretKey key = new SecretKeySpec(
                jwtSecret.getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
        );

        NimbusJwtDecoder decoder = NimbusJwtDecoder
                .withSecretKey(key)
                .build();

        decoder.setJwtValidator(
                new DelegatingOAuth2TokenValidator<>(
                        JwtValidators.createDefault(),
                        numericSubjectValidator()
                )
        );

        return decoder;
    }

    /**
     * Tokens issued before the subject switched from email to user ID
     * are rejected, which forces a fresh sign-in.
     */
    private OAuth2TokenValidator<Jwt> numericSubjectValidator() {

        OAuth2Error invalidSubject = new OAuth2Error(
                OAuth2ErrorCodes.INVALID_TOKEN,
                "Token subject must be a user ID",
                null
        );

        return jwt -> {
            String subject = jwt.getSubject();

            if (subject != null
                    && !subject.isEmpty()
                    && subject.chars().allMatch(Character::isDigit)) {
                return OAuth2TokenValidatorResult.success();
            }

            return OAuth2TokenValidatorResult.failure(invalidSubject);
        };
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(
                allowedOrigins.stream()
                        .map(String::trim)
                        .filter(origin -> !origin.isEmpty())
                        .toList()
        );
        configuration.setAllowedMethods(
                List.of(
                        "GET",
                        "POST",
                        "PUT",
                        "PATCH",
                        "DELETE",
                        "OPTIONS"
                )
        );

        configuration.setAllowedHeaders(
                List.of("*")
        );

        configuration.setExposedHeaders(
                List.of(
                        "X-Request-Id",
                        "Retry-After",
                        "X-RateLimit-Limit",
                        "X-RateLimit-Remaining"
                )
        );

        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration(
                "/**",
                configuration
        );

        return source;
    }
}
