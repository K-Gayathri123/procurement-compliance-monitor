package com.procurement.monitor.service;

import com.procurement.monitor.dto.request.*;
import com.procurement.monitor.dto.response.*;
import com.procurement.monitor.entity.SupplierAlias;
import com.procurement.monitor.mapper.ResponseMapper;
import com.procurement.monitor.repository.SupplierAliasRepository;
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
public class SupplierAliasService {

    private static final Logger log =
        LoggerFactory.getLogger(SupplierAliasService.class);

    private final SupplierAliasRepository repository;
    private final MasterDataRules rules;

    public SupplierAliasService(
        SupplierAliasRepository repository,
        MasterDataRules rules
    ) {
        this.repository = repository;
        this.rules = rules;
    }

    @Transactional(readOnly = true)
    public PageResponse<SupplierAliasResponse> list(
        int page,
        int size,
        String sort,
        String direction,
        String query,
        boolean includeDeleted,
        Boolean active,
        Long supplierId
    ) {
        var pageable = PageRules.pageable(
            page, size, sort, direction,
            Set.of("aliasId", "supplierId", "aliasName")
        );

        Specification<SupplierAlias> specification =
            (root, criteria, builder) -> {
                var predicates = new ArrayList<
                    jakarta.persistence.criteria.Predicate>();

                if (!includeDeleted) predicates.add(builder.isNull(root.get("deletedAt")));

                if (query != null && !query.isBlank()) {
                    String pattern = PageRules.searchPattern(query);
                    predicates.add(builder.or(
                        builder.like(builder.lower(root.<String>get("aliasName")), pattern, '\\'),
                        builder.like(builder.lower(root.<String>get("normalizedAlias")), pattern, '\\')
                    ));
                }
                if (supplierId != null) predicates.add(builder.equal(root.get("supplierId"), supplierId));

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
    public SupplierAliasResponse get(Long id) {
        return ResponseMapper.toResponse(
            Checks.found(
                repository.findById(id), "Record not found"
            )
        );
    }

    @Transactional
    public SupplierAliasResponse create(
        SupplierAliasRequest request,
        Long actorId
    ) {
        var entity = new SupplierAlias();


        apply(entity, request);
        rules.validate(entity);

        entity = repository.saveAndFlush(entity);


        log.info(
            "SupplierAlias created; id={}, actorId={}",
            entity.getAliasId(), actorId
        );

        return ResponseMapper.toResponse(entity);
    }

    @Transactional
    public SupplierAliasResponse update(
        Long id,
        SupplierAliasRequest request,
        Long actorId
    ) {
        var entity = Checks.found(
            repository.findById(id), "Record not found"
        );

        Checks.conflict(entity.getDeletedAt() != null, "Restore the record before editing");

        rules.protectCanonicalAlias(entity);
        Checks.require(
            entity.getSupplierId().equals(request.supplierId()),
            "An alias cannot be reassigned to another supplier"
        );


        apply(entity, request);
        rules.validate(entity);

        entity = repository.saveAndFlush(entity);


        log.info(
            "SupplierAlias updated; id={}, actorId={}",
            id, actorId
        );

        return ResponseMapper.toResponse(entity);
    }

    private void apply(
        SupplierAlias entity,
        SupplierAliasRequest request
    ) {
        entity.setSupplierId(request.supplierId());
        entity.setAliasName(request.aliasName());
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

        rules.protectCanonicalAlias(entity);


        entity.setDeletedAt(
            java.time.Instant.now().toString()
        );
        entity.setDeletedByUserId(actorId);
        entity.setDeletionReason(request.reason().trim());

        repository.save(entity);

        log.info(
            "SupplierAlias soft-deleted; id={}, actorId={}",
            id, actorId
        );
    }

    @Transactional
    public SupplierAliasResponse restore(
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

        rules.validate(entity);

        repository.save(entity);

        log.info(
            "SupplierAlias restored; id={}, actorId={}",
            id, actorId
        );

        return ResponseMapper.toResponse(entity);
    }



}
