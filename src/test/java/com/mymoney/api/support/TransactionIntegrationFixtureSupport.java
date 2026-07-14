package com.mymoney.api.support;

import com.mymoney.api.account.Account;
import com.mymoney.api.category.Category;
import com.mymoney.api.member.FamilyMember;
import com.mymoney.api.transaction.OwnershipType;
import com.mymoney.api.transaction.Transaction;
import com.mymoney.api.transaction.TransactionSourceType;
import com.mymoney.api.transaction.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.stereotype.Component;

@Component
public class TransactionIntegrationFixtureSupport {

    private final TestIdentityFixtureSupport identityFixtureSupport;
    private final TestEntityFixtureSupport entityFixtureSupport;
    private final TestSharedSupport sharedSupport;

    public TransactionIntegrationFixtureSupport(
            TestIdentityFixtureSupport identityFixtureSupport,
            TestEntityFixtureSupport entityFixtureSupport,
            TestSharedSupport sharedSupport) {
        this.identityFixtureSupport = identityFixtureSupport;
        this.entityFixtureSupport = entityFixtureSupport;
        this.sharedSupport = sharedSupport;
    }

    public FamilyMember createAllowanceMember() {
        return identityFixtureSupport.persistAllowanceMember("Karol", "karol@bolso-em-dia.local", "karol123456");
    }

    public Category createGroceriesCategory() {
        return entityFixtureSupport.persistCategory(category -> {
            category.setName("Groceries");
            category.setCreatedInMonth(sharedSupport.currentReferenceMonth());
        });
    }

    public Category createTransportCategory() {
        return entityFixtureSupport.persistCategory(category -> {
            category.setName("Transport");
            category.setCreatedInMonth(sharedSupport.currentReferenceMonth());
        });
    }

    public Account createMainCheckingAccount() {
        return entityFixtureSupport.persistAccount(created -> {
            created.setName("Main Checking");
            created.setCreatedInMonth(sharedSupport.currentReferenceMonth());
        });
    }

    public Transaction createSharedTransaction(Account account, Category category) {
        return entityFixtureSupport.persistTransaction(created -> {
            created.setType(TransactionType.EXPENSE);
            created.setOwnershipType(OwnershipType.SHARED);
            created.setSourceType(TransactionSourceType.MANUAL);
            created.setDescription("Market");
            created.setAmount(new BigDecimal("150.00"));
            created.setConvertedAmount(new BigDecimal("150.00"));
            created.setTransactionDate(LocalDate.of(2026, 6, 10));
            created.setReferenceMonth(sharedSupport.currentReferenceMonth());
            created.setCategory(category);
            created.setAccount(account);
        });
    }

    public Transaction createTransportTransaction(Account account, Category category) {
        return entityFixtureSupport.persistTransaction(created -> {
            created.setType(TransactionType.EXPENSE);
            created.setOwnershipType(OwnershipType.SHARED);
            created.setSourceType(TransactionSourceType.MANUAL);
            created.setDescription("Taxi");
            created.setAmount(new BigDecimal("45.00"));
            created.setConvertedAmount(new BigDecimal("45.00"));
            created.setTransactionDate(LocalDate.of(2026, 6, 11));
            created.setReferenceMonth(sharedSupport.currentReferenceMonth());
            created.setCategory(category);
            created.setAccount(account);
        });
    }
}
