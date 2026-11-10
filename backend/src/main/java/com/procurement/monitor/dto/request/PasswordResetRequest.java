package com.procurement.monitor.dto.request;

import jakarta.validation.constraints.*;

public record PasswordResetRequest(
    @NotBlank @Size(min = 12, max = 64) String newPassword
) {}
