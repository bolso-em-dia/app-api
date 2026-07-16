package com.mymoney.api.budget.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mymoney.api.AuthenticatedIntegrationTestSupport;
import com.mymoney.api.account.Account;
import com.mymoney.api.account.AccountRepository;
import com.mymoney.api.account.AccountType;
import com.mymoney.api.account.CurrencyType;
import com.mymoney.api.auth.api.JsonTestUtils;
import com.mymoney.api.budget.Budget;
import com.mymoney.api.budget.BudgetRepository;
import com.mymoney.api.category.Category;
import com.mymoney.api.exchangerate.ExchangeRate;
import com.mymoney.api.exchangerate.ExchangeRateRepository;
import com.mymoney.api.fixedexpense.FixedExpenseTemplate;
import com.mymoney.api.fixedexpense.FixedExpenseTemplateRepository;
import com.mymoney.api.member.FamilyMember;
import com.mymoney.api.support.AccountTestFactory;
import com.mymoney.api.support.TransactionTestFactory;
import com.mymoney.api.transaction.OwnershipType;
import com.mymoney.api.transaction.TransactionRepository;
import com.mymoney.api.transaction.TransactionSourceType;
import com.mymoney.api.transaction.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class BudgetControllerIntegrationTest extends AuthenticatedIntegrationTestSupport {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private FixedExpenseTemplateRepository fixedExpenseTemplateRepository;

    @Autowired
    private ExchangeRateRepository exchangeRateRepository;

    @Autowired
    private BudgetRepository budgetRepository;

    private String adminToken;
    private String userToken;
    private FamilyMember allowanceMember;
    private Category groceries;
    private Category transport;
    private Account account;
    private Budget globalBudget;
    private Budget allowanceBudget;

    @BeforeEach
    void setUp() throws Exception {
        fixtures().ensureRegularUser();
        allowanceMember = budgetFixtures().createAllowanceMember();
        groceries = budgetFixtures().createGroceriesCategory();
        transport = budgetFixtures().createTransportCategory();
        account = budgetFixtures().createMainCheckingAccount();
        globalBudget = budgetFixtures().createGlobalBudget(groceries, transport);
        allowanceBudget = budgetFixtures().createAllowanceBudget(allowanceMember);
        budgetFixtures().createSharedGroceriesTransaction(account, groceries);
        budgetFixtures().createAllowanceTransportTransaction(account, transport, allowanceMember);

        adminToken = loginAsAdmin();
        userToken = loginAsUser();
    }

    @Test
    void listDetailTransactionsAndBreakdownWork() throws Exception {
        mockMvc.perform(get("/api/budgets")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("referenceMonth", "2026-06-01")
                        .param("search", "allowance")
                        .param("status", "ACTIVE")
                        .param("type", "ALLOWANCE")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].name").value("Karol Allowance"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));

        mockMvc.perform(get("/api/budgets/" + globalBudget.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .param("referenceMonth", "2026-06-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.consumedAmount").value(150.0))
                .andExpect(jsonPath("$.transactions.length()").value(1));

        mockMvc.perform(get("/api/budgets/" + allowanceBudget.getId() + "/transactions")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("referenceMonth", "2026-06-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].memberName").value("Karol"));

        mockMvc.perform(get("/api/budgets/" + allowanceBudget.getId() + "/category-breakdown")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("referenceMonth", "2026-06-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].categoryName").value("Transport"))
                .andExpect(jsonPath("$[0].amount").value(45.0));
    }

    @Test
    void budgetBusinessValidationRulesWork() throws Exception {
        mockMvc.perform(
                        post("/api/budgets")
                                .header("Authorization", "Bearer " + adminToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {
                                  "name": "Invalid Global",
                                  "type": "GLOBAL",
                                  "monthlyLimit": 150.00
                                }
                                """))
                .andExpect(status().isUnprocessableEntity());

        mockMvc.perform(patch("/api/budgets/" + globalBudget.getId() + "/archive")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("referenceMonth", "2026-05-01")
                        .content("{}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void budgetTypeSwitchClearsIncompatibleFields() throws Exception {
        mockMvc.perform(put("/api/budgets/" + allowanceBudget.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {
                                  "name": "Allowance To Global",
                                  "type": "GLOBAL",
                                  "categoryIds": ["%s"],
                                  "monthlyLimit": 500.00
                                }
                                """
                                        .formatted(groceries.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("GLOBAL"))
                .andExpect(jsonPath("$.ownerMemberId").value(nullValue()))
                .andExpect(jsonPath("$.categories.length()").value(1));

        mockMvc.perform(put("/api/budgets/" + globalBudget.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {
                                  "name": "Global To Allowance",
                                  "type": "ALLOWANCE",
                                  "ownerMemberId": "%s",
                                  "monthlyLimit": 320.00
                                }
                                """
                                        .formatted(allowanceMember.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("ALLOWANCE"))
                .andExpect(jsonPath("$.ownerMemberId")
                        .value(allowanceMember.getId().toString()))
                .andExpect(jsonPath("$.categories.length()").value(0));
    }

    @Test
    void categoryBreakdownUsesConvertedAmountForUsdTransactions() throws Exception {
        var usdRate = ExchangeRate.builder()
                .currency("USD")
                .rate(new BigDecimal("5.20"))
                .fetchedAt(OffsetDateTime.parse("2026-06-15T12:00:00Z"))
                .build();
        exchangeRateRepository.save(usdRate);

        var usdAccount = accountRepository.save(AccountTestFactory.create(created -> {
            created.setName("USD Wallet");
            created.setType(AccountType.CHECKING);
            created.setCurrency(CurrencyType.USD);
            created.setCreatedInMonth(LocalDate.of(2026, 6, 1));
        }));

        transactionRepository.save(TransactionTestFactory.create(created -> {
            created.setType(TransactionType.EXPENSE);
            created.setOwnershipType(OwnershipType.SHARED);
            created.setSourceType(TransactionSourceType.MANUAL);
            created.setDescription("Imported groceries");
            created.setAmount(new BigDecimal("100.00"));
            created.setConvertedAmount(new BigDecimal("520.00"));
            created.setExchangeRate(new BigDecimal("5.20"));
            created.setCurrency("USD");
            created.setTransactionDate(LocalDate.of(2026, 6, 15));
            created.setReferenceMonth(LocalDate.of(2026, 6, 1));
            created.setAccount(usdAccount);
            created.setCategory(groceries);
        }));

        mockMvc.perform(get("/api/budgets/" + globalBudget.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .param("referenceMonth", "2026-06-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.consumedAmount").value(670.0));

        mockMvc.perform(get("/api/budgets/" + globalBudget.getId() + "/category-breakdown")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("referenceMonth", "2026-06-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.categoryName=='Groceries')].amount").value(670.0));
    }

    @Test
    void createUpdateArchiveAndAuthorizationWork() throws Exception {
        mockMvc.perform(post("/api/budgets")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {
                                  "name": "New Allowance",
                                  "type": "ALLOWANCE",
                                  "ownerMemberId": "%s",
                                  "monthlyLimit": 300.00
                                }
                                """
                                        .formatted(allowanceMember.getId())))
                .andExpect(status().isConflict());

        var anotherAllowanceMember = budgetFixtures().createAllowanceMember();
        budgetFixtures().createAllowanceBudget(anotherAllowanceMember);

        mockMvc.perform(put("/api/budgets/" + globalBudget.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {
                                  "name": "Global To Existing Allowance Owner",
                                  "type": "ALLOWANCE",
                                  "ownerMemberId": "%s",
                                  "monthlyLimit": 300.00
                                }
                                """
                                        .formatted(anotherAllowanceMember.getId())))
                .andExpect(status().isConflict());

        MvcResult createResult = mockMvc.perform(post("/api/budgets")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {
                                  "name": "Transport Budget",
                                  "type": "GLOBAL",
                                  "categoryIds": ["%s"],
                                  "monthlyLimit": 200.00
                                }
                                """
                                        .formatted(transport.getId())))
                .andExpect(status().isCreated())
                .andReturn();

        String createdId =
                JsonTestUtils.extractJsonValue(createResult.getResponse().getContentAsString(), "id");

        mockMvc.perform(put("/api/budgets/" + createdId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {
                                  "name": "Transport Budget Updated",
                                  "type": "GLOBAL",
                                  "categoryIds": ["%s"],
                                  "monthlyLimit": 250.00
                                }
                                """
                                        .formatted(transport.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Transport Budget Updated"));

        mockMvc.perform(patch("/api/budgets/" + createdId + "/archive")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("referenceMonth", "2026-07-01")
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(get("/api/budgets")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("referenceMonth", "2026-07-01")
                        .param("status", "ARCHIVED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[?(@.name=='Transport Budget Updated')]")
                        .isNotEmpty());

        mockMvc.perform(get("/api/budgets")
                        .header("Authorization", "Bearer " + userToken)
                        .param("referenceMonth", "2026-06-01"))
                .andExpect(status().isForbidden());
    }

    @Test
    void budgetWritesShouldPopulateAuditFields() throws Exception {
        var createResult = mockMvc.perform(post("/api/budgets")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {
                                  "name": "Audited Budget",
                                  "type": "GLOBAL",
                                  "categoryIds": ["%s"],
                                  "monthlyLimit": 220.00
                                }
                                """
                                        .formatted(transport.getId())))
                .andExpect(status().isCreated())
                .andReturn();

        var createdId =
                JsonTestUtils.extractJsonValue(createResult.getResponse().getContentAsString(), "id");
        var created =
                budgetRepository.findById(java.util.UUID.fromString(createdId)).orElseThrow();
        assertThat(created.getCreatedBy()).isEqualTo(adminMemberId());
        assertThat(created.getUpdatedBy()).isEqualTo(adminMemberId());

        mockMvc.perform(put("/api/budgets/" + createdId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {
                                  "name": "Audited Budget Updated",
                                  "type": "GLOBAL",
                                  "categoryIds": ["%s"],
                                  "monthlyLimit": 230.00
                                }
                                """
                                        .formatted(transport.getId())))
                .andExpect(status().isOk());

        var updated =
                budgetRepository.findById(java.util.UUID.fromString(createdId)).orElseThrow();
        assertThat(updated.getUpdatedBy()).isEqualTo(adminMemberId());

        mockMvc.perform(patch("/api/budgets/" + createdId + "/archive")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("referenceMonth", "2026-07-01"))
                .andExpect(status().isOk());

        var archived =
                budgetRepository.findById(java.util.UUID.fromString(createdId)).orElseThrow();
        assertThat(archived.getUpdatedBy()).isEqualTo(adminMemberId());
    }

    @Test
    void futureMonthBudgetConsumesProjectedFixedExpense() throws Exception {
        LocalDate currentReferenceMonth = currentReferenceMonth();
        LocalDate futureReferenceMonth = currentReferenceMonth.plusMonths(4);

        FixedExpenseTemplate projectedExpense = FixedExpenseTemplate.builder()
                .name("Projected Market")
                .type(TransactionType.EXPENSE)
                .amount(new BigDecimal("210.00"))
                .convertedAmount(new BigDecimal("210.00"))
                .currency(com.mymoney.api.account.CurrencyType.BRL)
                .category(groceries)
                .account(account)
                .dueDay((short) 14)
                .createdInMonth(currentReferenceMonth.minusMonths(1))
                .active(true)
                .build();
        fixedExpenseTemplateRepository.save(projectedExpense);

        // Materialize transactions for the future month
        mockMvc.perform(post("/api/transactions/materialize")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("referenceMonth", futureReferenceMonth.toString()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/budgets/" + globalBudget.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .param("referenceMonth", futureReferenceMonth.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.consumedAmount").value(210.0))
                .andExpect(jsonPath("$.transactions.length()").value(1))
                .andExpect(jsonPath("$.transactions[0].sourceType").value("FIXED_EXPENSE"))
                .andExpect(jsonPath("$.transactions[0].projected").value(false));
    }

    @Test
    void listBudgetsRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/budgets").param("referenceMonth", "2026-06-01"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createBudgetRequiresAuthentication() throws Exception {
        mockMvc.perform(
                        post("/api/budgets")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {
                                  "name": "Test Budget",
                                  "type": "GLOBAL",
                                  "monthlyLimit": 500.0,
                                  "categoryIds": []
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getBudgetReturns404ForNonExistentId() throws Exception {
        mockMvc.perform(get("/api/budgets/00000000-0000-0000-0000-000000000000")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("referenceMonth", "2026-06-01"))
                .andExpect(status().isNotFound());
    }

    @Test
    void archiveBudgetReturns404ForNonExistentId() throws Exception {
        mockMvc.perform(patch("/api/budgets/00000000-0000-0000-0000-000000000000/archive")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("referenceMonth", "2026-07-01"))
                .andExpect(status().isNotFound());
    }

    private java.util.UUID adminMemberId() {
        return fixtures().ensureAdminCanUseProtectedApis().getId();
    }
}
