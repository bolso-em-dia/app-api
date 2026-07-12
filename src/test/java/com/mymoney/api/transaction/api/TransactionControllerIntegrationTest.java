package com.mymoney.api.transaction.api;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mymoney.api.AuthenticatedIntegrationTestSupport;
import com.mymoney.api.account.Account;
import com.mymoney.api.account.AccountRepository;
import com.mymoney.api.account.AccountType;
import com.mymoney.api.account.CurrencyType;
import com.mymoney.api.category.Category;
import com.mymoney.api.category.CategoryRepository;
import com.mymoney.api.exchangerate.ExchangeRate;
import com.mymoney.api.exchangerate.ExchangeRateRepository;
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
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.util.stream.IntStream;
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
class TransactionControllerIntegrationTest extends AuthenticatedIntegrationTestSupport {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

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
    private FixedExpenseTemplateRepository fixedExpenseTemplateRepository;

    @Autowired
    private ExchangeRateRepository exchangeRateRepository;

    private String adminToken;
    private String userToken;
    private FamilyMember allowanceMember;
    private Category category;
    private Category transportCategory;
    private Account account;
    private Transaction sharedTransaction;

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

        allowanceMember = new FamilyMember();
        allowanceMember.setName("Karol");
        allowanceMember.setEmail("karol@bolso-em-dia.local");
        allowanceMember.setPasswordHash(passwordEncoder.encode("karol123456"));
        allowanceMember.setRole(FamilyRole.USER);
        allowanceMember.setActive(true);
        allowanceMember.setAllowanceEnabled(true);
        allowanceMember = familyMemberRepository.save(allowanceMember);

        category = new Category();
        category.setName("Groceries");
        category.setCreatedInMonth(LocalDate.of(2026, 6, 1));
        category = categoryRepository.save(category);

        transportCategory = new Category();
        transportCategory.setName("Transport");
        transportCategory.setCreatedInMonth(LocalDate.of(2026, 6, 1));
        transportCategory = categoryRepository.save(transportCategory);

        account = new Account();
        account.setName("Main Checking");
        account.setType(AccountType.CHECKING);
        account.setCreatedInMonth(LocalDate.of(2026, 6, 1));
        account = accountRepository.save(account);

        sharedTransaction = new Transaction();
        sharedTransaction.setType(TransactionType.EXPENSE);
        sharedTransaction.setOwnershipType(OwnershipType.SHARED);
        sharedTransaction.setSourceType(TransactionSourceType.MANUAL);
        sharedTransaction.setDescription("Market");
        sharedTransaction.setAmount(new BigDecimal("150.00"));
        sharedTransaction.setTransactionDate(LocalDate.of(2026, 6, 10));
        sharedTransaction.setReferenceMonth(LocalDate.of(2026, 6, 1));
        sharedTransaction.setCategory(category);
        sharedTransaction.setAccount(account);
        sharedTransaction = transactionRepository.save(sharedTransaction);

        Transaction transportTransaction = new Transaction();
        transportTransaction.setType(TransactionType.EXPENSE);
        transportTransaction.setOwnershipType(OwnershipType.SHARED);
        transportTransaction.setSourceType(TransactionSourceType.MANUAL);
        transportTransaction.setDescription("Taxi");
        transportTransaction.setAmount(new BigDecimal("45.00"));
        transportTransaction.setTransactionDate(LocalDate.of(2026, 6, 11));
        transportTransaction.setReferenceMonth(LocalDate.of(2026, 6, 1));
        transportTransaction.setCategory(transportCategory);
        transportTransaction.setAccount(account);
        transactionRepository.save(transportTransaction);

