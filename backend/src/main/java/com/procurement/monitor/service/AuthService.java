package com.procurement.monitor.service;

import com.procurement.monitor.dto.request.LoginRequest;
import com.procurement.monitor.dto.response.TokenResponse;
import com.procurement.monitor.exception.ApiException;
import com.procurement.monitor.repository.UserAccountRepository;
import com.procurement.monitor.security.CredentialVersion;
import com.procurement.monitor.validation.PasswordRules;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Service
public class AuthService {

    private static final Logger log =
        LoggerFactory.getLogger(AuthService.class);

    private final UserAccountRepository users;
    private final PasswordEncoder passwords;
    private final JwtEncoder encoder;
    private final String issuer;
    private final long ttl;
    private final String dummyHash;

    public AuthService(
        UserAccountRepository users,
        PasswordEncoder passwords,
        JwtEncoder encoder,
        @Value("${app.jwt.issuer}") String issuer,
        @Value("${app.jwt.ttl-seconds}") long ttl
    ) {
        if (ttl <= 0 || ttl > 86400000) {
            throw new IllegalStateException(
                "JWT TTL must be between 1 and 86400 seconds"
            );
        }

        this.users = users;
        this.passwords = passwords;
        this.encoder = encoder;
        this.issuer = issuer;
        this.ttl = ttl;

        this.dummyHash = passwords.encode(
            UUID.randomUUID().toString()
        );
    }

    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest request) {
        String email = request.email()
            .trim()
            .toLowerCase(Locale.ROOT);

        var user = users.findByEmail(email).orElse(null);

        String hash = user != null
            && user.getPasswordHash() != null
            ? user.getPasswordHash()
            : dummyHash;

        boolean matches =
            PasswordRules.supportedLoginInput(request.password())
                && passwords.matches(request.password(), hash);

        if (user == null
            || !matches
            || user.getPasswordHash() == null
            || !Boolean.TRUE.equals(user.getActive())
            || user.getDeletedAt() != null) {
            throw new ApiException(
                HttpStatus.UNAUTHORIZED,
                "Invalid credentials"
            );
        }

        Instant now = Instant.now();

        JwtClaimsSet claims = JwtClaimsSet.builder()
            .issuer(issuer)
            .subject(user.getUserId().toString())
            .issuedAt(now)
            .expiresAt(now.plusSeconds(ttl))
            .claim(
                "credentialVersion",
                CredentialVersion.fromPasswordHash(
                    user.getPasswordHash()
                )
            )
            .build();

        String token = encoder.encode(
            JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(),
                claims
            )
        ).getTokenValue();

        log.info(
            "Login succeeded; userId={}",
            user.getUserId()
        );

        return new TokenResponse(
            token, "Bearer", ttl
        );
    }
}
