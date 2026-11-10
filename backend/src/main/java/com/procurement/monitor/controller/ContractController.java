package com.procurement.monitor.controller;

import com.procurement.monitor.dto.request.*;
import com.procurement.monitor.dto.response.*;
import com.procurement.monitor.service.ContractService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/contracts")
@Validated
public class ContractController {

    private final ContractService service;

    public ContractController(ContractService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize(
        "hasAnyRole('ADMIN','PROCUREMENT_OPERATOR','BUYER','REVIEWER','ANALYST')"
    )
    public PageResponse<ContractResponse> list(
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
        @RequestParam(defaultValue = "contractId") String sort,
        @RequestParam(defaultValue = "ASC") String direction,
        @RequestParam(required = false) @Size(max = 200) String query,
        @RequestParam(defaultValue = "false") boolean includeDeleted,
        @RequestParam(required = false) Boolean active,
        @RequestParam(required = false) @Positive Long supplierId,
        @RequestParam(required = false) @Size(max = 100) String status
    ) {
        return service.list(
            page, size, sort, direction, query,
            includeDeleted, active, supplierId, status
        );
    }

    @GetMapping("/{id}")
    @PreAuthorize(
        "hasAnyRole('ADMIN','PROCUREMENT_OPERATOR','BUYER','REVIEWER','ANALYST')"
    )
    public ContractResponse get(
        @PathVariable @Positive Long id
    ) {
        return service.get(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public ContractResponse create(
        @Valid @RequestBody ContractRequest request,
        @AuthenticationPrincipal Jwt jwt
    ) {
        return service.create(
            request, Long.valueOf(jwt.getSubject())
        );
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ContractResponse update(
        @PathVariable @Positive Long id,
        @Valid @RequestBody ContractRequest request,
        @AuthenticationPrincipal Jwt jwt
    ) {
        return service.update(
            id, request, Long.valueOf(jwt.getSubject())
        );
    }




    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ContractResponse approve(
        @PathVariable @Positive Long id,
        @AuthenticationPrincipal Jwt jwt
    ) {
        return service.approve(
            id, Long.valueOf(jwt.getSubject())
        );
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('ADMIN')")
    public ContractResponse cancel(
        @PathVariable @Positive Long id,
        @Valid @RequestBody ActionRequest request,
        @AuthenticationPrincipal Jwt jwt
    ) {
        return service.cancel(
            id, request, Long.valueOf(jwt.getSubject())
        );
    }

}
