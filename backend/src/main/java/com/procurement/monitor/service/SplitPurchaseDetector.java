package com.procurement.monitor.service;

import com.procurement.monitor.entity.*;
import com.procurement.monitor.repository.*;
import com.procurement.monitor.validation.PurchaseRules;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class SplitPurchaseDetector {

    private record Key(
        Long supplierId,
        Long buyerId,
        String department,
        String orderDate,
        String itemCode,
        Long contractId,
        String orderCurrency
    ) {}

    private record Member(
        PurchaseOrder order,
        PurchaseOrderLine line,
        ComplianceEvaluation evaluation,
        ContractPriceTier tier
    ) {}

    private final PurchaseOrderRepository orders;
    private final PurchaseOrderLineRepository lines;
    private final ComplianceEvaluationRepository evaluations;
    private final ContractPriceTierRepository tiers;
    private final ProcurementExceptionRepository exceptions;
    private final ExceptionWriter writer;
    private final ObjectMapper json;

    public SplitPurchaseDetector(
        PurchaseOrderRepository orders,
        PurchaseOrderLineRepository lines,
        ComplianceEvaluationRepository evaluations,
        ContractPriceTierRepository tiers,
        ProcurementExceptionRepository exceptions,
        ExceptionWriter writer,
        ObjectMapper json
    ) {
        this.orders = orders;
        this.lines = lines;
        this.evaluations = evaluations;
        this.tiers = tiers;
        this.exceptions = exceptions;
        this.writer = writer;
        this.json = json;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void detect(
        Set<Long> newLineIds,
        Long actorId,
        String actorRole
    ) {
        Map<Long, PurchaseOrder> orderMap =
            orders.findAll().stream().collect(
                Collectors.toMap(
                    PurchaseOrder::getPoId,
                    Function.identity()
                )
            );

        Map<Long, ContractPriceTier> tierMap =
            tiers.findAll().stream().collect(
                Collectors.toMap(
                    ContractPriceTier::getTierId,
                    Function.identity()
                )
            );

        Map<Long, ComplianceEvaluation> latest =
            new HashMap<>();

        for (ComplianceEvaluation evaluation :
                evaluations.findAll()) {
            latest.merge(
                evaluation.getPoLineId(),
                evaluation,
                (left, right) ->
                    left.getEvaluationId()
                        > right.getEvaluationId()
                        ? left : right
            );
        }

        Map<Key, List<Member>> groups =
            new LinkedHashMap<>();

        for (PurchaseOrderLine line : lines.findAll()) {
            PurchaseOrder order = orderMap.get(
                line.getPoId()
            );

            ComplianceEvaluation evaluation =
                latest.get(line.getPoLineId());

            if (order == null
                || order.getSupplierId() == null
                || evaluation == null
                || evaluation.getContractId() == null
                || evaluation.getTierId() == null) {
                continue;
            }

            ContractPriceTier tier = tierMap.get(
                evaluation.getTierId()
            );

            if (tier == null) {
                continue;
            }

            Key key = new Key(
                order.getSupplierId(),
                order.getBuyerUserId(),
                order.getDepartment(),
                order.getOrderDate(),
                line.getItemCode(),
                evaluation.getContractId(),
                order.getCurrency()
            );

            groups.computeIfAbsent(
                key, ignored -> new ArrayList<>()
            ).add(
                new Member(
                    order, line, evaluation, tier
                )
            );
        }

        for (var entry : groups.entrySet()) {
            Key key = entry.getKey();
            List<Member> members = entry.getValue();

            boolean touched = members.stream()
                .anyMatch(member ->
                    newLineIds.contains(
                        member.line().getPoLineId()
                    )
                );

            if (!touched) {
                continue;
            }

            long distinctOrders = members.stream()
                .map(member ->
                    member.order().getPoId()
                )
                .distinct()
                .count();

            if (distinctOrders < 2) {
                continue;
            }

            long totalQuantity = 0L;

            for (Member member : members) {
                totalQuantity = Math.addExact(
                    totalQuantity,
                    member.line().getQuantity()
                );
            }

            final long groupQuantity = totalQuantity;

            var combinedTiers = tiers.findByContractId(
                key.contractId()
            ).stream()
                .filter(tier ->
                    tier.getMinQuantity() <= groupQuantity
                        && (
                            tier.getMaxQuantity() == null
                            || tier.getMaxQuantity()
                                >= groupQuantity
                        )
                )
                .toList();

            if (combinedTiers.size() != 1) {
                continue;
            }

            ContractPriceTier combined =
                combinedTiers.getFirst();

            boolean discountOpportunity =
                members.stream().anyMatch(member ->
                    combined.getReferenceUnitPriceScaled()
                        < member.tier()
                            .getReferenceUnitPriceScaled()
                );

            if (!discountOpportunity) {
                continue;
            }

            String keyJson =
                json.writeValueAsString(key);

            String groupKey =
                "SPLIT_V1:"
                    + PurchaseRules.sha256(keyJson);

            Set<Long> alreadyFlaggedEvaluations =
                exceptions.findByRelatedGroupKey(groupKey)
                    .stream()
                    .filter(exception ->
                        "POSSIBLE_SPLIT_PURCHASE".equals(
                            exception.getExceptionType()
                        )
                    )
                    .map(
                        ProcurementException::getEvaluationId
                    )
                    .collect(Collectors.toSet());

            Map<String, Object> evidence =
                new LinkedHashMap<>();

            evidence.put(
                "policy", "SAME_DAY_QUANTITY_DISCOUNT_V1"
            );
            evidence.put(
                "warning",
                "Review signal only; not proof of intentional splitting"
            );
            evidence.put(
                "combinedQuantity", groupQuantity
            );
            evidence.put(
                "combinedTierId", combined.getTierId()
            );
            evidence.put(
                "combinedReferenceUnitPriceScaled",
                combined.getReferenceUnitPriceScaled()
            );
            evidence.put(
                "poLineIds",
                members.stream()
                    .map(member ->
                        member.line().getPoLineId()
                    )
                    .sorted()
                    .toList()
            );

            String reason =
                json.writeValueAsString(evidence);

            for (Member member : members) {
                if (alreadyFlaggedEvaluations.contains(
                        member.evaluation()
                            .getEvaluationId())) {
                    continue;
                }

                writer.create(
                    member.evaluation(),
                    "POSSIBLE_SPLIT_PURCHASE",
                    groupKey,
                    reason,
                    actorId,
                    actorRole
                );
            }
        }
    }
}
