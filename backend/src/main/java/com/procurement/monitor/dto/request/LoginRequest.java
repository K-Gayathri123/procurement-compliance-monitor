package com.procurement.monitor.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
    @NotBlank @Email @Size(max = 200) String email,
    @NotBlank @Size(max = 200) String password
) {}
