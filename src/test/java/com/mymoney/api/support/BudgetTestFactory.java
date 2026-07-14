package com.mymoney.api.support;

import com.mymoney.api.budget.Budget;
import com.mymoney.api.budget.BudgetType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.function.Consumer;

public final class BudgetTestFactory {

    private BudgetTestFactory() {}

    public static Budget create() {
        return create(budget -> {});
    }

    public static Budget create(Consumer<Budget> customizer) {
        var budget = Budget.builder()
                .name("Test Budget")
                .type(BudgetType.GLOBAL)
                .monthlyLimit(new BigDecimal("100.00"))
                .createdInMonth(LocalDate.of(2026, 1, 1))
                .active(true)
                .build();
        customizer.accept(budget);
        return budget;
    }
}
