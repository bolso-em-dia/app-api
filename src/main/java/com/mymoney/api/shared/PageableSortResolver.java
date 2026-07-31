package com.mymoney.api.shared;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

public final class PageableSortResolver {

    private PageableSortResolver() {}

    public static <T extends Enum<T> & ApiSortOption> Pageable resolve(
            Pageable pageable, T sortBy, T defaultSortBy, ApiSortDirection sortDir) {
        var resolvedSortBy = sortBy == null ? defaultSortBy : sortBy;
        var resolvedSortDir = sortDir == null ? resolvedSortBy.defaultDirection() : sortDir;
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), resolvedSortBy.toSort(resolvedSortDir));
    }
}
