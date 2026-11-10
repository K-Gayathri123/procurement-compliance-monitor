package com.procurement.monitor.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.procurement.monitor.security.DatabaseJwtAuthenticationConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecretKey jwtSigningKey(
        @Value("${app.jwt.secret-base64}") String encoded
    ) {
        byte[] bytes;

        try {
            bytes = Base64.getDecoder().decode(encoded);
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException(
                "JWT_SECRET_BASE64 must contain valid Base64"
            );
        }

        if (bytes.length < 32) {
            throw new IllegalStateException(
                "Set JWT_SECRET_BASE64 to at least "
                    + "32 random bytes encoded as Base64"
            );
        }

        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    @Bean
    public JwtEncoder jwtEncoder(SecretKey key) {
        return new NimbusJwtEncoder(
            new ImmutableSecret<com.nimbusds.jose.proc.SecurityContext>(
                key
            )
        );
    }

    @Bean
    public JwtDecoder jwtDecoder(
        SecretKey key,
        @Value("${app.jwt.issuer}") String issuer
    ) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder
            .withSecretKey(key)
            .macAlgorithm(MacAlgorithm.HS256)
            .build();

        decoder.setJwtValidator(
            JwtValidators.createDefaultWithIssuer(issuer)
        );

        return decoder;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
        HttpSecurity http,
        DatabaseJwtAuthenticationConverter converter
    ) throws Exception {

        http
            // CSRF disabled only for this stateless,
            // Authorization-header bearer-token API.
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session
                .sessionCreationPolicy(
                    SessionCreationPolicy.STATELESS
                ))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/api/auth/login",
                    "/api/health"
                ).permitAll()
                .anyRequest().authenticated())
            .exceptionHandling(errors -> errors
                .authenticationEntryPoint(
                    (request, response, exception) -> {
                        response.setStatus(401);
                        response.setContentType("application/json");
                        response.getWriter().write(
                            "{\"error\":\"Authentication required\"}"
                        );
                    }
                )
                .accessDeniedHandler(
                    (request, response, exception) -> {
                        response.setStatus(403);
                        response.setContentType("application/json");
                        response.getWriter().write(
                            "{\"error\":\"Access denied\"}"
                        );
                    }
                ))
            .oauth2ResourceServer(resource -> resource
                .jwt(jwt -> jwt
                    .jwtAuthenticationConverter(converter))
                .authenticationEntryPoint(
                    (request, response, exception) -> {
                        response.setStatus(401);
                        response.setContentType("application/json");
                        response.getWriter().write(
                            "{\"error\":\"Invalid or missing token\"}"
                        );
                    }
                ));

        return http.build();
    }
}
