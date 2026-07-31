package com.mymoney.api.category;

import com.mymoney.api.shared.ApiSortDirection;
import com.mymoney.api.shared.ApiSortOption;
import java.util.List;
import org.springframework.data.domain.Sort;

public enum CategoryListSortBy implements ApiSortOption {
    NAME;

    @Override
    public ApiSortDirection defaultDirection() {
        return ApiSortDirection.ASC;
    }

    @Override
    public List<Sort.Order> orders(ApiSortDirection direction) {
        return List.of(ordered("name", direction), ascending("id"));
    }
}
