package com.procurement.monitor.dto.request;

import jakarta.validation.constraints.*;

public record ChangePasswordRequest(
    @NotBlank @Size(max = 200) String currentPassword,
    @NotBlank @Size(min = 12, max = 64) String newPassword
) {}
