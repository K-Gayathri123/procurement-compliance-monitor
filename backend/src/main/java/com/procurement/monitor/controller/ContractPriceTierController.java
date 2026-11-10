package com.procurement.monitor.controller;

import com.procurement.monitor.dto.request.*;
import com.procurement.monitor.dto.response.*;
import com.procurement.monitor.service.ContractPriceTierService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/contract-price-tiers")
@Validated
public class ContractPriceTierController {

    private final ContractPriceTierService service;

    public ContractPriceTierController(ContractPriceTierService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize(
        "hasAnyRole('ADMIN','PROCUREMENT_OPERATOR','BUYER','REVIEWER','ANALYST')"
    )
    public PageResponse<ContractPriceTierResponse> list(
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
        @RequestParam(defaultValue = "tierId") String sort,
        @RequestParam(defaultValue = "ASC") String direction,
        @RequestParam(required = false) @Size(max = 200) String query,
        @RequestParam(defaultValue = "false") boolean includeDeleted,
        @RequestParam(required = false) Boolean active,
        @RequestParam(required = false) @Positive Long contractId
    ) {
        return service.list(
            page, size, sort, direction, query,
            includeDeleted, active, contractId
        );
    }

    @GetMapping("/{id}")
    @PreAuthorize(
        "hasAnyRole('ADMIN','PROCUREMENT_OPERATOR','BUYER','REVIEWER','ANALYST')"
    )
    public ContractPriceTierResponse get(
        @PathVariable @Positive Long id
    ) {
        return service.get(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public ContractPriceTierResponse create(
        @Valid @RequestBody ContractPriceTierRequest request,
        @AuthenticationPrincipal Jwt jwt
    ) {
        return service.create(
            request, Long.valueOf(jwt.getSubject())
        );
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ContractPriceTierResponse update(
        @PathVariable @Positive Long id,
        @Valid @RequestBody ContractPriceTierRequest request,
        @AuthenticationPrincipal Jwt jwt
    ) {
        return service.update(
            id, request, Long.valueOf(jwt.getSubject())
        );
    }




    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUnused(
        @PathVariable @Positive Long id,
        @AuthenticationPrincipal Jwt jwt
    ) {
        service.deleteUnused(
            id, Long.valueOf(jwt.getSubject())
        );
    }

}
