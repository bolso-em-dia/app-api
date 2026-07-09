package com.mymoney.api.budget.api;

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
import com.mymoney.api.auth.api.JsonTestUtils;
import com.mymoney.api.budget.BudgetModel;
import com.mymoney.api.budget.BudgetModelRepository;
import com.mymoney.api.budget.BudgetType;
import com.mymoney.api.category.Category;
import com.mymoney.api.category.CategoryRepository;
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
import java.util.LinkedHashSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class BudgetControllerIntegrationTest extends AuthenticatedIntegrationTestSupport {

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

    private String adminToken;
    private String userToken;
    private FamilyMember regularUser;
    private FamilyMember allowanceMember;
    private Category groceries;
    private Category transport;
    private Account account;
    private BudgetModel globalBudget;
    private BudgetModel allowanceBudget;

    @BeforeEach
    void setUp() throws Exception {
        regularUser = familyMemberRepository
                .findByEmailIgnoreCase("user@bolso-em-dia.local")
                .orElseGet(FamilyMember::new);
        regularUser.setName("Regular User");
        regularUser.setEmail("user@bolso-em-dia.local");
        regularUser.setPasswordHash(passwordEncoder.encode("user123456"));
        regularUser.setRole(FamilyRole.USER);
        regularUser.setActive(true);
        regularUser.setAllowanceEnabled(false);
        regularUser = familyMemberRepository.save(regularUser);

        allowanceMember = new FamilyMember();
        allowanceMember.setName("Karol");
        allowanceMember.setEmail("karol-budget@bolso-em-dia.local");
        allowanceMember.setPasswordHash(passwordEncoder.encode("karol123456"));
        allowanceMember.setRole(FamilyRole.USER);
        allowanceMember.setActive(true);
        allowanceMember.setAllowanceEnabled(true);
        allowanceMember = familyMemberRepository.save(allowanceMember);

        groceries = new Category();
        groceries.setName("Groceries");
        groceries.setCreatedInMonth(LocalDate.of(2026, 6, 1));
        groceries = categoryRepository.save(groceries);

        transport = new Category();
        transport.setName("Transport");
        transport.setCreatedInMonth(LocalDate.of(2026, 6, 1));
        transport = categoryRepository.save(transport);

        account = new Account();
        account.setName("Main Checking");
        account.setType(AccountType.CHECKING);
        account.setCreatedInMonth(LocalDate.of(2026, 6, 1));
        account = accountRepository.save(account);

        globalBudget = new BudgetModel();
        globalBudget.setName("Family Essentials");
        globalBudget.setType(BudgetType.GLOBAL);
        globalBudget.setMonthlyLimit(new BigDecimal("1000.00"));
        globalBudget.setCreatedInMonth(LocalDate.of(2026, 6, 1));
        globalBudget.setActive(true);
        globalBudget.setCategories(new LinkedHashSet<>(java.util.List.of(groceries, transport)));
        globalBudget = budgetModelRepository.save(globalBudget);

        allowanceBudget = new BudgetModel();
        allowanceBudget.setName("Karol Allowance");
        allowanceBudget.setType(BudgetType.ALLOWANCE);
        allowanceBudget.setOwnerMember(allowanceMember);
        allowanceBudget.setMonthlyLimit(new BigDecimal("400.00"));
        allowanceBudget.setCreatedInMonth(LocalDate.of(2026, 6, 1));
        allowanceBudget.setActive(true);
        allowanceBudget = budgetModelRepository.save(allowanceBudget);

        Transaction sharedGroceries = new Transaction();
        sharedGroceries.setType(TransactionType.EXPENSE);
        sharedGroceries.setOwnershipType(OwnershipType.SHARED);
        sharedGroceries.setSourceType(TransactionSourceType.MANUAL);
        sharedGroceries.setDescription("Market");
        sharedGroceries.setAmount(new BigDecimal("150.00"));
        sharedGroceries.setTransactionDate(LocalDate.of(2026, 6, 10));
        sharedGroceries.setReferenceMonth(LocalDate.of(2026, 6, 1));
        sharedGroceries.setAccount(account);
        sharedGroceries.setCategory(groceries);
        transactionRepository.save(sharedGroceries);

        Transaction individualTransport = new Transaction();
        individualTransport.setType(TransactionType.EXPENSE);
        individualTransport.setOwnershipType(OwnershipType.INDIVIDUAL);
        individualTransport.setSourceType(TransactionSourceType.MANUAL);
        individualTransport.setDescription("Ride app");
        individualTransport.setAmount(new BigDecimal("45.00"));
        individualTransport.setTransactionDate(LocalDate.of(2026, 6, 11));
        individualTransport.setReferenceMonth(LocalDate.of(2026, 6, 1));
        individualTransport.setAccount(account);
        individualTransport.setCategory(transport);
        individualTransport.setMember(allowanceMember);
        transactionRepository.save(individualTransport);

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

        mockMvc.perform(post("/api/budgets")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {
                                  "name": "Invalid Allowance",
                                  "type": "ALLOWANCE",
                                  "ownerMemberId": "%s",
                                  "monthlyLimit": 150.00
                                }
                                """
                                        .formatted(regularUser.getId())))
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
}
