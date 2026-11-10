package com.procurement.monitor.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public class DatabaseJwtAuthenticationConverter
    implements Converter<Jwt, AbstractAuthenticationToken> {

    private final AccountSecurityService accounts;

    public DatabaseJwtAuthenticationConverter(
        AccountSecurityService accounts
    ) {
        this.accounts = accounts;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Long userId;

        try {
            userId = Long.valueOf(jwt.getSubject());
        } catch (NumberFormatException exception) {
            throw new InvalidBearerTokenException(
                "Invalid token subject"
            );
        }

        var authorities = accounts.authenticate(
            userId,
            jwt.getClaimAsString("credentialVersion")
        ).stream()
            .map(role ->
                new SimpleGrantedAuthority("ROLE_" + role))
            .toList();

        return new JwtAuthenticationToken(
            jwt,
            authorities,
            userId.toString()
        );
    }
}
