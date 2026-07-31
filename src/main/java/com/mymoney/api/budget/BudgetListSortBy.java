package com.mymoney.api.budget;

import com.mymoney.api.shared.ApiSortDirection;

public enum BudgetListSortBy {
    NAME(ApiSortDirection.ASC),
    MONTHLY_LIMIT(ApiSortDirection.DESC),
    REMAINING_AMOUNT(ApiSortDirection.DESC);

    private final ApiSortDirection defaultDirection;

    BudgetListSortBy(ApiSortDirection defaultDirection) {
        this.defaultDirection = defaultDirection;
    }

    public ApiSortDirection defaultDirection() {
        return defaultDirection;
    }
}
