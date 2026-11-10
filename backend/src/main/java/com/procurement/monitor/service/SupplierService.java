package com.procurement.monitor.service;

import com.procurement.monitor.dto.request.*;
import com.procurement.monitor.dto.response.*;
import com.procurement.monitor.entity.Supplier;
import com.procurement.monitor.mapper.ResponseMapper;
import com.procurement.monitor.repository.SupplierRepository;
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
public class SupplierService {

    private static final Logger log =
        LoggerFactory.getLogger(SupplierService.class);

    private final SupplierRepository repository;
    private final MasterDataRules rules;

    public SupplierService(
        SupplierRepository repository,
        MasterDataRules rules
    ) {
        this.repository = repository;
        this.rules = rules;
    }

    @Transactional(readOnly = true)
    public PageResponse<SupplierResponse> list(
        int page,
        int size,
        String sort,
        String direction,
        String query,
        boolean includeDeleted,
        Boolean active
    ) {
        var pageable = PageRules.pageable(
            page, size, sort, direction,
            Set.of("supplierId", "canonicalName", "active")
        );

        Specification<Supplier> specification =
            (root, criteria, builder) -> {
                var predicates = new ArrayList<
                    jakarta.persistence.criteria.Predicate>();

                if (!includeDeleted) predicates.add(builder.isNull(root.get("deletedAt")));
                if (active != null) predicates.add(builder.equal(root.get("active"), active));
                if (query != null && !query.isBlank()) {
                    String pattern = PageRules.searchPattern(query);
                    predicates.add(builder.or(
                        builder.like(builder.lower(root.<String>get("canonicalName")), pattern, '\\')
                    ));
                }


                return builder.and(
                    predicates.toArray(
                        jakarta.persistence.criteria.Predicate[]::new
                    )
                );
            };

        return PageResponse.from(
            repository.findAll(
                specification, pageable
            ).map(ResponseMapper::toResponse)
        );
    }

    @Transactional(readOnly = true)
    public SupplierResponse get(Long id) {
        return ResponseMapper.toResponse(
            Checks.found(
                repository.findById(id), "Record not found"
            )
        );
    }

    @Transactional
    public SupplierResponse create(
        SupplierRequest request,
        Long actorId
    ) {
        var entity = new Supplier();
        entity.setActive(true);

        apply(entity, request);
        rules.validate(entity);

        entity = repository.saveAndFlush(entity);
        rules.ensureCanonicalAlias(entity);

        log.info(
            "Supplier created; id={}, actorId={}",
            entity.getSupplierId(), actorId
        );

        return ResponseMapper.toResponse(entity);
    }

    @Transactional
    public SupplierResponse update(
        Long id,
        SupplierRequest request,
        Long actorId
    ) {
        var entity = Checks.found(
            repository.findById(id), "Record not found"
        );

        Checks.conflict(entity.getDeletedAt() != null, "Restore the record before editing");


        apply(entity, request);
        rules.validate(entity);

        entity = repository.saveAndFlush(entity);
        rules.ensureCanonicalAlias(entity);

        log.info(
            "Supplier updated; id={}, actorId={}",
            id, actorId
        );

        return ResponseMapper.toResponse(entity);
    }

    private void apply(
        Supplier entity,
        SupplierRequest request
    ) {
        entity.setCanonicalName(request.canonicalName());
    }


    @Transactional
    public void softDelete(
        Long id, ActionRequest request, Long actorId
    ) {
        var entity = Checks.found(
            repository.findById(id), "Record not found"
        );

        Checks.conflict(
            entity.getDeletedAt() != null,
            "Record is already soft-deleted"
        );


        entity.setActive(false);

        entity.setDeletedAt(
            java.time.Instant.now().toString()
        );
        entity.setDeletedByUserId(actorId);
        entity.setDeletionReason(request.reason().trim());

        repository.save(entity);

        log.info(
            "Supplier soft-deleted; id={}, actorId={}",
            id, actorId
        );
    }

    @Transactional
    public SupplierResponse restore(
        Long id, Long actorId
    ) {
        var entity = Checks.found(
            repository.findById(id), "Record not found"
        );

        Checks.conflict(
            entity.getDeletedAt() == null,
            "Record is not soft-deleted"
        );

        entity.setDeletedAt(null);
        entity.setDeletedByUserId(null);
        entity.setDeletionReason(null);
        entity.setActive(true);
        rules.validate(entity);
        rules.ensureCanonicalAlias(entity);

        repository.save(entity);

        log.info(
            "Supplier restored; id={}, actorId={}",
            id, actorId
        );

        return ResponseMapper.toResponse(entity);
    }



}
