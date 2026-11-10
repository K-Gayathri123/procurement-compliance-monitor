package com.procurement.monitor.controller;

import com.procurement.monitor.dto.request.*;
import com.procurement.monitor.dto.response.*;
import com.procurement.monitor.service.SupplierService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/suppliers")
@Validated
public class SupplierController {

    private final SupplierService service;

    public SupplierController(SupplierService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize(
        "hasAnyRole('ADMIN','PROCUREMENT_OPERATOR','BUYER','REVIEWER','ANALYST')"
    )
    public PageResponse<SupplierResponse> list(
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
        @RequestParam(defaultValue = "supplierId") String sort,
        @RequestParam(defaultValue = "ASC") String direction,
        @RequestParam(required = false) @Size(max = 200) String query,
        @RequestParam(defaultValue = "false") boolean includeDeleted,
        @RequestParam(required = false) Boolean active
    ) {
        return service.list(
            page, size, sort, direction, query,
            includeDeleted, active
        );
    }

    @GetMapping("/{id}")
    @PreAuthorize(
        "hasAnyRole('ADMIN','PROCUREMENT_OPERATOR','BUYER','REVIEWER','ANALYST')"
    )
    public SupplierResponse get(
        @PathVariable @Positive Long id
    ) {
        return service.get(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public SupplierResponse create(
        @Valid @RequestBody SupplierRequest request,
        @AuthenticationPrincipal Jwt jwt
    ) {
        return service.create(
            request, Long.valueOf(jwt.getSubject())
        );
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public SupplierResponse update(
        @PathVariable @Positive Long id,
        @Valid @RequestBody SupplierRequest request,
        @AuthenticationPrincipal Jwt jwt
    ) {
        return service.update(
            id, request, Long.valueOf(jwt.getSubject())
        );
    }


    @PostMapping("/{id}/soft-delete")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void softDelete(
        @PathVariable @Positive Long id,
        @Valid @RequestBody ActionRequest request,
        @AuthenticationPrincipal Jwt jwt
    ) {
        service.softDelete(
            id, request, Long.valueOf(jwt.getSubject())
        );
    }

    @PostMapping("/{id}/restore")
    @PreAuthorize("hasRole('ADMIN')")
    public SupplierResponse restore(
        @PathVariable @Positive Long id,
        @AuthenticationPrincipal Jwt jwt
    ) {
        return service.restore(
            id, Long.valueOf(jwt.getSubject())
        );
    }



}
