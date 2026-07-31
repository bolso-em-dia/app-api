package com.mymoney.api.transaction;

import com.mymoney.api.shared.ApiSortDirection;
import com.mymoney.api.shared.ApiSortOption;
import java.util.List;
import org.springframework.data.domain.Sort;

public enum TransactionListSortBy implements ApiSortOption {
    DATE {
        @Override
        public ApiSortDirection defaultDirection() {
            return ApiSortDirection.DESC;
        }

        @Override
        public List<Sort.Order> orders(ApiSortDirection direction) {
            return List.of(
                    ordered("transactionDate", direction), ordered("createdAt", direction), ordered("id", direction));
        }
    },
    AMOUNT {
        @Override
        public ApiSortDirection defaultDirection() {
            return ApiSortDirection.DESC;
        }

        @Override
        public List<Sort.Order> orders(ApiSortDirection direction) {
            return List.of(
                    ordered("amount", direction),
                    descending("transactionDate"),
                    descending("createdAt"),
                    descending("id"));
        }
    },
    DESCRIPTION {
        @Override
        public ApiSortDirection defaultDirection() {
            return ApiSortDirection.ASC;
        }

        @Override
        public List<Sort.Order> orders(ApiSortDirection direction) {
            return List.of(
                    ordered("description", direction),
                    descending("transactionDate"),
                    descending("createdAt"),
                    descending("id"));
        }
    }
}
