package com.procurement.monitor.mapper;

import com.procurement.monitor.entity.*;
import com.procurement.monitor.dto.response.*;

public final class ResponseMapper {

    private ResponseMapper() {}

    public static UserAccountResponse toResponse(
        UserAccount entity
    ) {
        return new UserAccountResponse(
            entity.getUserId(),
            entity.getFullName(),
            entity.getEmail(),
            entity.getActive(),
            entity.getCreatedAt(),
            entity.getDeletedAt(),
            entity.getDeletedByUserId(),
            entity.getDeletionReason()
        );
    }


    public static RoleResponse toResponse(
        Role entity
    ) {
        return new RoleResponse(
            entity.getRoleId(),
            entity.getRoleName()
        );
    }


    public static UserRoleResponse toResponse(
        UserRole entity
    ) {
        return new UserRoleResponse(
            entity.getUserId(),
            entity.getRoleId()
        );
    }


    public static SupplierResponse toResponse(
        Supplier entity
    ) {
        return new SupplierResponse(
            entity.getSupplierId(),
            entity.getCanonicalName(),
            entity.getActive(),
            entity.getDeletedAt(),
            entity.getDeletedByUserId(),
            entity.getDeletionReason()
        );
    }


    public static SupplierAliasResponse toResponse(
        SupplierAlias entity
    ) {
        return new SupplierAliasResponse(
            entity.getAliasId(),
            entity.getSupplierId(),
            entity.getAliasName(),
            entity.getNormalizedAlias(),
            entity.getDeletedAt(),
            entity.getDeletedByUserId(),
            entity.getDeletionReason()
        );
    }


    public static ContractResponse toResponse(
        Contract entity
    ) {
        return new ContractResponse(
            entity.getContractId(),
            entity.getSupplierId(),
            entity.getContractNumber(),
            entity.getItemCode(),
            entity.getEffectiveFrom(),
            entity.getEffectiveTo(),
            entity.getCurrency(),
            entity.getStatus()
        );
    }


    public static ContractPriceTierResponse toResponse(
        ContractPriceTier entity
    ) {
        return new ContractPriceTierResponse(
            entity.getTierId(),
            entity.getContractId(),
            entity.getMinQuantity(),
            entity.getMaxQuantity(),
            entity.getReferenceUnitPriceScaled(),
            entity.getAllowedMinPriceScaled(),
            entity.getAllowedMaxPriceScaled()
        );
    }


    public static UploadBatchResponse toResponse(
        UploadBatch entity
    ) {
        return new UploadBatchResponse(
            entity.getUploadId(),
            entity.getUploadedByUserId(),
            entity.getFileName(),
            entity.getFileHash(),
            entity.getS3ObjectKey(),
            entity.getUploadedAt(),
            entity.getProcessingStatus()
        );
    }


    public static PurchaseOrderResponse toResponse(
        PurchaseOrder entity
    ) {
        return new PurchaseOrderResponse(
            entity.getPoId(),
            entity.getUploadId(),
            entity.getSupplierId(),
            entity.getBuyerUserId(),
            entity.getSourcePoNumber(),
            entity.getOriginalSupplierName(),
            entity.getDepartment(),
            entity.getOrderDate(),
            entity.getCurrency()
        );
    }


    public static PurchaseOrderLineResponse toResponse(
        PurchaseOrderLine entity
    ) {
        return new PurchaseOrderLineResponse(
            entity.getPoLineId(),
            entity.getPoId(),
            entity.getLineNumber(),
            entity.getItemCode(),
            entity.getQuantity(),
            entity.getActualUnitPriceScaled()
        );
    }


    public static ExchangeRateResponse toResponse(
        ExchangeRate entity
    ) {
        return new ExchangeRateResponse(
            entity.getRateId(),
            entity.getFromCurrency(),
            entity.getToCurrency(),
            entity.getRateDate(),
            entity.getConversionRate(),
            entity.getRateSource()
        );
    }


    public static ComplianceEvaluationResponse toResponse(
        ComplianceEvaluation entity
    ) {
        return new ComplianceEvaluationResponse(
            entity.getEvaluationId(),
            entity.getPoLineId(),
            entity.getContractId(),
            entity.getTierId(),
            entity.getRateId(),
            entity.getEvaluatedAt(),
            entity.getRuleVersion(),
            entity.getResultStatus(),
            entity.getComparisonCurrency(),
            entity.getConvertedUnitPriceScaled(),
            entity.getPotentialPriceLeakageScaled(),
            entity.getMatchingSnapshot(),
            entity.getExplanation()
        );
    }


    public static ProcurementExceptionResponse toResponse(
        ProcurementException entity
    ) {
        return new ProcurementExceptionResponse(
            entity.getExceptionId(),
            entity.getEvaluationId(),
            entity.getExceptionType(),
            entity.getRelatedGroupKey(),
            entity.getReviewStatus(),
            entity.getReason(),
            entity.getCreatedAt()
        );
    }


    public static ReviewEventResponse toResponse(
        ReviewEvent entity
    ) {
        return new ReviewEventResponse(
            entity.getReviewEventId(),
            entity.getExceptionId(),
            entity.getActorUserId(),
            entity.getActorRoleAtAction(),
            entity.getAction(),
            entity.getComment(),
            entity.getCreatedAt()
        );
    }


    public static RecoveryRecordResponse toResponse(
        RecoveryRecord entity
    ) {
        return new RecoveryRecordResponse(
            entity.getRecoveryId(),
            entity.getExceptionId(),
            entity.getRecordedByUserId(),
            entity.getRecordType(),
            entity.getAmountScaled(),
            entity.getCurrency(),
            entity.getEvidenceReference(),
            entity.getRecordedAt()
        );
    }

}
