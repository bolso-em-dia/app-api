package com.mymoney.api.support;

import com.mymoney.api.account.Account;
import com.mymoney.api.budget.BudgetType;
import com.mymoney.api.category.Category;
import com.mymoney.api.member.FamilyMember;
import com.mymoney.api.transaction.OwnershipType;
import com.mymoney.api.transaction.TransactionSourceType;
import com.mymoney.api.transaction.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class DashboardIntegrationFixtureSupport {

    private final TestIdentityFixtureSupport identityFixtureSupport;
    private final TestEntityFixtureSupport entityFixtureSupport;
    private final TestSharedSupport sharedSupport;

    public DashboardIntegrationFixtureSupport(
            TestIdentityFixtureSupport identityFixtureSupport,
            TestEntityFixtureSupport entityFixtureSupport,
            TestSharedSupport sharedSupport) {
        this.identityFixtureSupport = identityFixtureSupport;
        this.entityFixtureSupport = entityFixtureSupport;
        this.sharedSupport = sharedSupport;
    }

    public FamilyMember createAllowanceMember() {
        return identityFixtureSupport.persistAllowanceMember(
                "Karol", "karol-dashboard@bolso-em-dia.local", "karol123456");
    }

    public Category createGroceriesCategory() {
        return entityFixtureSupport.persistCategory(category -> {
            category.setName("Groceries");
            category.setCreatedInMonth(sharedSupport.currentReferenceMonth());
        });
    }

    public Category createSalaryCategory() {
        return entityFixtureSupport.persistCategory(category -> {
            category.setName("Salary");
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

    public void createFamilyEssentialsBudget(Category groceries, Category transport) {
        entityFixtureSupport.persistBudget(created -> {
            created.setName("Family Essentials");
            created.setType(BudgetType.GLOBAL);
            created.setMonthlyLimit(new BigDecimal("1000.00"));
            created.setCreatedInMonth(sharedSupport.currentReferenceMonth());
            created.setActive(true);
            created.setCategories(new LinkedHashSet<>(List.of(groceries, transport)));
        });
    }

    public void createAllowanceBudget(FamilyMember allowanceMember) {
        entityFixtureSupport.persistBudget(created -> {
            created.setName("Karol Allowance");
            created.setType(BudgetType.ALLOWANCE);
            created.setOwnerMember(allowanceMember);
            created.setMonthlyLimit(new BigDecimal("400.00"));
            created.setCreatedInMonth(sharedSupport.currentReferenceMonth());
            created.setActive(true);
        });
    }

    public void createSalaryTransaction(Account account, Category salary) {
        entityFixtureSupport.persistTransaction(created -> {
            created.setType(TransactionType.INCOME);
            created.setOwnershipType(OwnershipType.SHARED);
            created.setSourceType(TransactionSourceType.MANUAL);
            created.setDescription("June salary");
            created.setAmount(new BigDecimal("5000.00"));
            created.setConvertedAmount(new BigDecimal("5000.00"));
            created.setTransactionDate(LocalDate.of(2026, 6, 5));
            created.setReferenceMonth(sharedSupport.currentReferenceMonth());
            created.setAccount(account);
            created.setCategory(salary);
        });
    }

    public void createMarketTransaction(Account account, Category groceries) {
        entityFixtureSupport.persistTransaction(created -> {
            created.setType(TransactionType.EXPENSE);
            created.setOwnershipType(OwnershipType.SHARED);
            created.setSourceType(TransactionSourceType.MANUAL);
            created.setDescription("Market");
            created.setAmount(new BigDecimal("150.00"));
            created.setConvertedAmount(new BigDecimal("150.00"));
            created.setTransactionDate(LocalDate.of(2026, 6, 10));
            created.setReferenceMonth(sharedSupport.currentReferenceMonth());
            created.setAccount(account);
            created.setCategory(groceries);
        });
    }

    public void createAllowanceRideAppTransaction(Account account, Category transport, FamilyMember allowanceMember) {
        entityFixtureSupport.persistTransaction(created -> {
            created.setType(TransactionType.EXPENSE);
            created.setOwnershipType(OwnershipType.INDIVIDUAL);
            created.setSourceType(TransactionSourceType.MANUAL);
            created.setDescription("Ride app");
            created.setAmount(new BigDecimal("45.00"));
            created.setConvertedAmount(new BigDecimal("45.00"));
            created.setTransactionDate(LocalDate.of(2026, 6, 11));
            created.setReferenceMonth(sharedSupport.currentReferenceMonth());
            created.setAccount(account);
            created.setCategory(transport);
            created.setMember(allowanceMember);
        });
    }
}
