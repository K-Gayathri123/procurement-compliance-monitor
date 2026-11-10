package com.procurement.monitor.validation;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Locale;
import java.util.Set;

public final class PageRules {

    private PageRules() {}

    public static Pageable pageable(
        int page,
        int size,
        String sort,
        String direction,
        Set<String> allowedSorts
    ) {
        Checks.require(page >= 0, "Page must be zero or greater");
        Checks.require(
            size >= 1 && size <= 100,
            "Size must be between 1 and 100"
        );
        Checks.require(
            allowedSorts.contains(sort),
            "Unsupported sort field"
        );

        String normalized = direction.toUpperCase(Locale.ROOT);

        Checks.require(
            normalized.equals("ASC") || normalized.equals("DESC"),
            "Direction must be ASC or DESC"
        );

        return PageRequest.of(
            page,
            size,
            Sort.by(
                Sort.Direction.valueOf(normalized),
                sort
            )
        );
    }

    public static String searchPattern(String value) {
        return "%" + value.trim()
            .toLowerCase(Locale.ROOT)
            .replace("\\", "\\\\")
            .replace("%", "\\%")
            .replace("_", "\\_")
            + "%";
    }
}
