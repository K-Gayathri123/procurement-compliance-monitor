package com.procurement.monitor.service;

import com.procurement.monitor.dto.response.*;
import com.procurement.monitor.entity.*;
import com.procurement.monitor.mapper.ResponseMapper;
import com.procurement.monitor.repository.*;
import com.procurement.monitor.security.OperationalAccess;
import com.procurement.monitor.validation.Checks;
import com.procurement.monitor.validation.MasterDataRules;
import com.procurement.monitor.validation.PurchaseRules;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

@Service
public class PurchaseIngestionService {

    private static final Logger log =
        LoggerFactory.getLogger(PurchaseIngestionService.class);

    private final PurchaseCsvParser parser;
    private final UploadBatchRepository uploads;
    private final PurchaseOrderRepository orders;
    private final PurchaseOrderLineRepository lines;
    private final UserAccountRepository users;
    private final SupplierAliasRepository aliases;
    private final SupplierRepository suppliers;
    private final ProcurementExceptionRepository exceptions;
    private final OperationalAccess access;
    private final LocalOriginalStorage storage;
    private final ComplianceEngine compliance;
    private final SplitPurchaseDetector splitDetector;

    public PurchaseIngestionService(
        PurchaseCsvParser parser,
        UploadBatchRepository uploads,
        PurchaseOrderRepository orders,
        PurchaseOrderLineRepository lines,
        UserAccountRepository users,
        SupplierAliasRepository aliases,
        SupplierRepository suppliers,
        ProcurementExceptionRepository exceptions,
        OperationalAccess access,
        LocalOriginalStorage storage,
        ComplianceEngine compliance,
        SplitPurchaseDetector splitDetector
    ) {
        this.parser = parser;
        this.uploads = uploads;
        this.orders = orders;
        this.lines = lines;
        this.users = users;
        this.aliases = aliases;
        this.suppliers = suppliers;
        this.exceptions = exceptions;
        this.access = access;
        this.storage = storage;
        this.compliance = compliance;
        this.splitDetector = splitDetector;
    }

    @Transactional
    public UploadProcessingResponse ingest(
        MultipartFile file,
        Long actorId
    ) {
        String actorRole =
            access.importRole(actorId);

        Checks.require(
            file != null
                && !file.isEmpty()
                && file.getSize() <= 2L * 1024 * 1024,
            "Upload must contain a CSV file of at most 2 MiB"
        );

        byte[] bytes;

        try {
            bytes = file.getBytes();
        } catch (IOException exception) {
            throw new IllegalStateException(
                "Cannot read uploaded file", exception
            );
        }

        String hash = PurchaseRules.sha256(bytes);

        Checks.conflict(
            uploads.existsByFileHash(hash),
            "This exact file has already been imported"
        );

        List<PurchaseCsvParser.Row> rows =
            parser.parse(bytes);

        Map<String, List<PurchaseCsvParser.Row>> groups =
            new LinkedHashMap<>();

        for (PurchaseCsvParser.Row row : rows) {
            groups.computeIfAbsent(
                row.poNumber(),
                ignored -> new ArrayList<>()
            ).add(row);
        }

        // Validate all PO uniqueness and buyer references before writes.
        Map<String, UserAccount> buyerByEmail =
            new HashMap<>();

        for (var entry : groups.entrySet()) {
            Checks.conflict(
                orders.existsBySourcePoNumber(
                    entry.getKey()
                ),
                "An uploaded purchase-order number already exists"
            );

            String email =
                entry.getValue().getFirst().buyerEmail();

            if (!buyerByEmail.containsKey(email)) {
                UserAccount buyer = Checks.found(
                    users.findByEmail(email),
                    "CSV buyer_email does not match an existing user"
                );

                Checks.require(
                    buyer.getDeletedAt() == null,
                    "CSV buyer account is soft-deleted"
                );

                Checks.require(
                    access.has(
                        buyer.getUserId(), "BUYER"
                    ),
                    "CSV buyer account must have the BUYER role"
                );

                // Inactive seeded buyers may be associated with
                // imported historical orders. This does not activate them.
                buyerByEmail.put(email, buyer);
            }
        }

        String fileName =
            Optional.ofNullable(
                file.getOriginalFilename()
            ).orElse("purchase_orders.csv")
                .replace('\\', '/');

        fileName = fileName.substring(
            fileName.lastIndexOf('/') + 1
        );

        fileName = PurchaseRules.text(
            fileName, 200, "file name"
        );

        UploadBatch upload = new UploadBatch();
        upload.setUploadedByUserId(actorId);
        upload.setFileName(fileName);
        upload.setFileHash(hash);
        upload.setS3ObjectKey(null);
        upload.setProcessingStatus("PROCESSING");

        upload = uploads.saveAndFlush(upload);

        storage.store(hash, bytes);

        long exceptionCountBefore = exceptions.count();

        Set<Long> newLineIds = new HashSet<>();

        for (var entry : groups.entrySet()) {
            PurchaseCsvParser.Row first =
                entry.getValue().getFirst();

            Long supplierId = resolveSupplier(
                first.supplierName()
            );

            PurchaseOrder order = new PurchaseOrder();
            order.setUploadId(upload.getUploadId());
            order.setSupplierId(supplierId);
            order.setBuyerUserId(
                buyerByEmail.get(
                    first.buyerEmail()
                ).getUserId()
            );
            order.setSourcePoNumber(first.poNumber());
            order.setOriginalSupplierName(
                first.supplierName()
            );
            order.setDepartment(first.department());
            order.setOrderDate(first.orderDate());
            order.setCurrency(first.currency());

            order = orders.saveAndFlush(order);

            for (PurchaseCsvParser.Row row :
                    entry.getValue()) {
                PurchaseOrderLine line =
                    new PurchaseOrderLine();

                line.setPoId(order.getPoId());
                line.setLineNumber(row.lineNumber());
                line.setItemCode(row.itemCode());
                line.setQuantity(row.quantity());
                line.setActualUnitPriceScaled(
                    row.unitPriceScaled()
                );

                line = lines.saveAndFlush(line);

                newLineIds.add(
                    line.getPoLineId()
                );

                compliance.evaluate(
                    order,
                    line,
                    actorId,
                    actorRole
                );
            }
        }

        splitDetector.detect(
            newLineIds,
            actorId,
            actorRole
        );

        upload.setProcessingStatus("COMPLETED");
        uploads.saveAndFlush(upload);

        long createdExceptions =
            exceptions.count() - exceptionCountBefore;

        log.info(
            "Purchase import completed; uploadId={}, "
                + "orders={}, lines={}, exceptions={}, actorId={}",
            upload.getUploadId(),
            groups.size(),
            rows.size(),
            createdExceptions,
            actorId
        );

        return new UploadProcessingResponse(
            ResponseMapper.toResponse(upload),
            groups.size(),
            rows.size(),
            rows.size(),
            createdExceptions
        );
    }

    private Long resolveSupplier(String name) {
        var alias = aliases.findByNormalizedAlias(
            MasterDataRules.normalizeAlias(name)
        ).orElse(null);

        if (alias == null
            || alias.getDeletedAt() != null) {
            return null;
        }

        Supplier supplier = suppliers.findById(
            alias.getSupplierId()
        ).orElse(null);

        if (supplier == null
            || supplier.getDeletedAt() != null
            || !Boolean.TRUE.equals(
                supplier.getActive())) {
            return null;
        }

        return supplier.getSupplierId();
    }
}
