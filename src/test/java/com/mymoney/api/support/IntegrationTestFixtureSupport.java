package com.mymoney.api.support;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.mymoney.api.account.Account;
import com.mymoney.api.budget.Budget;
import com.mymoney.api.category.Category;
import com.mymoney.api.exchangerate.ExchangeRate;
import com.mymoney.api.fixedexpense.FixedExpenseTemplate;
import com.mymoney.api.member.FamilyMember;
import com.mymoney.api.transaction.Transaction;
import java.time.LocalDate;
import java.util.function.Consumer;
import org.springframework.stereotype.Component;

@Component
public class IntegrationTestFixtureSupport {

    public static final String ADMIN_EMAIL = "admin@bolso-em-dia.local";
    public static final String ADMIN_PASSWORD = "admin123456";
    public static final String USER_EMAIL = "user@bolso-em-dia.local";
    public static final String USER_PASSWORD = "user123456";

    private final TestIdentityFixtureSupport identityFixtureSupport;
    private final TestEntityFixtureSupport entityFixtureSupport;
    private final TestSharedSupport sharedSupport;

    public IntegrationTestFixtureSupport(
            TestIdentityFixtureSupport identityFixtureSupport,
            TestEntityFixtureSupport entityFixtureSupport,
            TestSharedSupport sharedSupport) {
        this.identityFixtureSupport = identityFixtureSupport;
        this.entityFixtureSupport = entityFixtureSupport;
        this.sharedSupport = sharedSupport;
    }

    public LocalDate currentReferenceMonth() {
        return sharedSupport.currentReferenceMonth();
    }

    public LocalDate today() {
        return sharedSupport.today();
    }

    public String writeJson(Object value) throws JsonProcessingException {
        return sharedSupport.writeJson(value);
    }

    public FamilyMember ensureAdminCanUseProtectedApis() {
        return identityFixtureSupport.ensureAdminCanUseProtectedApis();
    }

    public FamilyMember ensureRegularUser() {
        return identityFixtureSupport.ensureRegularUser();
    }

    public FamilyMember persistFamilyMember(String email, String rawPassword, Consumer<FamilyMember> customizer) {
        return identityFixtureSupport.persistFamilyMember(email, rawPassword, customizer);
    }

    public FamilyMember persistAllowanceMember(String name, String email, String rawPassword) {
        return identityFixtureSupport.persistAllowanceMember(name, email, rawPassword);
    }

    public Account persistAccount(Consumer<Account> customizer) {
        return entityFixtureSupport.persistAccount(customizer);
    }

    public Category persistCategory(Consumer<Category> customizer) {
        return entityFixtureSupport.persistCategory(customizer);
    }

    public Budget persistBudget(Consumer<Budget> customizer) {
        return entityFixtureSupport.persistBudget(customizer);
    }

    public Transaction persistTransaction(Consumer<Transaction> customizer) {
        return entityFixtureSupport.persistTransaction(customizer);
    }

    public FixedExpenseTemplate persistFixedExpenseTemplate(Consumer<FixedExpenseTemplate> customizer) {
        return entityFixtureSupport.persistFixedExpenseTemplate(customizer);
    }

    public ExchangeRate persistExchangeRate(Consumer<ExchangeRate> customizer) {
        return entityFixtureSupport.persistExchangeRate(customizer);
    }
}
