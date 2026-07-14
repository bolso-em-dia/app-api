package com.mymoney.api.support;

import com.mymoney.api.fixedexpense.FixedExpenseTemplate;
import com.mymoney.api.transaction.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.function.Consumer;

public final class FixedExpenseTemplateTestFactory {

    private FixedExpenseTemplateTestFactory() {}

    public static FixedExpenseTemplate create() {
        return create(template -> {});
    }

    public static FixedExpenseTemplate create(Consumer<FixedExpenseTemplate> customizer) {
        var template = FixedExpenseTemplate.builder()
                .name("Test Fixed Expense")
                .type(TransactionType.EXPENSE)
                .amount(new BigDecimal("100.00"))
                .convertedAmount(new BigDecimal("100.00"))
                .dueDay((short) 10)
                .createdInMonth(LocalDate.of(2026, 1, 1))
                .active(true)
                .build();
        customizer.accept(template);
        return template;
    }
}
