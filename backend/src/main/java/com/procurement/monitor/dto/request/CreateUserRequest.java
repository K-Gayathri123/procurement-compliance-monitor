package com.procurement.monitor.dto.request;

import jakarta.validation.constraints.*;
import java.util.Set;

public record CreateUserRequest(
    @NotBlank @Size(max = 150) String fullName,
    @NotBlank @Email @Size(max = 200) String email,
    @NotBlank @Size(min = 12, max = 64) String password,
    @NotEmpty @Size(max = 10)
    Set<@NotNull @Positive Long> roleIds
) {}
