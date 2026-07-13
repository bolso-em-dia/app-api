package com.mymoney.api.support;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mymoney.api.account.Account;
import com.mymoney.api.account.AccountRepository;
import com.mymoney.api.budget.Budget;
import com.mymoney.api.budget.BudgetRepository;
import com.mymoney.api.budget.BudgetType;
import com.mymoney.api.category.Category;
import com.mymoney.api.category.CategoryRepository;
import com.mymoney.api.exchangerate.ExchangeRate;
import com.mymoney.api.exchangerate.ExchangeRateRepository;
import com.mymoney.api.fixedexpense.FixedExpenseTemplate;
import com.mymoney.api.fixedexpense.FixedExpenseTemplateRepository;
import com.mymoney.api.member.FamilyMember;
import com.mymoney.api.member.FamilyMemberRepository;
import com.mymoney.api.member.FamilyRole;
import com.mymoney.api.shared.DateProvider;
import com.mymoney.api.transaction.OwnershipType;
import com.mymoney.api.transaction.Transaction;
import com.mymoney.api.transaction.TransactionRepository;
import com.mymoney.api.transaction.TransactionSourceType;
import com.mymoney.api.transaction.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.function.Consumer;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class IntegrationTestFixtureSupport {

    public static final String ADMIN_EMAIL = "admin@bolso-em-dia.local";
    public static final String ADMIN_PASSWORD = "admin123456";
    public static final String USER_EMAIL = "user@bolso-em-dia.local";
    public static final String USER_PASSWORD = "user123456";

    private final FamilyMemberRepository familyMemberRepository;
    private final PasswordEncoder passwordEncoder;
    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;
    private final BudgetRepository budgetRepository;
    private final TransactionRepository transactionRepository;
    private final FixedExpenseTemplateRepository fixedExpenseTemplateRepository;
    private final ExchangeRateRepository exchangeRateRepository;
    private final ObjectMapper objectMapper;
    private final DateProvider dateProvider;

    public IntegrationTestFixtureSupport(
            FamilyMemberRepository familyMemberRepository,
            PasswordEncoder passwordEncoder,
            AccountRepository accountRepository,
            CategoryRepository categoryRepository,
            BudgetRepository budgetRepository,
            TransactionRepository transactionRepository,
            FixedExpenseTemplateRepository fixedExpenseTemplateRepository,
            ExchangeRateRepository exchangeRateRepository,
            ObjectMapper objectMapper,
            DateProvider dateProvider) {
        this.familyMemberRepository = familyMemberRepository;
        this.passwordEncoder = passwordEncoder;
        this.accountRepository = accountRepository;
        this.categoryRepository = categoryRepository;
        this.budgetRepository = budgetRepository;
        this.transactionRepository = transactionRepository;
        this.fixedExpenseTemplateRepository = fixedExpenseTemplateRepository;
        this.exchangeRateRepository = exchangeRateRepository;
        this.objectMapper = objectMapper;
        this.dateProvider = dateProvider;
    }

    public LocalDate currentReferenceMonth() {
        return dateProvider.currentReferenceMonth();
    }

    public LocalDate today() {
        return dateProvider.today();
    }

    public String writeJson(Object value) throws JsonProcessingException {
        return objectMapper.writeValueAsString(value);
    }

    public FamilyMember ensureAdminCanUseProtectedApis() {
        var admin = familyMemberRepository.findByEmailIgnoreCase(ADMIN_EMAIL).orElseThrow();
        if (!admin.isMustChangePassword()) {
            return admin;
        }
        admin.setMustChangePassword(false);
        return familyMemberRepository.save(admin);
    }

    public FamilyMember ensureRegularUser() {
        return persistFamilyMember(USER_EMAIL, USER_PASSWORD, member -> {
            member.setName("Regular User");
            member.setRole(FamilyRole.USER);
            member.setActive(true);
            member.setAllowanceEnabled(false);
            member.setMustChangePassword(false);
        });
    }

    public FamilyMember persistFamilyMember(String email, String rawPassword, Consumer<FamilyMember> customizer) {
        var member = familyMemberRepository.findByEmailIgnoreCase(email).orElseGet(FamilyMemberTestFactory::create);
        member.setEmail(email);
        member.setPasswordHash(passwordEncoder.encode(rawPassword));
        member.setRole(FamilyRole.USER);
        member.setActive(true);
        member.setAllowanceEnabled(false);
        member.setMustChangePassword(false);
        customizer.accept(member);
        return familyMemberRepository.save(member);
    }

    public FamilyMember persistAllowanceMember(String name, String email, String rawPassword) {
        return persistFamilyMember(email, rawPassword, member -> {
            member.setName(name);
            member.setAllowanceEnabled(true);
        });
    }

    public Account persistAccount(Consumer<Account> customizer) {
        return accountRepository.save(AccountTestFactory.create(customizer));
    }

    public Category persistCategory(Consumer<Category> customizer) {
        return categoryRepository.save(CategoryTestFactory.create(customizer));
    }

    public Budget persistBudget(Consumer<Budget> customizer) {
        return budgetRepository.save(BudgetTestFactory.create(customizer));
    }

    public Transaction persistTransaction(Consumer<Transaction> customizer) {
        return transactionRepository.save(TransactionTestFactory.create(customizer));
    }

    public FixedExpenseTemplate persistFixedExpenseTemplate(Consumer<FixedExpenseTemplate> customizer) {
        var template = new FixedExpenseTemplate();
        template.setName("Test Fixed Expense");
        template.setType(TransactionType.EXPENSE);
        template.setAmount(new BigDecimal("100.00"));
        template.setConvertedAmount(new BigDecimal("100.00"));
        template.setDueDay((short) 10);
        template.setCreatedInMonth(currentReferenceMonth());
        template.setActive(true);
        customizer.accept(template);
        return fixedExpenseTemplateRepository.save(template);
    }

    public ExchangeRate persistExchangeRate(Consumer<ExchangeRate> customizer) {
        var rate = new ExchangeRate();
        rate.setCurrency("USD");
        rate.setRate(new BigDecimal("5.00"));
        rate.setFetchedAt(OffsetDateTime.parse("2026-06-15T12:00:00Z"));
        customizer.accept(rate);
        return exchangeRateRepository.save(rate);
    }

    public TransactionScenario createTransactionScenario() {
        var regularUser = ensureRegularUser();
        var allowanceMember = persistAllowanceMember("Karol", "karol@bolso-em-dia.local", "karol123456");
        var groceries = persistCategory(category -> {
            category.setName("Groceries");
            category.setCreatedInMonth(currentReferenceMonth());
        });
        var transport = persistCategory(category -> {
            category.setName("Transport");
            category.setCreatedInMonth(currentReferenceMonth());
        });
        var account = persistAccount(created -> {
            created.setName("Main Checking");
            created.setCreatedInMonth(currentReferenceMonth());
        });
        var sharedTransaction = persistTransaction(created -> {
            created.setType(TransactionType.EXPENSE);
            created.setOwnershipType(OwnershipType.SHARED);
            created.setSourceType(TransactionSourceType.MANUAL);
            created.setDescription("Market");
            created.setAmount(new BigDecimal("150.00"));
            created.setConvertedAmount(new BigDecimal("150.00"));
            created.setTransactionDate(LocalDate.of(2026, 6, 10));
            created.setReferenceMonth(currentReferenceMonth());
            created.setCategory(groceries);
            created.setAccount(account);
        });
        var transportTransaction = persistTransaction(created -> {
            created.setType(TransactionType.EXPENSE);
            created.setOwnershipType(OwnershipType.SHARED);
            created.setSourceType(TransactionSourceType.MANUAL);
            created.setDescription("Taxi");
            created.setAmount(new BigDecimal("45.00"));
            created.setConvertedAmount(new BigDecimal("45.00"));
            created.setTransactionDate(LocalDate.of(2026, 6, 11));
            created.setReferenceMonth(currentReferenceMonth());
            created.setCategory(transport);
            created.setAccount(account);
        });
        return new TransactionScenario(
                regularUser, allowanceMember, groceries, transport, account, sharedTransaction, transportTransaction);
    }

    public BudgetScenario createBudgetScenario() {
        var regularUser = ensureRegularUser();
        var allowanceMember = persistAllowanceMember("Karol", "karol-budget@bolso-em-dia.local", "karol123456");
        var groceries = persistCategory(category -> {
            category.setName("Groceries");
            category.setCreatedInMonth(currentReferenceMonth());
        });
        var transport = persistCategory(category -> {
            category.setName("Transport");
            category.setCreatedInMonth(currentReferenceMonth());
        });
        var account = persistAccount(created -> {
            created.setName("Main Checking");
            created.setCreatedInMonth(currentReferenceMonth());
        });
        var globalBudget = persistBudget(created -> {
            created.setName("Family Essentials");
            created.setType(BudgetType.GLOBAL);
            created.setMonthlyLimit(new BigDecimal("1000.00"));
            created.setCreatedInMonth(currentReferenceMonth());
            created.setActive(true);
            created.setCategories(new LinkedHashSet<>(List.of(groceries, transport)));
        });
        var allowanceBudget = persistBudget(created -> {
            created.setName("Karol Allowance");
            created.setType(BudgetType.ALLOWANCE);
            created.setOwnerMember(allowanceMember);
            created.setMonthlyLimit(new BigDecimal("400.00"));
            created.setCreatedInMonth(currentReferenceMonth());
            created.setActive(true);
        });
        persistTransaction(created -> {
            created.setType(TransactionType.EXPENSE);
            created.setOwnershipType(OwnershipType.SHARED);
            created.setSourceType(TransactionSourceType.MANUAL);
            created.setDescription("Market");
            created.setAmount(new BigDecimal("150.00"));
            created.setConvertedAmount(new BigDecimal("150.00"));
            created.setTransactionDate(LocalDate.of(2026, 6, 10));
            created.setReferenceMonth(currentReferenceMonth());
            created.setAccount(account);
            created.setCategory(groceries);
        });
        persistTransaction(created -> {
            created.setType(TransactionType.EXPENSE);
            created.setOwnershipType(OwnershipType.INDIVIDUAL);
            created.setSourceType(TransactionSourceType.MANUAL);
            created.setDescription("Ride app");
            created.setAmount(new BigDecimal("45.00"));
            created.setConvertedAmount(new BigDecimal("45.00"));
            created.setTransactionDate(LocalDate.of(2026, 6, 11));
            created.setReferenceMonth(currentReferenceMonth());
            created.setAccount(account);
            created.setCategory(transport);
            created.setMember(allowanceMember);
        });
        return new BudgetScenario(
                regularUser, allowanceMember, groceries, transport, account, globalBudget, allowanceBudget);
    }

    public DashboardScenario createDashboardScenario() {
        var regularUser = ensureRegularUser();
        var allowanceMember = persistAllowanceMember("Karol", "karol-dashboard@bolso-em-dia.local", "karol123456");
        var groceries = persistCategory(category -> {
            category.setName("Groceries");
            category.setCreatedInMonth(currentReferenceMonth());
        });
        var salary = persistCategory(category -> {
            category.setName("Salary");
            category.setCreatedInMonth(currentReferenceMonth());
        });
        var transport = persistCategory(category -> {
            category.setName("Transport");
            category.setCreatedInMonth(currentReferenceMonth());
        });
        var account = persistAccount(created -> {
            created.setName("Main Checking");
            created.setCreatedInMonth(currentReferenceMonth());
        });
        persistBudget(created -> {
            created.setName("Family Essentials");
            created.setType(BudgetType.GLOBAL);
            created.setMonthlyLimit(new BigDecimal("1000.00"));
            created.setCreatedInMonth(currentReferenceMonth());
            created.setActive(true);
            created.setCategories(new LinkedHashSet<>(List.of(groceries, transport)));
        });
        persistBudget(created -> {
            created.setName("Karol Allowance");
            created.setType(BudgetType.ALLOWANCE);
            created.setOwnerMember(allowanceMember);
            created.setMonthlyLimit(new BigDecimal("400.00"));
            created.setCreatedInMonth(currentReferenceMonth());
            created.setActive(true);
        });
        persistTransaction(created -> {
            created.setType(TransactionType.INCOME);
            created.setOwnershipType(OwnershipType.SHARED);
            created.setSourceType(TransactionSourceType.MANUAL);
            created.setDescription("June salary");
            created.setAmount(new BigDecimal("5000.00"));
            created.setConvertedAmount(new BigDecimal("5000.00"));
            created.setTransactionDate(LocalDate.of(2026, 6, 5));
            created.setReferenceMonth(currentReferenceMonth());
            created.setAccount(account);
            created.setCategory(salary);
        });
        persistTransaction(created -> {
            created.setType(TransactionType.EXPENSE);
            created.setOwnershipType(OwnershipType.SHARED);
            created.setSourceType(TransactionSourceType.MANUAL);
            created.setDescription("Market");
            created.setAmount(new BigDecimal("150.00"));
            created.setConvertedAmount(new BigDecimal("150.00"));
            created.setTransactionDate(LocalDate.of(2026, 6, 10));
            created.setReferenceMonth(currentReferenceMonth());
            created.setAccount(account);
            created.setCategory(groceries);
        });
        persistTransaction(created -> {
            created.setType(TransactionType.EXPENSE);
            created.setOwnershipType(OwnershipType.INDIVIDUAL);
            created.setSourceType(TransactionSourceType.MANUAL);
            created.setDescription("Ride app");
            created.setAmount(new BigDecimal("45.00"));
            created.setConvertedAmount(new BigDecimal("45.00"));
            created.setTransactionDate(LocalDate.of(2026, 6, 11));
            created.setReferenceMonth(currentReferenceMonth());
            created.setAccount(account);
            created.setCategory(transport);
            created.setMember(allowanceMember);
        });
        return new DashboardScenario(regularUser, allowanceMember, groceries, salary, transport, account);
    }

    public record TransactionScenario(
            FamilyMember regularUser,
            FamilyMember allowanceMember,
            Category groceries,
            Category transport,
            Account account,
            Transaction sharedTransaction,
            Transaction transportTransaction) {}

    public record BudgetScenario(
            FamilyMember regularUser,
            FamilyMember allowanceMember,
            Category groceries,
            Category transport,
            Account account,
            Budget globalBudget,
            Budget allowanceBudget) {}

    public record DashboardScenario(
            FamilyMember regularUser,
            FamilyMember allowanceMember,
            Category groceries,
            Category salary,
            Category transport,
            Account account) {}
}
