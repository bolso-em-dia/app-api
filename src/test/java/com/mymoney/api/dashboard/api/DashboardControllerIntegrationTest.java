package com.mymoney.api.dashboard.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mymoney.api.AuthenticatedIntegrationTestSupport;
import com.mymoney.api.account.Account;
import com.mymoney.api.account.AccountRepository;
import com.mymoney.api.category.Category;
import com.mymoney.api.category.CategoryRepository;
import com.mymoney.api.fixedexpense.FixedExpenseTemplate;
import com.mymoney.api.fixedexpense.FixedExpenseTemplateRepository;
import com.mymoney.api.transaction.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DashboardControllerIntegrationTest extends AuthenticatedIntegrationTestSupport {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private FixedExpenseTemplateRepository fixedExpenseTemplateRepository;

    private String adminToken;
    private String userToken;

    @BeforeEach
    void setUp() throws Exception {
        fixtures().ensureRegularUser();
        var allowanceMember = dashboardFixtures().createAllowanceMember();
        var groceries = dashboardFixtures().createGroceriesCategory();
        var salary = dashboardFixtures().createSalaryCategory();
        var transport = dashboardFixtures().createTransportCategory();
        var account = dashboardFixtures().createMainCheckingAccount();
        dashboardFixtures().createFamilyEssentialsBudget(groceries, transport);
        dashboardFixtures().createAllowanceBudget(allowanceMember);
        dashboardFixtures().createSalaryTransaction(account, salary);
        dashboardFixtures().createMarketTransaction(account, groceries);
        dashboardFixtures().createAllowanceRideAppTransaction(account, transport, allowanceMember);

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
        LocalDate currentReferenceMonth = currentReferenceMonth();
        LocalDate futureReferenceMonth = currentReferenceMonth.plusMonths(4);

        Category groceries =
                categoryRepository.findByNormalizedName("groceries").orElseThrow();
        Category salary = categoryRepository.findByNormalizedName("salary").orElseThrow();
        Account account =
                accountRepository.findByNormalizedName("main checking").orElseThrow();

        FixedExpenseTemplate projectedExpense = FixedExpenseTemplate.builder()
                .name("Projected Rent")
                .type(TransactionType.EXPENSE)
                .amount(new BigDecimal("200.00"))
                .category(groceries)
                .account(account)
                .dueDay((short) 8)
                .createdInMonth(currentReferenceMonth.minusMonths(1))
                .active(true)
                .build();
        fixedExpenseTemplateRepository.save(projectedExpense);

        FixedExpenseTemplate projectedIncome = FixedExpenseTemplate.builder()
                .name("Projected Salary")
                .type(TransactionType.INCOME)
                .amount(new BigDecimal("1000.00"))
                .category(salary)
                .account(account)
                .dueDay((short) 6)
                .createdInMonth(currentReferenceMonth.minusMonths(1))
                .active(true)
                .build();
        fixedExpenseTemplateRepository.save(projectedIncome);

        // Materialize transactions for the future month
        mockMvc.perform(post("/api/transactions/materialize")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("referenceMonth", futureReferenceMonth.toString()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/dashboard")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("referenceMonth", futureReferenceMonth.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.totalIncome").value(1000.0))
                .andExpect(jsonPath("$.summary.totalExpense").value(200.0))
                .andExpect(jsonPath("$.summary.balance").value(800.0))
                .andExpect(jsonPath("$.recentTransactions[0].projected").value(false));
    }

    @Test
    void dashboardReturns401WithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/dashboard").param("referenceMonth", "2026-06-01"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void dashboardWithInvalidReferenceMonthReturns400() throws Exception {
        mockMvc.perform(get("/api/dashboard")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("referenceMonth", "not-a-date"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void dashboardIncludesProjectedFlagInRecentTransactions() throws Exception {
        LocalDate futureReferenceMonth = currentReferenceMonth().plusMonths(4);

        Category groceries =
                categoryRepository.findByNormalizedName("groceries").orElseThrow();
        Account account =
                accountRepository.findByNormalizedName("main checking").orElseThrow();

        FixedExpenseTemplate projectedExpense = FixedExpenseTemplate.builder()
                .name("Projected Rent")
                .type(TransactionType.EXPENSE)
                .amount(new BigDecimal("200.00"))
                .category(groceries)
                .account(account)
                .dueDay((short) 8)
                .createdInMonth(currentReferenceMonth().minusMonths(1))
                .active(true)
                .build();
        fixedExpenseTemplateRepository.save(projectedExpense);

        // Materialize transactions for the future month
        mockMvc.perform(post("/api/transactions/materialize")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("referenceMonth", futureReferenceMonth.toString()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/dashboard")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("referenceMonth", futureReferenceMonth.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recentTransactions[0].projected").value(false));
    }

    @Test
    void dashboardIncludesReservedBudgetAmountWhenBudgetsExist() throws Exception {
        mockMvc.perform(get("/api/dashboard")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("referenceMonth", "2026-06-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.reservedBudgetAmount").isNumber())
                .andExpect(jsonPath("$.summary.totalExpense").isNumber());
    }

    @Test
    void recentTransactionsIncludeOriginalAmountWhenPresent() throws Exception {
        mockMvc.perform(get("/api/dashboard")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("referenceMonth", "2026-06-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recentTransactions").isArray());
    }

    @Test
    void dashboardAggregatesConvertedAmountsCorrectly() throws Exception {
        mockMvc.perform(get("/api/dashboard")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("referenceMonth", "2026-06-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.totalIncome").isNumber())
                .andExpect(jsonPath("$.summary.balance").isNumber());
    }
}
