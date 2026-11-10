package com.procurement.monitor.service;

import com.procurement.monitor.dto.request.*;
import com.procurement.monitor.dto.response.*;
import com.procurement.monitor.entity.ExchangeRate;
import com.procurement.monitor.mapper.ResponseMapper;
import com.procurement.monitor.repository.ExchangeRateRepository;
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
public class ExchangeRateService {

    private static final Logger log =
        LoggerFactory.getLogger(ExchangeRateService.class);

    private final ExchangeRateRepository repository;
    private final MasterDataRules rules;

    public ExchangeRateService(
        ExchangeRateRepository repository,
        MasterDataRules rules
    ) {
        this.repository = repository;
        this.rules = rules;
    }

    @Transactional(readOnly = true)
    public PageResponse<ExchangeRateResponse> list(
        int page,
        int size,
        String sort,
        String direction,
        String query,
        boolean includeDeleted,
        Boolean active,
        String fromCurrency,
        String toCurrency,
        String rateDate
    ) {
        var pageable = PageRules.pageable(
            page, size, sort, direction,
            Set.of("rateId", "fromCurrency", "toCurrency", "rateDate", "rateSource")
        );

        Specification<ExchangeRate> specification =
            (root, criteria, builder) -> {
                var predicates = new ArrayList<
                    jakarta.persistence.criteria.Predicate>();



                if (query != null && !query.isBlank()) {
                    String pattern = PageRules.searchPattern(query);
                    predicates.add(builder.or(
                        builder.like(builder.lower(root.<String>get("rateSource")), pattern, '\\')
                    ));
                }
                if (fromCurrency != null) predicates.add(builder.equal(root.get("fromCurrency"), fromCurrency));
                if (toCurrency != null) predicates.add(builder.equal(root.get("toCurrency"), toCurrency));
                if (rateDate != null) predicates.add(builder.equal(root.get("rateDate"), rateDate));

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
    public ExchangeRateResponse get(Long id) {
        return ResponseMapper.toResponse(
            Checks.found(
                repository.findById(id), "Record not found"
            )
        );
    }

    @Transactional
    public ExchangeRateResponse create(
        ExchangeRateRequest request,
        Long actorId
    ) {
        var entity = new ExchangeRate();


        apply(entity, request);
        rules.validate(entity);

        entity = repository.saveAndFlush(entity);


        log.info(
            "ExchangeRate created; id={}, actorId={}",
            entity.getRateId(), actorId
        );

        return ResponseMapper.toResponse(entity);
    }

    @Transactional
    public ExchangeRateResponse update(
        Long id,
        ExchangeRateRequest request,
        Long actorId
    ) {
        var entity = Checks.found(
            repository.findById(id), "Record not found"
        );


        rules.editable(entity);

        apply(entity, request);
        rules.validate(entity);

        entity = repository.saveAndFlush(entity);


        log.info(
            "ExchangeRate updated; id={}, actorId={}",
            id, actorId
        );

        return ResponseMapper.toResponse(entity);
    }

    private void apply(
        ExchangeRate entity,
        ExchangeRateRequest request
    ) {
        entity.setFromCurrency(request.fromCurrency());
        entity.setToCurrency(request.toCurrency());
        entity.setRateDate(request.rateDate());
        entity.setConversionRate(request.conversionRate());
        entity.setRateSource(request.rateSource());
    }




    @Transactional
    public void deleteUnused(Long id, Long actorId) {
        ExchangeRate entity = Checks.found(
            repository.findById(id), "Exchange rate not found"
        );

        rules.editable(entity);
        repository.delete(entity);

        log.info(
            "Unused exchange rate deleted; id={}, actorId={}",
            id, actorId
        );
    }

}
