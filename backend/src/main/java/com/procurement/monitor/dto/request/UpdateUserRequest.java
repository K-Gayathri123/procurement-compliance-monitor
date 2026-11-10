package com.procurement.monitor.dto.request;

import jakarta.validation.constraints.*;

public record UpdateUserRequest(
    @NotBlank @Size(max = 150) String fullName,
    @NotBlank @Email @Size(max = 200) String email,
    @NotNull Boolean active
) {}
