package com.procurement.monitor.controller;

import com.procurement.monitor.dto.response.*;
import com.procurement.monitor.service.OperationalQueryService;
import jakarta.validation.constraints.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/purchase-orders")
@Validated
@PreAuthorize(
    "hasAnyRole('ADMIN','PROCUREMENT_OPERATOR','BUYER','REVIEWER','ANALYST')"
)
public class PurchaseController {

    private final OperationalQueryService service;

    public PurchaseController(
        OperationalQueryService service
    ) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<PurchaseOrderResponse> list(
        @RequestParam(defaultValue = "0")
        @Min(0) int page,

        @RequestParam(defaultValue = "20")
        @Min(1) @Max(100) int size,

        @RequestParam(defaultValue = "poId")
        String sort,

        @RequestParam(defaultValue = "DESC")
        String direction,

        @RequestParam(required = false)
        @Positive Long uploadId,

        @RequestParam(required = false)
        @Positive Long supplierId,

        @RequestParam(required = false)
        @Positive Long buyerUserId,

        @RequestParam(required = false)
        @Size(max = 200) String query,

        @AuthenticationPrincipal Jwt jwt
    ) {
        return service.orders(
            page, size, sort, direction,
            uploadId, supplierId, buyerUserId,
            query,
            Long.valueOf(jwt.getSubject())
        );
    }

    @GetMapping("/{id}")
    public PurchaseOrderDetailResponse get(
        @PathVariable @Positive Long id,
        @AuthenticationPrincipal Jwt jwt
    ) {
        return service.order(
            id,
            Long.valueOf(jwt.getSubject())
        );
    }

    @GetMapping("/lines/{lineId}/evaluations")
    public List<ComplianceEvaluationResponse> evaluations(
        @PathVariable @Positive Long lineId,
        @AuthenticationPrincipal Jwt jwt
    ) {
        return service.evaluations(
            lineId,
            Long.valueOf(jwt.getSubject())
        );
    }
}
