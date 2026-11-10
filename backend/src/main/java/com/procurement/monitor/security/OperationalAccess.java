package com.procurement.monitor.security;

import com.procurement.monitor.entity.*;
import com.procurement.monitor.repository.*;
import com.procurement.monitor.validation.Checks;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Component
public class OperationalAccess {

    private final AccountSecurityService accounts;
    private final PurchaseOrderRepository orders;
    private final PurchaseOrderLineRepository lines;
    private final ComplianceEvaluationRepository evaluations;

    public OperationalAccess(
        AccountSecurityService accounts,
        PurchaseOrderRepository orders,
        PurchaseOrderLineRepository lines,
        ComplianceEvaluationRepository evaluations
    ) {
        this.accounts = accounts;
        this.orders = orders;
        this.lines = lines;
        this.evaluations = evaluations;
    }

    public List<String> roles(Long actorId) {
        return accounts.roleNames(actorId);
    }

    public boolean has(Long actorId, String role) {
        return roles(actorId).contains(role);
    }

    public boolean globalRead(Long actorId) {
        return roles(actorId).stream().anyMatch(role ->
            List.of(
                "ADMIN",
                "PROCUREMENT_OPERATOR",
                "REVIEWER",
                "ANALYST"
            ).contains(role)
        );
    }

    public String importRole(Long actorId) {
        if (has(actorId, "ADMIN")) {
            return "ADMIN";
        }

        Checks.forbidden(
            !has(actorId, "PROCUREMENT_OPERATOR"),
            "Purchase import requires an operational role"
        );

        return "PROCUREMENT_OPERATOR";
    }

    public String reviewerRole(Long actorId) {
        if (has(actorId, "ADMIN")) {
            return "ADMIN";
        }

        Checks.forbidden(
            !has(actorId, "REVIEWER"),
            "Reviewer role is required"
        );

        return "REVIEWER";
    }

    public PurchaseOrder orderFor(
        ComplianceEvaluation evaluation
    ) {
        var line = Checks.found(
            lines.findById(evaluation.getPoLineId()),
            "Purchase line not found"
        );

        return Checks.found(
            orders.findById(line.getPoId()),
            "Purchase order not found"
        );
    }

    public PurchaseOrder orderFor(
        ProcurementException exception
    ) {
        return orderFor(
            Checks.found(
                evaluations.findById(
                    exception.getEvaluationId()
                ),
                "Evaluation not found"
            )
        );
    }

    public void readOrder(
        PurchaseOrder order, Long actorId
    ) {
        Checks.forbidden(
            !globalRead(actorId)
                && !Objects.equals(
                    order.getBuyerUserId(), actorId
                ),
            "This purchase belongs to another buyer"
        );
    }

    public void readException(
        ProcurementException exception, Long actorId
    ) {
        readOrder(orderFor(exception), actorId);
    }
}
