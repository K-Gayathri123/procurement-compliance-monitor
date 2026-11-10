package com.procurement.monitor.service;

import com.procurement.monitor.entity.*;
import com.procurement.monitor.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ProcurementAnalyticsService {

    private final PurchaseOrderRepository orders;
    private final PurchaseOrderLineRepository lines;
    private final ComplianceEvaluationRepository evaluations;
    private final ProcurementExceptionRepository exceptions;
    private final RecoveryRecordRepository recoveries;

    public ProcurementAnalyticsService(
        PurchaseOrderRepository orders,
        PurchaseOrderLineRepository lines,
        ComplianceEvaluationRepository evaluations,
        ProcurementExceptionRepository exceptions,
        RecoveryRecordRepository recoveries
    ) {
        this.orders = orders;
        this.lines = lines;
        this.evaluations = evaluations;
        this.exceptions = exceptions;
        this.recoveries = recoveries;
    }

    public record Summary(
        long orderCount,
        long lineCount,
        long evaluatedLineCount,
        long compliantLineCount,
        long exceptionCount,
        long possibleSplitGroupCount,
        Map<String, Long> exceptionsByReviewStatus,
        Map<String, String> spendScaledByCurrency,
        Map<String, String> potentialLeakageScaledByCurrency,
        Map<String, String> recoverableEstimateScaledByCurrency,
        Map<String, String> actualRecoveryScaledByCurrency,
        int moneyScale,
        String interpretation
    ) {}

    @Transactional(readOnly = true)
    public Summary summary() {
        List<PurchaseOrder> allOrders =
            orders.findAll();

        List<PurchaseOrderLine> allLines =
            lines.findAll();

        List<ProcurementException> allExceptions =
            exceptions.findAll();

        Map<Long, PurchaseOrder> orderMap =
            allOrders.stream().collect(
                Collectors.toMap(
                    PurchaseOrder::getPoId,
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

        Map<String, BigInteger> spend =
            new TreeMap<>();

        for (PurchaseOrderLine line : allLines) {
            PurchaseOrder order =
                orderMap.get(line.getPoId());

            BigInteger total =
                BigInteger.valueOf(
                    line.getActualUnitPriceScaled()
                ).multiply(
                    BigInteger.valueOf(
                        line.getQuantity()
                    )
                );

            add(
                spend,
                order.getCurrency(),
                total
            );
        }

        Map<String, BigInteger> leakage =
            new TreeMap<>();

        for (ComplianceEvaluation evaluation :
                latest.values()) {
            Long amount =
                evaluation.getPotentialPriceLeakageScaled();

            if (amount != null
                && evaluation.getComparisonCurrency() != null) {
                add(
                    leakage,
                    evaluation.getComparisonCurrency(),
                    BigInteger.valueOf(amount)
                );
            }
        }

        Map<String, BigInteger> estimates =
            new TreeMap<>();

        Map<String, BigInteger> actual =
            new TreeMap<>();

        for (RecoveryRecord record :
                recoveries.findAll()) {
            Map<String, BigInteger> target =
                "ACTUAL_RECOVERY".equals(
                    record.getRecordType()
                ) ? actual : estimates;

            add(
                target,
                record.getCurrency(),
                BigInteger.valueOf(
                    record.getAmountScaled()
                )
            );
        }

        Map<String, Long> byStatus =
            allExceptions.stream().collect(
                Collectors.groupingBy(
                    ProcurementException::getReviewStatus,
                    TreeMap::new,
                    Collectors.counting()
                )
            );

        long splitGroups =
            allExceptions.stream()
                .filter(exception ->
                    "POSSIBLE_SPLIT_PURCHASE".equals(
                        exception.getExceptionType()
                    )
                )
                .map(
                    ProcurementException::getRelatedGroupKey
                )
                .filter(Objects::nonNull)
                .distinct()
                .count();

        long compliant =
            latest.values().stream()
                .filter(evaluation ->
                    "COMPLIANT".equals(
                        evaluation.getResultStatus()
                    )
                )
                .count();

        return new Summary(
            allOrders.size(),
            allLines.size(),
            latest.size(),
            compliant,
            allExceptions.size(),
            splitGroups,
            byStatus,
            strings(spend),
            strings(leakage),
            strings(estimates),
            strings(actual),
            10000,
            "Compliance counts describe latest line evaluations. "
                + "Split flags and review outcomes are separate. "
                + "Potential leakage is not confirmed or recovered savings. "
                + "Estimates and actual recoveries must not be added together. "
                + "Money totals must not be combined across currencies."
        );
    }

    private void add(
        Map<String, BigInteger> target,
        String currency,
        BigInteger amount
    ) {
        target.merge(
            currency, amount, BigInteger::add
        );
    }

    private Map<String, String> strings(
        Map<String, BigInteger> source
    ) {
        Map<String, String> result =
            new TreeMap<>();

        source.forEach(
            (currency, amount) ->
                result.put(
                    currency,
                    amount.toString()
                )
        );

        return result;
    }
}