        adminToken = loginAsAdmin();
        userToken = loginAsUser();
    }

    @Test
    void adminCanListCreateGetAndUpdateTransactions() throws Exception {
        mockMvc.perform(get("/api/transactions")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("referenceMonth", "2026-06-01")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].description").isArray())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.totalItems").value(2))
                .andExpect(jsonPath("$.totalPages").value(2));

        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {
                                  "type": "EXPENSE",
                                  "ownershipType": "SHARED",
                                  "description": "Water bill",
                                  "amount": 95.30,
                                  "transactionDate": "2026-06-12",
                                  "accountId": "%s",
                                  "categoryId": "%s",
                                  "installmentCount": 1
                                }
                                """
                                        .formatted(account.getId(), category.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].description").value("Water bill"))
                .andExpect(jsonPath("$[0].sourceType").value("MANUAL"));

        mockMvc.perform(get("/api/transactions/" + sharedTransaction.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Market"));

        mockMvc.perform(put("/api/transactions/" + sharedTransaction.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {
                                  "type": "EXPENSE",
                                  "ownershipType": "SHARED",
                                  "description": "Updated Market",
                                  "amount": 200.00,
                                  "transactionDate": "2026-06-15",
                                  "accountId": "%s",
                                  "categoryId": "%s"
                                }
                                """
                                        .formatted(account.getId(), category.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Updated Market"))
                .andExpect(jsonPath("$.amount").value(200.0));
    }

    @Test
    void createRejectsUnknownRequestFields() throws Exception {
        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {
                                  "type": "EXPENSE",
                                  "ownershipType": "SHARED",
                                  "description": "Water bill",
                                  "amount": 95.30,
                                  "transactionDate": "2026-06-12",
                                  "accountId": "%s",
                                  "categoryId": "%s",
                                  "referenceMonth": "2026-06-01"
                                }
                                """
                                        .formatted(account.getId(), category.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request body contains unsupported field: referenceMonth."));
    }

    @Test
    void installmentsAndIndividualAllowanceValidationWork() throws Exception {
        MvcResult installmentResult = mockMvc.perform(post("/api/transactions")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {
                                  "type": "EXPENSE",
                                  "ownershipType": "INDIVIDUAL",
                                  "description": "Personal shopping",
                                  "amount": 300.00,
                                  "transactionDate": "2026-06-20",
                                  "accountId": "%s",
                                  "categoryId": "%s",
                                  "memberId": "%s",
                                  "installmentCount": 3
                                }
                                """
                                        .formatted(account.getId(), category.getId(), allowanceMember.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].sourceType").value("INSTALLMENT"))
                .andExpect(jsonPath("$[0].amount").value(100.0))
                .andExpect(jsonPath("$[0].referenceMonth").value("2026-06-01"))
                .andExpect(jsonPath("$[1].amount").value(100.0))
                .andExpect(jsonPath("$[1].referenceMonth").value("2026-07-01"))
                .andExpect(jsonPath("$[2].amount").value(100.0))
                .andExpect(jsonPath("$[2].referenceMonth").value("2026-08-01"))
                .andExpect(jsonPath("$[2].installmentNumber").value(3))
                .andExpect(jsonPath("$[2].installmentTotal").value(3))
                .andReturn();

        String installmentGroupId = OBJECT_MAPPER
                .readTree(installmentResult.getResponse().getContentAsString())
                .get(0)
                .get("installmentGroupId")
                .asText();

        mockMvc.perform(get("/api/transactions")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("referenceMonth", "2026-07-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].installmentGroupId").value(installmentGroupId));

        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {
                                  "type": "EXPENSE",
                                  "ownershipType": "INDIVIDUAL",
                                  "description": "Blocked personal expense",
                                  "amount": 30.00,
                                  "transactionDate": "2026-06-20",
                                  "accountId": "%s",
                                  "categoryId": "%s"
                                }
                                """
                                        .formatted(account.getId(), category.getId())))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void unauthenticatedRequestsAreRejected() throws Exception {
        mockMvc.perform(get("/api/transactions").param("referenceMonth", "2026-06-01"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/transactions/descriptions").param("query", "Mar"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {
                                  "type": "EXPENSE",
                                  "ownershipType": "SHARED",
                                  "description": "Blocked write",
                                  "amount": 50.00,
                                  "transactionDate": "2026-06-12",
                                  "accountId": "%s",
                                  "categoryId": "%s"
                                }
                                """
                                        .formatted(account.getId(), category.getId())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void sharedTransactionsIgnoreMemberIdAndUpdateMovesReferenceMonth() throws Exception {
        MvcResult createResult = mockMvc.perform(post("/api/transactions")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {
                                  "type": "EXPENSE",
                                  "ownershipType": "SHARED",
                                  "description": "Shared With Member",
                                  "amount": 25.00,
                                  "transactionDate": "2026-06-13",
                                  "accountId": "%s",
                                  "categoryId": "%s",
                                  "memberId": "%s"
                                }
                                """
                                        .formatted(account.getId(), category.getId(), allowanceMember.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].memberId").value(nullValue()))
                .andReturn();

        String createdId = OBJECT_MAPPER
                .readTree(createResult.getResponse().getContentAsString())
                .get(0)
                .get("id")
                .asText();

        mockMvc.perform(put("/api/transactions/" + createdId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {
                                  "type": "EXPENSE",
                                  "ownershipType": "SHARED",
                                  "description": "Moved To July",
                                  "amount": 25.00,
                                  "transactionDate": "2026-07-05",
                                  "accountId": "%s",
                                  "categoryId": "%s",
                                  "memberId": "%s"
                                }
                                """
                                        .formatted(account.getId(), category.getId(), allowanceMember.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.referenceMonth").value("2026-07-01"))
                .andExpect(jsonPath("$.memberId").value(nullValue()));
    }

    @Test
    void futureMonthListIncludesProjectedFixedTransaction() throws Exception {
        LocalDate currentReferenceMonth = YearMonth.now().atDay(1);
        LocalDate futureReferenceMonth = currentReferenceMonth.plusMonths(4);

        FixedExpenseTemplate template = new FixedExpenseTemplate();
        template.setName("Projected Rent");
        template.setType(TransactionType.EXPENSE);
        template.setAmount(new BigDecimal("880.00"));
        template.setCategory(category);
        template.setAccount(account);
        template.setDueDay((short) 12);
        template.setCreatedInMonth(currentReferenceMonth.minusMonths(1));
        template.setActive(true);
        template = fixedExpenseTemplateRepository.save(template);

        // Materialize transactions for the future month
        mockMvc.perform(post("/api/transactions/materialize")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("referenceMonth", futureReferenceMonth.toString()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/transactions")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("referenceMonth", futureReferenceMonth.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].description").value("Projected Rent"))
                .andExpect(jsonPath("$.items[0].sourceType").value("FIXED_EXPENSE"))
                .andExpect(jsonPath("$.items[0].fixedExpenseTemplateId")
                        .value(template.getId().toString()))
                .andExpect(jsonPath("$.items[0].projected").value(false))
                .andExpect(jsonPath("$.items[0].transactionDate")
                        .value(futureReferenceMonth.withDayOfMonth(12).toString()));
    }

    @Test
    void descriptionSuggestionsClampTheRequestedLimit() throws Exception {
        IntStream.range(0, 15).forEach(index -> {
            Transaction transaction = new Transaction();
            transaction.setType(TransactionType.EXPENSE);
            transaction.setOwnershipType(OwnershipType.SHARED);
            transaction.setSourceType(TransactionSourceType.MANUAL);
            transaction.setDescription("Suggestion " + index);
            transaction.setAmount(new BigDecimal("10.00"));
            transaction.setTransactionDate(LocalDate.of(2026, 6, 12));
            transaction.setReferenceMonth(LocalDate.of(2026, 6, 1));
            transaction.setCategory(category);
            transaction.setAccount(account);
            transactionRepository.save(transaction);
        });

        mockMvc.perform(get("/api/transactions/descriptions")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("query", "Suggestion")
                        .param("limit", "99"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(12));
    }

    @Test
    void descriptionSuggestionsRejectNegativeLimit() throws Exception {
        mockMvc.perform(get("/api/transactions/descriptions")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("query", "Suggestion")
                        .param("limit", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Limit must be positive."));
    }

    @Test
    void installmentAmountDistributionPreservesTotalForUnevenDivision() throws Exception {
        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {
                                  "type": "EXPENSE",
                                  "ownershipType": "SHARED",
                                  "description": "Notebook",
                                  "amount": 1000.00,
                                  "transactionDate": "2026-06-20",
                                  "accountId": "%s",
                                  "categoryId": "%s",
                                  "installmentCount": 3
                                }
                                """
                                        .formatted(account.getId(), category.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].amount").value(333.34))
                .andExpect(jsonPath("$[1].amount").value(333.33))
                .andExpect(jsonPath("$[2].amount").value(333.33));
    }

    @Test
    void createTransactionRejectsInstallmentPlanBeyondTwoYears() throws Exception {
        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {
                                  "type": "EXPENSE",
                                  "ownershipType": "SHARED",
                                  "description": "Long plan",
                                  "amount": 100.00,
                                  "transactionDate": "%s",
                                  "accountId": "%s",
                                  "categoryId": "%s",
                                  "installmentCount": 26
                                }
                                """
                                        .formatted(YearMonth.now().atDay(10), account.getId(), category.getId())))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("Installment plan cannot exceed 2 years."));
    }

    @Test
    void listSupportsMultipleCategoryIdsAndKeepsSingleCategoryCompatibility() throws Exception {
        mockMvc.perform(get("/api/transactions")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("referenceMonth", "2026-06-01")
                        .param(
                                "categoryIds",
                                category.getId().toString(),
                                transportCategory.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(2));

        mockMvc.perform(get("/api/transactions")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("referenceMonth", "2026-06-01")
                        .param("categoryId", category.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.items[0].categoryName").value("Groceries"));

        mockMvc.perform(get("/api/transactions")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("referenceMonth", "2026-06-01")
                        .param("categoryId", category.getId().toString())
                        .param("categoryIds", transportCategory.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.items[0].categoryName").value("Transport"));
    }

    @Test
    void userCannotAccessTransactionWriteOrDetailEndpoints() throws Exception {
        mockMvc.perform(get("/api/transactions/descriptions")
                        .header("Authorization", "Bearer " + userToken)
                        .param("query", "Mar"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {
                                  "type": "EXPENSE",
                                  "ownershipType": "SHARED",
                                  "description": "Blocked write",
                                  "amount": 50.00,
                                  "transactionDate": "2026-06-12",
                                  "accountId": "%s",
                                  "categoryId": "%s"
                                }
                                """
                                        .formatted(account.getId(), category.getId())))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/transactions/" + sharedTransaction.getId())
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/transactions/" + sharedTransaction.getId())
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {
                                  "type": "EXPENSE",
                                  "ownershipType": "SHARED",
                                  "description": "Blocked update",
                                  "amount": 200.00,
                                  "transactionDate": "2026-06-15",
                                  "accountId": "%s",
                                  "categoryId": "%s"
                                }
                                """
                                        .formatted(account.getId(), category.getId())))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/transactions/" + sharedTransaction.getId())
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteSupportsSingleAndAllScopesForInstallments() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/transactions")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {
                                  "type": "EXPENSE",
                                  "ownershipType": "SHARED",
                                  "description": "Phone installment",
                                  "amount": 300.00,
                                  "transactionDate": "2026-06-25",
                                  "accountId": "%s",
                                  "categoryId": "%s",
                                  "installmentCount": 3
                                }
                                """
                                        .formatted(account.getId(), category.getId())))
                .andExpect(status().isCreated())
                .andReturn();

        String firstInstallmentId = OBJECT_MAPPER
                .readTree(result.getResponse().getContentAsString())
                .get(0)
                .get("id")
                .asText();
        String secondInstallmentId = OBJECT_MAPPER
                .readTree(result.getResponse().getContentAsString())
                .get(1)
                .get("id")
                .asText();
        String installmentGroupId = OBJECT_MAPPER
                .readTree(result.getResponse().getContentAsString())
                .get(0)
                .get("installmentGroupId")
                .asText();

        mockMvc.perform(delete("/api/transactions/" + secondInstallmentId)
                        .header("Authorization", "Bearer " + adminToken)
                        .param("scope", "SINGLE"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/transactions")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("referenceMonth", "2026-07-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[?(@.installmentGroupId=='" + installmentGroupId + "')]")
                        .isEmpty());

        mockMvc.perform(delete("/api/transactions/" + firstInstallmentId)
                        .header("Authorization", "Bearer " + adminToken)
                        .param("scope", "ALL"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/transactions")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("referenceMonth", "2026-06-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[?(@.installmentGroupId=='" + installmentGroupId + "')]")
                        .isEmpty());

        mockMvc.perform(get("/api/transactions")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("referenceMonth", "2026-08-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[?(@.installmentGroupId=='" + installmentGroupId + "')]")
                        .isEmpty());
    }

    @Test
    void deleteScopesAndAuthorizationWork() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/transactions")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {
                                  "type": "EXPENSE",
                                  "ownershipType": "SHARED",
                                  "description": "Notebook installment",
                                  "amount": 500.00,
                                  "transactionDate": "2026-06-25",
                                  "accountId": "%s",
                                  "categoryId": "%s",
                                  "installmentCount": 3
                                }
                                """
                                        .formatted(account.getId(), category.getId())))
                .andExpect(status().isCreated())
                .andReturn();

        String secondInstallmentId = OBJECT_MAPPER
                .readTree(result.getResponse().getContentAsString())
                .get(1)
                .get("id")
                .asText();

        mockMvc.perform(delete("/api/transactions/" + secondInstallmentId)
                        .header("Authorization", "Bearer " + adminToken)
                        .param("scope", "FUTURE"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/transactions")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("referenceMonth", "2026-06-01"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/transactions")
                        .header("Authorization", "Bearer " + userToken)
                        .param("referenceMonth", "2026-06-01"))
                .andExpect(status().isForbidden());
    }

    @Test
    void listTransactionsRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/transactions").param("referenceMonth", "2026-06-01"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createTransactionRequiresAuthentication() throws Exception {
        mockMvc.perform(
                        post("/api/transactions")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {
                                  "type": "EXPENSE",
                                  "ownershipType": "SHARED",
                                  "description": "Test",
                                  "amount": 50.0,
                                  "transactionDate": "2026-06-10",
                                  "accountId": "00000000-0000-0000-0000-000000000000",
                                  "categoryId": "00000000-0000-0000-0000-000000000000"
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateTransactionReturns404ForNonExistentId() throws Exception {
        mockMvc.perform(put("/api/transactions/00000000-0000-0000-0000-000000000000")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {
                                  "type": "EXPENSE",
                                  "ownershipType": "SHARED",
                                  "description": "Updated",
                                  "amount": 75.0,
                                  "transactionDate": "2026-06-10",
                                  "accountId": "%s",
                                  "categoryId": "%s"
                                }
                                """
                                        .formatted(account.getId(), category.getId())))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteTransactionReturns404ForNonExistentId() throws Exception {
        mockMvc.perform(delete("/api/transactions/00000000-0000-0000-0000-000000000000")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("scope", "SINGLE"))
                .andExpect(status().isNotFound());
    }

    @Test
    void createTransactionWithEmptyDescriptionReturns400() throws Exception {
        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {
                                  "type": "EXPENSE",
                                  "ownershipType": "SHARED",
                                  "description": "",
                                  "amount": 50.0,
                                  "transactionDate": "2026-06-10",
                                  "accountId": "%s",
                                  "categoryId": "%s"
                                }
                                """
                                        .formatted(account.getId(), category.getId())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createTransactionWithZeroAmountReturns400() throws Exception {
        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {
                                  "type": "EXPENSE",
                                  "ownershipType": "SHARED",
                                  "description": "Groceries",
                                  "amount": 0,
                                  "transactionDate": "2026-06-10",
                                  "accountId": "%s",
                                  "categoryId": "%s"
                                }
                                """
                                        .formatted(account.getId(), category.getId())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getTransactionReturns404ForNonExistentId() throws Exception {
        mockMvc.perform(get("/api/transactions/00000000-0000-0000-0000-000000000000")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void getTransactionRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/transactions/00000000-0000-0000-0000-000000000000"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void putTransactionRequiresAuthentication() throws Exception {
        mockMvc.perform(
                        put("/api/transactions/00000000-0000-0000-0000-000000000000")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {"type": "EXPENSE", "ownershipType": "SHARED", "description": "T", "amount": 50.0, "transactionDate": "2026-06-10", "accountId": "00000000-0000-0000-0000-000000000000", "categoryId": "00000000-0000-0000-0000-000000000000"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deleteTransactionRequiresAuthentication() throws Exception {
        mockMvc.perform(delete("/api/transactions/00000000-0000-0000-0000-000000000000")
                        .param("scope", "SINGLE"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deleteTransactionWithInvalidScopeReturns400() throws Exception {
        mockMvc.perform(delete("/api/transactions/" + sharedTransaction.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .param("scope", "INVALID"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createTransactionWithUSDAccount_convertsAmount() throws Exception {
        ExchangeRate rate = new ExchangeRate();
        rate.setCurrency("USD");
        rate.setRate(new BigDecimal("5.10"));
        rate.setFetchedAt(OffsetDateTime.now());
        exchangeRateRepository.save(rate);

        Account usdAccount = new Account();
        usdAccount.setName("US Account");
        usdAccount.setType(AccountType.CHECKING);
        usdAccount.setCurrency(CurrencyType.USD);
        usdAccount.setCreatedInMonth(LocalDate.of(2026, 6, 1));
        usdAccount = accountRepository.save(usdAccount);

        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {"type":"EXPENSE","ownershipType":"SHARED","description":"USD expense",
                                "amount":100,"transactionDate":"2026-06-10",
                                "accountId":"%s","categoryId":"%s"}
                                """
                                        .formatted(usdAccount.getId(), category.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].amount").value(100.00))
                .andExpect(jsonPath("$[0].convertedAmount").value(510.00))
                .andExpect(jsonPath("$[0].exchangeRate").value(5.10))
                .andExpect(jsonPath("$[0].currency").value("USD"));
    }

    @Test
    void createTransactionWithBRLAccount_noConversion() throws Exception {
        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {"type":"EXPENSE","ownershipType":"SHARED","description":"BRL expense",
                                "amount":150,"transactionDate":"2026-06-10",
                                "accountId":"%s","categoryId":"%s"}
                                """
                                        .formatted(account.getId(), category.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].amount").value(150.00))
                .andExpect(jsonPath("$[0].convertedAmount").value(150.00))
                .andExpect(jsonPath("$[0].exchangeRate").doesNotExist())
                .andExpect(jsonPath("$[0].currency").value("BRL"));
    }

    @Test
    void getTransactionWithUSD_showsCurrencyFields() throws Exception {
        ExchangeRate rate = new ExchangeRate();
        rate.setCurrency("USD");
        rate.setRate(new BigDecimal("5.10"));
        rate.setFetchedAt(OffsetDateTime.now());
        exchangeRateRepository.save(rate);

        Account usdAccount = new Account();
        usdAccount.setName("US Account");
        usdAccount.setType(AccountType.CHECKING);
        usdAccount.setCurrency(CurrencyType.USD);
        usdAccount.setCreatedInMonth(LocalDate.of(2026, 6, 1));
        usdAccount = accountRepository.save(usdAccount);

        Transaction usdTx = new Transaction();
        usdTx.setType(TransactionType.EXPENSE);
        usdTx.setOwnershipType(OwnershipType.SHARED);
        usdTx.setSourceType(TransactionSourceType.MANUAL);
        usdTx.setDescription("USD Tx");
        usdTx.setAmount(new BigDecimal("100.00"));
        usdTx.setConvertedAmount(new BigDecimal("510.00"));
        usdTx.setExchangeRate(new BigDecimal("5.10"));
        usdTx.setCurrency("USD");
        usdTx.setTransactionDate(LocalDate.of(2026, 6, 10));
        usdTx.setReferenceMonth(LocalDate.of(2026, 6, 1));
        usdTx.setAccount(usdAccount);
        usdTx.setCategory(category);
        transactionRepository.save(usdTx);

        mockMvc.perform(get("/api/transactions/" + usdTx.getId()).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(100.00))
                .andExpect(jsonPath("$.convertedAmount").value(510.00))
                .andExpect(jsonPath("$.exchangeRate").value(5.10))
                .andExpect(jsonPath("$.currency").value("USD"));
    }

    @Test
    void searchByDescriptionFiltersCorrectly() throws Exception {
        mockMvc.perform(get("/api/transactions")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("referenceMonth", "2026-06-01")
                        .param("search", "Market"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].description").value("Market"))
                .andExpect(jsonPath("$.totalItems").value(1));
    }

    @Test
    void searchByDescriptionCaseInsensitive() throws Exception {
        mockMvc.perform(get("/api/transactions")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("referenceMonth", "2026-06-01")
                        .param("search", "market"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].description").value("Market"))
                .andExpect(jsonPath("$.totalItems").value(1));
    }

    @Test
    void searchByDescriptionNoMatchReturnsEmpty() throws Exception {
        mockMvc.perform(get("/api/transactions")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("referenceMonth", "2026-06-01")
                        .param("search", "nonexistent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0))
                .andExpect(jsonPath("$.totalItems").value(0));
    }
}
