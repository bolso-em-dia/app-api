package com.mymoney.api.shared;

import org.springframework.data.domain.Sort;

public enum ApiSortDirection {
    ASC,
    DESC;

    public Sort.Direction toSpringDirection() {
        return this == ASC ? Sort.Direction.ASC : Sort.Direction.DESC;
    }
}
