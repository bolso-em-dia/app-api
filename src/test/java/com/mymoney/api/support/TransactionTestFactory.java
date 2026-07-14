package com.mymoney.api.support;

import com.mymoney.api.transaction.OwnershipType;
import com.mymoney.api.transaction.Transaction;
import com.mymoney.api.transaction.TransactionSourceType;
import com.mymoney.api.transaction.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.function.Consumer;

public final class TransactionTestFactory {

    private TransactionTestFactory() {}

    public static Transaction create() {
        return create(transaction -> {});
    }

    public static Transaction create(Consumer<Transaction> customizer) {
        var transaction = Transaction.builder()
                .type(TransactionType.EXPENSE)
                .ownershipType(OwnershipType.SHARED)
                .sourceType(TransactionSourceType.MANUAL)
                .description("Test Transaction")
                .amount(new BigDecimal("10.00"))
                .convertedAmount(new BigDecimal("10.00"))
                .currency("BRL")
                .transactionDate(LocalDate.of(2026, 1, 10))
                .referenceMonth(LocalDate.of(2026, 1, 1))
                .build();
        customizer.accept(transaction);
        return transaction;
    }
}
