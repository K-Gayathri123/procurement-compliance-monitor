package com.procurement.monitor.service;

import com.procurement.monitor.dto.response.*;
import com.procurement.monitor.entity.*;
import com.procurement.monitor.mapper.ResponseMapper;
import com.procurement.monitor.repository.*;
import com.procurement.monitor.security.OperationalAccess;
import com.procurement.monitor.validation.Checks;
import com.procurement.monitor.validation.PageRules;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Transactional(readOnly = true)
public class OperationalQueryService {

    private final UploadBatchRepository uploads;
    private final PurchaseOrderRepository orders;
    private final PurchaseOrderLineRepository lines;
    private final ComplianceEvaluationRepository evaluations;
    private final ProcurementExceptionRepository exceptions;
    private final OperationalAccess access;

    public OperationalQueryService(
        UploadBatchRepository uploads,
        PurchaseOrderRepository orders,
        PurchaseOrderLineRepository lines,
        ComplianceEvaluationRepository evaluations,
        ProcurementExceptionRepository exceptions,
        OperationalAccess access
    ) {
        this.uploads = uploads;
        this.orders = orders;
        this.lines = lines;
        this.evaluations = evaluations;
        this.exceptions = exceptions;
        this.access = access;
    }

    public PageResponse<UploadBatchResponse> uploads(
        int page, int size
    ) {
        return PageResponse.from(
            uploads.findAll(
                PageRules.pageable(
                    page, size,
                    "uploadId", "DESC",
                    Set.of("uploadId")
                )
            ).map(ResponseMapper::toResponse)
        );
    }

    public UploadBatchResponse upload(Long id) {
        return ResponseMapper.toResponse(
            Checks.found(
                uploads.findById(id),
                "Upload not found"
            )
        );
    }

    public PageResponse<PurchaseOrderResponse> orders(
        int page,
        int size,
        String sort,
        String direction,
        Long uploadId,
        Long supplierId,
        Long buyerUserId,
        String query,
        Long actorId
    ) {
        boolean global = access.globalRead(actorId);

        if (!global) {
            Checks.forbidden(
                buyerUserId != null
                    && !buyerUserId.equals(actorId),
                "Cannot query another buyer's purchases"
            );
        }

        Long buyerFilter =
            global ? buyerUserId : actorId;

        Specification<PurchaseOrder> spec =
            (root, criteria, builder) -> {
                var predicates = new ArrayList<
                    jakarta.persistence.criteria.Predicate>();

                if (uploadId != null) {
                    predicates.add(
                        builder.equal(
                            root.get("uploadId"),
                            uploadId
                        )
                    );
                }

                if (supplierId != null) {
                    predicates.add(
                        builder.equal(
                            root.get("supplierId"),
                            supplierId
                        )
                    );
                }

                if (buyerFilter != null) {
                    predicates.add(
                        builder.equal(
                            root.get("buyerUserId"),
                            buyerFilter
                        )
                    );
                }

                if (query != null && !query.isBlank()) {
                    String pattern =
                        PageRules.searchPattern(query);

                    predicates.add(
                        builder.or(
                            builder.like(
                                builder.lower(
                                    root.<String>get(
                                        "sourcePoNumber"
                                    )
                                ),
                                pattern, '\\'
                            ),
                            builder.like(
                                builder.lower(
                                    root.<String>get(
                                        "originalSupplierName"
                                    )
                                ),
                                pattern, '\\'
                            )
                        )
                    );
                }

                return builder.and(
                    predicates.toArray(
                        jakarta.persistence.criteria.Predicate[]::new
                    )
                );
            };

        return PageResponse.from(
            orders.findAll(
                spec,
                PageRules.pageable(
                    page, size, sort, direction,
                    Set.of(
                        "poId", "sourcePoNumber",
                        "orderDate", "supplierId",
                        "buyerUserId"
                    )
                )
            ).map(ResponseMapper::toResponse)
        );
    }

    public PurchaseOrderDetailResponse order(
        Long id, Long actorId
    ) {
        PurchaseOrder order = Checks.found(
            orders.findById(id),
            "Purchase order not found"
        );

        access.readOrder(order, actorId);

        return new PurchaseOrderDetailResponse(
            ResponseMapper.toResponse(order),
            lines.findByPoId(id).stream()
                .sorted(
                    Comparator.comparing(
                        PurchaseOrderLine::getLineNumber
                    )
                )
                .map(ResponseMapper::toResponse)
                .toList()
        );
    }

    public List<ComplianceEvaluationResponse> evaluations(
        Long lineId, Long actorId
    ) {
        PurchaseOrderLine line = Checks.found(
            lines.findById(lineId),
            "Purchase line not found"
        );

        PurchaseOrder order = Checks.found(
            orders.findById(line.getPoId()),
            "Purchase order not found"
        );

        access.readOrder(order, actorId);

        return evaluations.findByPoLineId(lineId)
            .stream()
            .sorted(
                Comparator.comparing(
                    ComplianceEvaluation::getEvaluationId
                )
            )
            .map(ResponseMapper::toResponse)
            .toList();
    }

    public PageResponse<ProcurementExceptionResponse> exceptions(
        int page,
        int size,
        String status,
        String type,
        Long actorId
    ) {
        boolean global =
            access.globalRead(actorId);

        Specification<ProcurementException> spec =
            (root, criteria, builder) -> {
                var predicates = new ArrayList<
                    jakarta.persistence.criteria.Predicate>();

                if (status != null) {
                    predicates.add(
                        builder.equal(
                            root.get("reviewStatus"),
                            status
                        )
                    );
                }

                if (type != null) {
                    predicates.add(
                        builder.equal(
                            root.get("exceptionType"),
                            type
                        )
                    );
                }

                if (!global) {
                    var evaluationJoin =
                        root.join(
                            "evaluationIdReference"
                        );

                    var lineJoin =
                        evaluationJoin.join(
                            "poLineIdReference"
                        );

                    var orderJoin =
                        lineJoin.join(
                            "poIdReference"
                        );

                    predicates.add(
                        builder.equal(
                            orderJoin.get("buyerUserId"),
                            actorId
                        )
                    );
                }

                return builder.and(
                    predicates.toArray(
                        jakarta.persistence.criteria.Predicate[]::new
                    )
                );
            };

        return PageResponse.from(
            exceptions.findAll(
                spec,
                PageRules.pageable(
                    page, size,
                    "exceptionId", "DESC",
                    Set.of("exceptionId")
                )
            ).map(ResponseMapper::toResponse)
        );
    }

    public ProcurementExceptionResponse exception(
        Long id, Long actorId
    ) {
        ProcurementException exception =
            Checks.found(
                exceptions.findById(id),
                "Exception not found"
            );

        access.readException(
            exception, actorId
        );

        return ResponseMapper.toResponse(
            exception
        );
    }
}
