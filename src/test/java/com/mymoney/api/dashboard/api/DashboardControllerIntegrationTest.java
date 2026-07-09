package com.mymoney.api.dashboard.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mymoney.api.AuthenticatedIntegrationTestSupport;
import com.mymoney.api.account.Account;
import com.mymoney.api.account.AccountRepository;
import com.mymoney.api.account.AccountType;
import com.mymoney.api.budget.BudgetModel;
import com.mymoney.api.budget.BudgetModelRepository;
import com.mymoney.api.budget.BudgetType;
import com.mymoney.api.category.Category;
import com.mymoney.api.category.CategoryRepository;
import com.mymoney.api.fixedexpense.FixedExpenseTemplate;
import com.mymoney.api.fixedexpense.FixedExpenseTemplateRepository;
import com.mymoney.api.member.FamilyMember;
import com.mymoney.api.member.FamilyMemberRepository;
import com.mymoney.api.member.FamilyRole;
import com.mymoney.api.transaction.OwnershipType;
import com.mymoney.api.transaction.Transaction;
import com.mymoney.api.transaction.TransactionRepository;
import com.mymoney.api.transaction.TransactionSourceType;
import com.mymoney.api.transaction.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.LinkedHashSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DashboardControllerIntegrationTest extends AuthenticatedIntegrationTestSupport {

    @Autowired
    private FamilyMemberRepository familyMemberRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private BudgetModelRepository budgetModelRepository;

    @Autowired
    private FixedExpenseTemplateRepository fixedExpenseTemplateRepository;

    private String adminToken;
    private String userToken;

    @BeforeEach
    void setUp() throws Exception {
        FamilyMember regularUser = familyMemberRepository
                .findByEmailIgnoreCase("user@bolso-em-dia.local")
                .orElseGet(FamilyMember::new);
        regularUser.setName("Regular User");
        regularUser.setEmail("user@bolso-em-dia.local");
        regularUser.setPasswordHash(passwordEncoder.encode("user123456"));
        regularUser.setRole(FamilyRole.USER);
        regularUser.setActive(true);
        regularUser.setAllowanceEnabled(false);
        familyMemberRepository.save(regularUser);

        FamilyMember allowanceMember = new FamilyMember();
        allowanceMember.setName("Karol");
        allowanceMember.setEmail("karol-dashboard@bolso-em-dia.local");
        allowanceMember.setPasswordHash(passwordEncoder.encode("karol123456"));
        allowanceMember.setRole(FamilyRole.USER);
        allowanceMember.setActive(true);
        allowanceMember.setAllowanceEnabled(true);
        allowanceMember = familyMemberRepository.save(allowanceMember);

        Category groceries = new Category();
        groceries.setName("Groceries");
        groceries.setCreatedInMonth(LocalDate.of(2026, 6, 1));
        groceries = categoryRepository.save(groceries);

        Category salary = new Category();
        salary.setName("Salary");
        salary.setCreatedInMonth(LocalDate.of(2026, 6, 1));
        salary = categoryRepository.save(salary);

        Category transport = new Category();
        transport.setName("Transport");
        transport.setCreatedInMonth(LocalDate.of(2026, 6, 1));
        transport = categoryRepository.save(transport);

        Account account = new Account();
        account.setName("Main Checking");
        account.setType(AccountType.CHECKING);
        account.setCreatedInMonth(LocalDate.of(2026, 6, 1));
        account = accountRepository.save(account);

        BudgetModel familyBudget = new BudgetModel();
        familyBudget.setName("Family Essentials");
        familyBudget.setType(BudgetType.GLOBAL);
        familyBudget.setMonthlyLimit(new BigDecimal("1000.00"));
        familyBudget.setCreatedInMonth(LocalDate.of(2026, 6, 1));
        familyBudget.setActive(true);
        familyBudget.setCategories(new LinkedHashSet<>(java.util.List.of(groceries, transport)));
        budgetModelRepository.save(familyBudget);

        BudgetModel allowanceBudget = new BudgetModel();
        allowanceBudget.setName("Karol Allowance");
        allowanceBudget.setType(BudgetType.ALLOWANCE);
        allowanceBudget.setOwnerMember(allowanceMember);
        allowanceBudget.setMonthlyLimit(new BigDecimal("400.00"));
        allowanceBudget.setCreatedInMonth(LocalDate.of(2026, 6, 1));
        allowanceBudget.setActive(true);
        budgetModelRepository.save(allowanceBudget);

        transactionRepository.save(createTransaction(
                TransactionType.INCOME,
                OwnershipType.SHARED,
                "June salary",
                new BigDecimal("5000.00"),
                LocalDate.of(2026, 6, 5),
                LocalDate.of(2026, 6, 1),
                account,
                salary,
                null));
        transactionRepository.save(createTransaction(
                TransactionType.EXPENSE,
                OwnershipType.SHARED,
                "Market",
                new BigDecimal("150.00"),
                LocalDate.of(2026, 6, 10),
                LocalDate.of(2026, 6, 1),
                account,
                groceries,
                null));
        transactionRepository.save(createTransaction(
                TransactionType.EXPENSE,
                OwnershipType.INDIVIDUAL,
                "Ride app",
                new BigDecimal("45.00"),
                LocalDate.of(2026, 6, 11),
                LocalDate.of(2026, 6, 1),
                account,
                transport,
                allowanceMember));

        adminToken = loginAsAdmin();
        userToken = loginAsUser();
    }

    @Test
    void dashboardRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/dashboard").param("referenceMonth", "2026-06-01"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void dashboardReturnsZeroTotalsForMonthWithoutTransactions() throws Exception {
        mockMvc.perform(get("/api/dashboard")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("referenceMonth", "2027-01-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.totalIncome").value(0.0))
                .andExpect(jsonPath("$.summary.totalExpense").value(0.0))
                .andExpect(jsonPath("$.summary.balance").value(0.0))
                .andExpect(jsonPath("$.recentTransactions.length()").value(0))
                .andExpect(jsonPath("$.categoryBreakdown.length()").value(0));
    }

    @Test
    void dashboardReturnsAggregatedDataForAdminAndUser() throws Exception {
        mockMvc.perform(get("/api/dashboard")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("referenceMonth", "2026-06-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.totalIncome").value(5000.0))
                .andExpect(jsonPath("$.summary.totalExpense").value(195.0))
                .andExpect(jsonPath("$.summary.balance").value(4805.0))
                .andExpect(jsonPath("$.summary.availableBalance").value(3600.0))
                .andExpect(jsonPath("$.summary.reservedBudgetAmount").value(1205.0))
                .andExpect(jsonPath("$.budgets.length()").value(2))
                .andExpect(jsonPath("$.recentTransactions[0].description").value("Ride app"))
                .andExpect(jsonPath("$.categoryBreakdown[0].categoryName").value("Groceries"));

        mockMvc.perform(get("/api/dashboard")
                        .header("Authorization", "Bearer " + userToken)
                        .param("referenceMonth", "2026-06-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.totalExpense").value(195.0));
    }

    @Test
    void futureMonthDashboardIncludesProjectedFixedTransactions() throws Exception {
        LocalDate currentReferenceMonth = YearMonth.now().atDay(1);
        LocalDate futureReferenceMonth = currentReferenceMonth.plusMonths(1);

        Category groceries =
                categoryRepository.findByNormalizedName("groceries").orElseThrow();
        Category salary = categoryRepository.findByNormalizedName("salary").orElseThrow();
        Account account =
                accountRepository.findByNormalizedName("main checking").orElseThrow();

        FixedExpenseTemplate projectedExpense = new FixedExpenseTemplate();
        projectedExpense.setName("Projected Rent");
        projectedExpense.setType(TransactionType.EXPENSE);
        projectedExpense.setAmount(new BigDecimal("200.00"));
        projectedExpense.setCategory(groceries);
        projectedExpense.setAccount(account);
        projectedExpense.setDueDay((short) 8);
        projectedExpense.setCreatedInMonth(currentReferenceMonth.minusMonths(1));
        projectedExpense.setActive(true);
        fixedExpenseTemplateRepository.save(projectedExpense);

        FixedExpenseTemplate projectedIncome = new FixedExpenseTemplate();
        projectedIncome.setName("Projected Salary");
        projectedIncome.setType(TransactionType.INCOME);
        projectedIncome.setAmount(new BigDecimal("1000.00"));
        projectedIncome.setCategory(salary);
        projectedIncome.setAccount(account);
        projectedIncome.setDueDay((short) 6);
        projectedIncome.setCreatedInMonth(currentReferenceMonth.minusMonths(1));
        projectedIncome.setActive(true);
        fixedExpenseTemplateRepository.save(projectedIncome);

        mockMvc.perform(get("/api/dashboard")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("referenceMonth", futureReferenceMonth.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.totalIncome").value(1000.0))
                .andExpect(jsonPath("$.summary.totalExpense").value(200.0))
                .andExpect(jsonPath("$.summary.balance").value(800.0))
                .andExpect(jsonPath("$.recentTransactions[0].projected").value(true));
    }

    private Transaction createTransaction(
            TransactionType type,
            OwnershipType ownershipType,
            String description,
            BigDecimal amount,
            LocalDate transactionDate,
            LocalDate referenceMonth,
            Account account,
            Category category,
            FamilyMember member) {
        Transaction transaction = new Transaction();
        transaction.setType(type);
        transaction.setOwnershipType(ownershipType);
        transaction.setSourceType(TransactionSourceType.MANUAL);
        transaction.setDescription(description);
        transaction.setAmount(amount);
        transaction.setTransactionDate(transactionDate);
        transaction.setReferenceMonth(referenceMonth);
        transaction.setAccount(account);
        transaction.setCategory(category);
        transaction.setMember(member);
        return transaction;
    }
}
