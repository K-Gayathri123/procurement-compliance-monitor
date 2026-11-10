package com.procurement.monitor.controller;

import com.procurement.monitor.dto.request.*;
import com.procurement.monitor.dto.response.*;
import com.procurement.monitor.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@PreAuthorize("hasRole('ADMIN')")
@Validated
public class UserController {

    private final UserService service;

    public UserController(UserService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<UserAccountResponse> list(
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
        @RequestParam(defaultValue = "userId") String sort,
        @RequestParam(required = false) @Size(max = 200) String query,
        @RequestParam(defaultValue = "false") boolean includeDeleted
    ) {
        return service.list(
            page, size, sort, query, includeDeleted
        );
    }

    @GetMapping("/{id}")
    public UserAccountResponse get(
        @PathVariable @Positive Long id
    ) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserAccountResponse create(
        @Valid @RequestBody CreateUserRequest request,
        @AuthenticationPrincipal Jwt jwt
    ) {
        return service.create(
            request,
            Long.valueOf(jwt.getSubject())
        );
    }

    @PutMapping("/{id}")
    public UserAccountResponse update(
        @PathVariable @Positive Long id,
        @Valid @RequestBody UpdateUserRequest request,
        @AuthenticationPrincipal Jwt jwt
    ) {
        return service.update(
            id, request,
            Long.valueOf(jwt.getSubject())
        );
    }

    @GetMapping("/{id}/roles")
    public List<RoleResponse> assignedRoles(
        @PathVariable @Positive Long id
    ) {
        return service.assignedRoles(id);
    }

    @PutMapping("/{id}/roles")
    public List<RoleResponse> assignRoles(
        @PathVariable @Positive Long id,
        @Valid @RequestBody RoleAssignmentRequest request,
        @AuthenticationPrincipal Jwt jwt
    ) {
        return service.assignRoles(
            id, request,
            Long.valueOf(jwt.getSubject())
        );
    }

    @PostMapping("/{id}/reset-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(
        @PathVariable @Positive Long id,
        @Valid @RequestBody PasswordResetRequest request,
        @AuthenticationPrincipal Jwt jwt
    ) {
        service.resetPassword(
            id, request,
            Long.valueOf(jwt.getSubject())
        );
    }

    @PostMapping("/{id}/soft-delete")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void softDelete(
        @PathVariable @Positive Long id,
        @Valid @RequestBody ActionRequest request,
        @AuthenticationPrincipal Jwt jwt
    ) {
        service.softDelete(
            id, request,
            Long.valueOf(jwt.getSubject())
        );
    }

    @PostMapping("/{id}/restore")
    public UserAccountResponse restore(
        @PathVariable @Positive Long id,
        @AuthenticationPrincipal Jwt jwt
    ) {
        return service.restore(
            id,
            Long.valueOf(jwt.getSubject())
        );
    }
}
