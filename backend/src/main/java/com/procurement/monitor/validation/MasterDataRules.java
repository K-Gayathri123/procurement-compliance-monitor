package com.procurement.monitor.validation;

import com.procurement.monitor.entity.*;
import com.procurement.monitor.repository.*;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.Locale;
import java.util.Objects;

@Component
public class MasterDataRules {

    private final SupplierRepository suppliers;
    private final SupplierAliasRepository aliases;
    private final ContractRepository contracts;
    private final ContractPriceTierRepository tiers;
    private final ExchangeRateRepository rates;
    private final ComplianceEvaluationRepository evaluations;

    public MasterDataRules(
        SupplierRepository suppliers,
        SupplierAliasRepository aliases,
        ContractRepository contracts,
        ContractPriceTierRepository tiers,
        ExchangeRateRepository rates,
        ComplianceEvaluationRepository evaluations
    ) {
        this.suppliers = suppliers;
        this.aliases = aliases;
        this.contracts = contracts;
        this.tiers = tiers;
        this.rates = rates;
        this.evaluations = evaluations;
    }

    public static String normalizeAlias(String name) {
        return name.trim().toLowerCase(Locale.ROOT);
    }

    private LocalDate date(String value) {
        LocalDate result = LocalDate.parse(value);

        Checks.require(
            result.toString().equals(value),
            "Dates must use YYYY-MM-DD format"
        );

        return result;
    }

    public Supplier availableSupplier(Long supplierId) {
        Supplier supplier = Checks.found(
            suppliers.findById(supplierId),
            "Supplier not found"
        );

        Checks.require(
            supplier.getDeletedAt() == null
                && Boolean.TRUE.equals(supplier.getActive()),
            "Supplier is unavailable for new master data"
        );

        return supplier;
    }

    public void validate(Supplier supplier) {
        String name = supplier.getCanonicalName().trim();

        Checks.require(!name.isBlank(), "Supplier name is required");
        supplier.setCanonicalName(name);

        String normalized = normalizeAlias(name);

        boolean duplicateName = suppliers.findAll().stream()
            .anyMatch(other ->
                normalizeAlias(other.getCanonicalName())
                    .equals(normalized)
                && !Objects.equals(
                    other.getSupplierId(),
                    supplier.getSupplierId()
                )
            );

        Checks.conflict(
            duplicateName,
            "Canonical supplier name is already reserved"
        );

        var alias = aliases.findByNormalizedAlias(normalized);

        Checks.conflict(
            alias.isPresent()
                && !Objects.equals(
                    alias.get().getSupplierId(),
                    supplier.getSupplierId()
                ),
            "This name is an alias belonging to another supplier"
        );

        Checks.conflict(
            alias.isPresent()
                && alias.get().getDeletedAt() != null,
            "Restore the reserved alias before using this name"
        );
    }

    public void ensureCanonicalAlias(Supplier supplier) {
        String normalized = normalizeAlias(
            supplier.getCanonicalName()
        );

        var existing = aliases.findByNormalizedAlias(normalized);

        if (existing.isPresent()) {
            Checks.conflict(
                !existing.get().getSupplierId().equals(
                    supplier.getSupplierId()
                )
                || existing.get().getDeletedAt() != null,
                "Canonical alias is unavailable"
            );
            return;
        }

        var alias = new SupplierAlias();
        alias.setSupplierId(supplier.getSupplierId());
        alias.setAliasName(supplier.getCanonicalName());
        alias.setNormalizedAlias(normalized);
        aliases.save(alias);
    }

    public void validate(SupplierAlias alias) {
        availableSupplier(alias.getSupplierId());

        String name = alias.getAliasName().trim();

        Checks.require(!name.isBlank(), "Alias name is required");

        alias.setAliasName(name);
        alias.setNormalizedAlias(normalizeAlias(name));

        var existing = aliases.findByNormalizedAlias(
            alias.getNormalizedAlias()
        );

        Checks.conflict(
            existing.isPresent()
                && !Objects.equals(
                    existing.get().getAliasId(),
                    alias.getAliasId()
                ),
            "Alias is already reserved, including deleted aliases"
        );
    }

    public void protectCanonicalAlias(SupplierAlias alias) {
        Supplier supplier = Checks.found(
            suppliers.findById(alias.getSupplierId()),
            "Supplier not found"
        );

        Checks.conflict(
            normalizeAlias(supplier.getCanonicalName()).equals(
                alias.getNormalizedAlias()
            ),
            "Canonical alias cannot be edited or deleted directly; "
                + "update the supplier name first"
        );
    }

    public void editable(Contract contract) {
        Checks.conflict(
            !"DRAFT".equals(contract.getStatus()),
            "Only draft contracts can be edited"
        );

        Checks.conflict(
            !evaluations.findByContractId(
                contract.getContractId()
            ).isEmpty(),
            "A contract used in an evaluation is immutable"
        );
    }

    public void validate(Contract contract) {
        availableSupplier(contract.getSupplierId());

        LocalDate start = date(contract.getEffectiveFrom());
        LocalDate end = date(contract.getEffectiveTo());

        Checks.require(
            !end.isBefore(start),
            "Contract end date precedes start date"
        );

        contract.setContractNumber(
            contract.getContractNumber().trim()
        );
        contract.setItemCode(
            contract.getItemCode().trim()
        );

        Checks.require(
            !contract.getContractNumber().isBlank(),
            "Contract number is required"
        );
        Checks.require(
            !contract.getItemCode().isBlank(),
            "Item code is required"
        );
    }

    public void editable(ContractPriceTier tier) {
        Contract contract = Checks.found(
            contracts.findById(tier.getContractId()),
            "Contract not found"
        );

        editable(contract);

        Checks.conflict(
            tier.getTierId() != null
                && !evaluations.findByTierId(
                    tier.getTierId()
                ).isEmpty(),
            "An evaluated tier cannot be edited or deleted"
        );
    }

