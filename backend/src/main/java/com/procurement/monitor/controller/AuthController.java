package com.procurement.monitor.controller;

import com.procurement.monitor.dto.request.ChangePasswordRequest;
import com.procurement.monitor.dto.request.LoginRequest;
import com.procurement.monitor.dto.response.CurrentUserResponse;
import com.procurement.monitor.dto.response.TokenResponse;
import com.procurement.monitor.security.AccountSecurityService;
import com.procurement.monitor.service.AuthService;
import com.procurement.monitor.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService auth;
    private final AccountSecurityService accounts;
    private final UserService users;

    public AuthController(
        AuthService auth,
        AccountSecurityService accounts,
        UserService users
    ) {
        this.auth = auth;
        this.accounts = accounts;
        this.users = users;
    }

    @PostMapping("/login")
    public TokenResponse login(
        @Valid @RequestBody LoginRequest request
    ) {
        return auth.login(request);
    }

    @GetMapping("/me")
    public CurrentUserResponse current(
        @AuthenticationPrincipal Jwt jwt
    ) {
        return accounts.current(
            Long.valueOf(jwt.getSubject())
        );
    }

    @PostMapping("/change-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(
        @AuthenticationPrincipal Jwt jwt,
        @Valid @RequestBody ChangePasswordRequest request
    ) {
        users.changeOwnPassword(
            Long.valueOf(jwt.getSubject()),
            request
        );
    }
}
