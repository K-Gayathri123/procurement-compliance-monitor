package com.procurement.monitor.validation;

import java.nio.charset.StandardCharsets;

public final class PasswordRules {

    private PasswordRules() {}

    public static void validateNewPassword(String password) {
        Checks.require(
            password != null
                && password.length() >= 12
                && password.length() <= 64
                && !password.isBlank(),
            "New password must contain 12–64 characters"
        );

        // Also enforce a byte limit for BCrypt.
        Checks.require(
            password.getBytes(StandardCharsets.UTF_8).length <= 72,
            "Password must not exceed 72 UTF-8 bytes"
        );
    }

    public static boolean supportedLoginInput(String password) {
        return password != null
            && password.getBytes(StandardCharsets.UTF_8).length <= 72;
    }
}
