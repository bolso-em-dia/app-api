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
        var transaction = new Transaction();
        transaction.setType(TransactionType.EXPENSE);
        transaction.setOwnershipType(OwnershipType.SHARED);
        transaction.setSourceType(TransactionSourceType.MANUAL);
        transaction.setDescription("Test Transaction");
        transaction.setAmount(new BigDecimal("10.00"));
        transaction.setConvertedAmount(new BigDecimal("10.00"));
        transaction.setCurrency("BRL");
        transaction.setTransactionDate(LocalDate.of(2026, 1, 10));
        transaction.setReferenceMonth(LocalDate.of(2026, 1, 1));
        customizer.accept(transaction);
        return transaction;
    }
}
