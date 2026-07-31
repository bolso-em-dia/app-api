package com.mymoney.api.shared;

import java.util.List;
import org.springframework.data.domain.Sort;

public interface ApiSortOption {

    ApiSortDirection defaultDirection();

    List<Sort.Order> orders(ApiSortDirection direction);

    default Sort toSort(ApiSortDirection direction) {
        return Sort.by(orders(direction));
    }

    default Sort.Order ordered(String property, ApiSortDirection direction) {
        return new Sort.Order(direction.toSpringDirection(), property);
    }

    default Sort.Order ascending(String property) {
        return new Sort.Order(Sort.Direction.ASC, property);
    }

    default Sort.Order descending(String property) {
        return new Sort.Order(Sort.Direction.DESC, property);
    }
}
