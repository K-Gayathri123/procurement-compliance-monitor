package com.procurement.monitor.controller;

import com.procurement.monitor.service.ProcurementAnalyticsService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/analytics")
@PreAuthorize("hasAnyRole('ADMIN','ANALYST')")
public class ProcurementAnalyticsController {

    private final ProcurementAnalyticsService service;

    public ProcurementAnalyticsController(
        ProcurementAnalyticsService service
    ) {
        this.service = service;
    }

    @GetMapping("/summary")
    public ProcurementAnalyticsService.Summary summary() {
        return service.summary();
    }
}
