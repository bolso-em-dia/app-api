package com.mymoney.api.fixedexpense;

import com.mymoney.api.shared.ApiSortDirection;
import com.mymoney.api.shared.ApiSortOption;
import java.util.List;
import org.springframework.data.domain.Sort;

public enum FixedExpenseTemplateListSortBy implements ApiSortOption {
    NAME {
        @Override
        public ApiSortDirection defaultDirection() {
            return ApiSortDirection.ASC;
        }

        @Override
        public List<Sort.Order> orders(ApiSortDirection direction) {
            return List.of(ordered("name", direction), ascending("id"));
        }
    },
    AMOUNT {
        @Override
        public ApiSortDirection defaultDirection() {
            return ApiSortDirection.DESC;
        }

        @Override
        public List<Sort.Order> orders(ApiSortDirection direction) {
            return List.of(ordered("amount", direction), ascending("name"), ascending("id"));
        }
    },
    DUE_DAY {
        @Override
        public ApiSortDirection defaultDirection() {
            return ApiSortDirection.ASC;
        }

        @Override
        public List<Sort.Order> orders(ApiSortDirection direction) {
            return List.of(ordered("dueDay", direction), ascending("name"), ascending("id"));
        }
    }
}
