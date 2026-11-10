package com.procurement.monitor.service;

import com.procurement.monitor.dto.request.*;
import com.procurement.monitor.dto.response.*;
import com.procurement.monitor.entity.ContractPriceTier;
import com.procurement.monitor.mapper.ResponseMapper;
import com.procurement.monitor.repository.ContractPriceTierRepository;
import com.procurement.monitor.validation.Checks;
import com.procurement.monitor.validation.MasterDataRules;
import com.procurement.monitor.validation.PageRules;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Set;

@Service
public class ContractPriceTierService {

    private static final Logger log = LoggerFactory.getLogger(ContractPriceTierService.class);
    private final ContractPriceTierRepository repository;
    private final MasterDataRules rules;
    public ContractPriceTierService(ContractPriceTierRepository repository, MasterDataRules rules) {
        this.repository = repository;
        this.rules = rules;
    }

    @Transactional(readOnly = true)
    public PageResponse<ContractPriceTierResponse> list(int page, int size, String sort, String direction, String query, boolean includeDeleted, Boolean active, Long contractId) {
        var pageable = PageRules.pageable(page, size, sort, direction, Set.of("tierId", "contractId", "minQuantity"));
        Specification<ContractPriceTier> specification = (root, criteria, builder) -> {
            var predicates = new ArrayList<jakarta.persistence.criteria.Predicate>();
            Checks.require(query == null || query.isBlank(), "Text search is not supported for price tiers");
            if (contractId != null) predicates.add(builder.equal(root.get("contractId"), contractId));
            return builder.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };

        return PageResponse.from(repository.findAll(specification, pageable).map(ResponseMapper::toResponse));
    }

    @Transactional(readOnly = true)
    public ContractPriceTierResponse get(Long id) {
        return ResponseMapper.toResponse(Checks.found(repository.findById(id), "Record not found"));
    }

    @Transactional
    public ContractPriceTierResponse create(ContractPriceTierRequest request, Long actorId) {
        var entity = new ContractPriceTier();
        apply(entity, request);
        rules.validate(entity);
        entity = repository.saveAndFlush(entity);
        log.info("ContractPriceTier created; id={}, actorId={}", entity.getTierId(), actorId);
        return ResponseMapper.toResponse(entity);
    }

    @Transactional
    public ContractPriceTierResponse update(Long id, ContractPriceTierRequest request, Long actorId) {
        var entity = Checks.found(repository.findById(id), "Record not found");
        rules.editable(entity);
        Checks.require(entity.getContractId().equals(request.contractId()), "A tier cannot be moved to another contract");
        apply(entity, request);
        rules.validate(entity);
        entity = repository.saveAndFlush(entity);
        log.info("ContractPriceTier updated; id={}, actorId={}", id, actorId);
        return ResponseMapper.toResponse(entity);
    }

    private void apply(ContractPriceTier entity, ContractPriceTierRequest request) {
        entity.setContractId(request.contractId());
        entity.setMinQuantity(request.minQuantity());
        entity.setMaxQuantity(request.maxQuantity());
        entity.setReferenceUnitPriceScaled(request.referenceUnitPriceScaled());
        entity.setAllowedMinPriceScaled(request.allowedMinPriceScaled());
        entity.setAllowedMaxPriceScaled(request.allowedMaxPriceScaled());
    }


    @Transactional
    public void deleteUnused(Long id, Long actorId) {
        ContractPriceTier entity = Checks.found(repository.findById(id), "Price tier not found");

        rules.editable(entity);
        repository.delete(entity);

        log.info("Unused draft tier deleted; id={}, actorId={}", id, actorId);
    }

}
