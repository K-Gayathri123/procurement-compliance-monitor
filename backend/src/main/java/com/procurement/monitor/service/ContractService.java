package com.procurement.monitor.service;

import com.procurement.monitor.dto.request.*;
import com.procurement.monitor.dto.response.*;
import com.procurement.monitor.entity.Contract;
import com.procurement.monitor.mapper.ResponseMapper;
import com.procurement.monitor.repository.ContractRepository;
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
public class ContractService {

    private static final Logger log = LoggerFactory.getLogger(ContractService.class);

    private final ContractRepository repository;
    private final MasterDataRules rules;

    public ContractService(ContractRepository repository, MasterDataRules rules) {
        this.repository = repository;
        this.rules = rules;
    }

    @Transactional(readOnly = true)
    public PageResponse<ContractResponse> list(int page, int size, String sort, String direction, String query, boolean includeDeleted, Boolean active, Long supplierId, String status) {
        var pageable = PageRules.pageable(page, size, sort, direction, Set.of("contractId", "supplierId", "contractNumber", "itemCode", "effectiveFrom", "effectiveTo", "status"));

        Specification<Contract> specification = (root, criteria, builder) -> {
            var predicates = new ArrayList<jakarta.persistence.criteria.Predicate>();


            if (query != null && !query.isBlank()) {
                String pattern = PageRules.searchPattern(query);
                predicates.add(builder.or(builder.like(builder.lower(root.<String>get("contractNumber")), pattern, '\\'), builder.like(builder.lower(root.<String>get("itemCode")), pattern, '\\')));
            }
            if (supplierId != null) predicates.add(builder.equal(root.get("supplierId"), supplierId));
            if (status != null) predicates.add(builder.equal(root.get("status"), status));

            return builder.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };

        return PageResponse.from(repository.findAll(specification, pageable).map(ResponseMapper::toResponse));
    }

    @Transactional(readOnly = true)
    public ContractResponse get(Long id) {
        return ResponseMapper.toResponse(Checks.found(repository.findById(id), "Record not found"));
    }

    @Transactional
    public ContractResponse create(ContractRequest request, Long actorId) {
        var entity = new Contract();
        entity.setStatus("DRAFT");

        apply(entity, request);
        rules.validate(entity);

        entity = repository.saveAndFlush(entity);


        log.info("Contract created; id={}, actorId={}", entity.getContractId(), actorId);

        return ResponseMapper.toResponse(entity);
    }

    @Transactional
    public ContractResponse update(Long id, ContractRequest request, Long actorId) {
        var entity = Checks.found(repository.findById(id), "Record not found");


        rules.editable(entity);

        apply(entity, request);
        rules.validate(entity);

        entity = repository.saveAndFlush(entity);


        log.info("Contract updated; id={}, actorId={}", id, actorId);

        return ResponseMapper.toResponse(entity);
    }

    private void apply(Contract entity, ContractRequest request) {
        entity.setSupplierId(request.supplierId());
        entity.setContractNumber(request.contractNumber());
        entity.setItemCode(request.itemCode());
        entity.setEffectiveFrom(request.effectiveFrom());
        entity.setEffectiveTo(request.effectiveTo());
        entity.setCurrency(request.currency());
    }


    @Transactional
    public ContractResponse approve(Long id, Long actorId) {
        Contract entity = Checks.found(repository.findById(id), "Contract not found");

        rules.approve(entity);
        repository.save(entity);

        log.info("Contract approved; id={}, actorId={}", id, actorId);

        return ResponseMapper.toResponse(entity);
    }

    @Transactional
    public ContractResponse cancel(Long id, ActionRequest request, Long actorId) {
        Contract entity = Checks.found(repository.findById(id), "Contract not found");

        Checks.conflict("CANCELLED".equals(entity.getStatus()), "Contract is already cancelled");

        entity.setStatus("CANCELLED");
        repository.save(entity);

        // Reason is validated, but not logged because it may contain
        // sensitive text. Contract-event persistence is a future extension.
        log.info("Contract cancelled; id={}, actorId={}", id, actorId);

        return ResponseMapper.toResponse(entity);
    }

}
