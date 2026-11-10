package com.procurement.monitor.controller;

import com.procurement.monitor.dto.response.*;
import com.procurement.monitor.service.*;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/uploads")
@Validated
@PreAuthorize("hasAnyRole('ADMIN','PROCUREMENT_OPERATOR')")
public class UploadController {

    private final PurchaseIngestionService ingestion;
    private final OperationalQueryService queries;

    public UploadController(
        PurchaseIngestionService ingestion,
        OperationalQueryService queries
    ) {
        this.ingestion = ingestion;
        this.queries = queries;
    }

    @PostMapping(
        consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @ResponseStatus(HttpStatus.CREATED)
    public UploadProcessingResponse upload(
        @RequestPart("file") MultipartFile file,
        @AuthenticationPrincipal Jwt jwt
    ) {
        return ingestion.ingest(
            file,
            Long.valueOf(jwt.getSubject())
        );
    }

    @GetMapping
    public PageResponse<UploadBatchResponse> list(
        @RequestParam(defaultValue = "0")
        @Min(0) int page,

        @RequestParam(defaultValue = "20")
        @Min(1) @Max(100) int size
    ) {
        return queries.uploads(page, size);
    }

    @GetMapping("/{id}")
    public UploadBatchResponse get(
        @PathVariable @Positive Long id
    ) {
        return queries.upload(id);
    }
}