    public void validate(ContractPriceTier tier) {
        Contract contract = Checks.found(
            contracts.findById(tier.getContractId()),
            "Contract not found"
        );

        editable(contract);

        Checks.require(
            tier.getMinQuantity() > 0,
            "Minimum quantity must be positive"
        );

        Checks.require(
            tier.getMaxQuantity() == null
                || tier.getMaxQuantity() >= tier.getMinQuantity(),
            "Invalid quantity range"
        );

        Checks.require(
            tier.getAllowedMinPriceScaled() >= 0
                && tier.getAllowedMaxPriceScaled()
                    >= tier.getAllowedMinPriceScaled(),
            "Invalid allowed price band"
        );

        Checks.require(
            tier.getReferenceUnitPriceScaled()
                >= tier.getAllowedMinPriceScaled()
            && tier.getReferenceUnitPriceScaled()
                <= tier.getAllowedMaxPriceScaled(),
            "Reference price must be inside the price band"
        );

        long maximum = tier.getMaxQuantity() == null
            ? Long.MAX_VALUE : tier.getMaxQuantity();

        boolean overlaps = tiers.findByContractId(
            tier.getContractId()
        ).stream()
            .filter(other ->
                !Objects.equals(
                    other.getTierId(), tier.getTierId()
                )
            )
            .anyMatch(other -> {
                long otherMaximum = other.getMaxQuantity() == null
                    ? Long.MAX_VALUE : other.getMaxQuantity();

                return tier.getMinQuantity() <= otherMaximum
                    && other.getMinQuantity() <= maximum;
            });

        Checks.conflict(
            overlaps, "Quantity tiers overlap"
        );
    }

    public void approve(Contract contract) {
        editable(contract);
        validate(contract);

        var contractTiers = tiers.findByContractId(
            contract.getContractId()
        ).stream()
            .sorted(Comparator.comparing(
                ContractPriceTier::getMinQuantity
            ))
            .toList();

        Checks.require(
            !contractTiers.isEmpty(),
            "Contract requires at least one price tier"
        );

        // MVP assumption: approved contracts cover every positive
        // whole-number quantity, starting at 1.
        long expectedMinimum = 1L;

        for (int index = 0; index < contractTiers.size(); index++) {
            ContractPriceTier tier = contractTiers.get(index);

            validate(tier);

            Checks.require(
                tier.getMinQuantity() == expectedMinimum,
                "Approved tiers must cover quantities from 1 "
                    + "without gaps"
            );

            boolean last = index == contractTiers.size() - 1;

            if (last) {
                Checks.require(
                    tier.getMaxQuantity() == null,
                    "Final tier must have no upper quantity limit"
                );
            } else {
                Checks.require(
                    tier.getMaxQuantity() != null
                        && tier.getMaxQuantity() < Long.MAX_VALUE,
                    "Only the final tier may be unbounded"
                );

                expectedMinimum = Math.addExact(
                    tier.getMaxQuantity(), 1L
                );
            }
        }

        boolean overlaps = contracts.findBySupplierId(
            contract.getSupplierId()
        ).stream()
            .filter(other ->
                !Objects.equals(
                    other.getContractId(),
                    contract.getContractId()
                )
            )
            .filter(other ->
                "APPROVED".equals(other.getStatus())
                && other.getItemCode().equals(
                    contract.getItemCode()
                )
            )
            .anyMatch(other ->
                other.getEffectiveFrom().compareTo(
                    contract.getEffectiveTo()
                ) <= 0
                && contract.getEffectiveFrom().compareTo(
                    other.getEffectiveTo()
                ) <= 0
            );

        Checks.conflict(
            overlaps,
            "Approved contract overlaps another approved "
                + "agreement for this supplier and item"
        );

        contract.setStatus("APPROVED");
    }

    public void editable(ExchangeRate rate) {
        Checks.conflict(
            rate.getRateId() != null
                && !evaluations.findByRateId(
                    rate.getRateId()
                ).isEmpty(),
            "An exchange rate used by an evaluation is immutable"
        );
    }

    public void validate(ExchangeRate rate) {
        date(rate.getRateDate());

        Checks.require(
            !rate.getFromCurrency().equals(rate.getToCurrency()),
            "Exchange-rate currencies must differ"
        );

        BigDecimal decimalRate = new BigDecimal(
            rate.getConversionRate()
        );

        Checks.require(
            decimalRate.signum() > 0,
            "Exchange rate must be positive"
        );

        Checks.require(
            decimalRate.precision() <= 20
                && decimalRate.scale() >= 0
                && decimalRate.scale() <= 10,
            "Rate supports up to 20 significant digits "
                + "and 10 decimal places; avoid exponent notation"
        );

        rate.setConversionRate(
            decimalRate.stripTrailingZeros().toPlainString()
        );
        rate.setRateSource(rate.getRateSource().trim());

        Checks.require(
            !rate.getRateSource().isBlank(),
            "Rate source is required"
        );

        boolean duplicate = rates.findAll().stream()
            .filter(other ->
                !Objects.equals(
                    other.getRateId(), rate.getRateId()
                )
            )
            .anyMatch(other ->
                other.getFromCurrency().equals(
                    rate.getFromCurrency()
                )
                && other.getToCurrency().equals(
                    rate.getToCurrency()
                )
                && other.getRateDate().equals(
                    rate.getRateDate()
                )
                && other.getRateSource().equals(
                    rate.getRateSource()
                )
            );

        Checks.conflict(
            duplicate,
            "Rate already exists for this currency pair, "
                + "date, and source"
        );
    }
}
