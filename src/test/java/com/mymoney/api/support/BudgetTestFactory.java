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
        var budget = new Budget();
        budget.setName("Test Budget");
        budget.setType(BudgetType.GLOBAL);
        budget.setMonthlyLimit(new BigDecimal("100.00"));
        budget.setCreatedInMonth(LocalDate.of(2026, 1, 1));
        budget.setActive(true);
        customizer.accept(budget);
        return budget;
    }
}
