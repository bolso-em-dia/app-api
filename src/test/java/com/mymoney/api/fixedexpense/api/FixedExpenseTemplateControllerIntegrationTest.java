package com.mymoney.api.fixedexpense.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mymoney.api.AuthenticatedIntegrationTestSupport;
import com.mymoney.api.account.Account;
import com.mymoney.api.account.AccountType;
import com.mymoney.api.auth.api.JsonTestUtils;
import com.mymoney.api.category.Category;
import com.mymoney.api.fixedexpense.FixedExpenseTemplate;
import com.mymoney.api.fixedexpense.FixedExpenseTemplateRepository;
import com.mymoney.api.fixedexpense.api.request.CreateFixedExpenseTemplateRequest;
import com.mymoney.api.fixedexpense.api.request.UpdateFixedExpenseTemplateRequest;
import com.mymoney.api.transaction.EffectiveMonthlyTransactionService;
import com.mymoney.api.transaction.Transaction;
import com.mymoney.api.transaction.TransactionRepository;
import com.mymoney.api.transaction.TransactionType;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class FixedExpenseTemplateControllerIntegrationTest extends AuthenticatedIntegrationTestSupport {

    @Autowired
    private FixedExpenseTemplateRepository fixedExpenseTemplateRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private EffectiveMonthlyTransactionService effectiveMonthlyTransactionService;

    @Autowired
    private EntityManager entityManager;

    private String adminToken;
    private String userToken;
    private Category category;
    private Account account;
    private FixedExpenseTemplate template;

    @BeforeEach
    void setUp() throws Exception {
        fixtures().ensureRegularUser();
        category = fixtures().persistCategory(created -> {
            created.setName("Housing");
            created.setCreatedInMonth(currentReferenceMonth());
        });
        account = fixtures().persistAccount(created -> {
            created.setName("Main Checking");
            created.setType(AccountType.CHECKING);
            created.setCreatedInMonth(currentReferenceMonth());
        });
        template = fixtures().persistFixedExpenseTemplate(created -> {
            created.setName("Rent");
            created.setType(TransactionType.EXPENSE);
            created.setAmount(new BigDecimal("1800.00"));
            created.setCategory(category);
            created.setAccount(account);
            created.setDueDay((short) 5);
            created.setCreatedInMonth(currentReferenceMonth());
            created.setActive(true);
        });

        adminToken = loginAsAdmin();
        userToken = loginAsUser();
    }

    @Test
    void adminCanListCreateGetAndUpdateTemplates() throws Exception {
        mockMvc.perform(get("/api/fixed-transactions")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].name").isArray())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));

        template.setArchivedFromMonth(LocalDate.of(2026, 7, 1));
        template.setActive(false);
        fixedExpenseTemplateRepository.save(template);

        mockMvc.perform(get("/api/fixed-transactions")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("search", "rent")
                        .param("status", "ARCHIVED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].name").value("Rent"))
                .andExpect(jsonPath("$.totalItems").value(1));

        mockMvc.perform(post("/api/fixed-transactions")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new CreateFixedExpenseTemplateRequest(
                                "Internet",
                                TransactionType.INCOME,
                                new BigDecimal("120.50"),
                                category.getId(),
                                account.getId(),
                                12))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Internet"))
                .andExpect(jsonPath("$.type").value("INCOME"))
                .andExpect(jsonPath("$.amount").value(120.5))
                .andExpect(jsonPath("$.dueDay").value(12));

        mockMvc.perform(get("/api/fixed-transactions/" + template.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Rent"))
                .andExpect(jsonPath("$.type").value("EXPENSE"));

        mockMvc.perform(put("/api/fixed-transactions/" + template.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new UpdateFixedExpenseTemplateRequest(
                                "Rent Updated",
                                TransactionType.EXPENSE,
                                new BigDecimal("1850.00"),
                                category.getId(),
                                account.getId(),
                                7))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Rent Updated"))
                .andExpect(jsonPath("$.amount").value(1850.0))
                .andExpect(jsonPath("$.dueDay").value(7));
    }

    @Test
    void updateSynchronizesCurrentMonthMaterializedTransaction() throws Exception {
        LocalDate currentReferenceMonth = currentReferenceMonth();
        LocalDate previousReferenceMonth = currentReferenceMonth.minusMonths(1);

        template.setCreatedInMonth(previousReferenceMonth);
        fixedExpenseTemplateRepository.save(template);

        mockMvc.perform(put("/api/fixed-transactions/" + template.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new UpdateFixedExpenseTemplateRequest(
                                "Rent August",
                                TransactionType.INCOME,
                                new BigDecimal("2450.00"),
                                category.getId(),
                                account.getId(),
                                9))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Rent August"))
                .andExpect(jsonPath("$.type").value("INCOME"))
                .andExpect(jsonPath("$.amount").value(2450.0))
                .andExpect(jsonPath("$.dueDay").value(9));

        var materializedTransaction = transactionRepository
                .findByFixedExpenseTemplateIdAndReferenceMonth(template.getId(), currentReferenceMonth)
                .orElseThrow();
        org.assertj.core.api.Assertions.assertThat(materializedTransaction.getDescription())
                .isEqualTo("Rent August");
        org.assertj.core.api.Assertions.assertThat(materializedTransaction.getType())
                .isEqualTo(TransactionType.INCOME);
        org.assertj.core.api.Assertions.assertThat(materializedTransaction.getAmount())
                .isEqualByComparingTo("2450.00");
        org.assertj.core.api.Assertions.assertThat(materializedTransaction.getTransactionDate())
                .isEqualTo(currentReferenceMonth.withDayOfMonth(9));
    }

    @Test
    void deleteRemovesTemplateAndCurrentMonthTransactions() throws Exception {
        LocalDate currentMonth = currentReferenceMonth();
        LocalDate pastMonth = currentMonth.minusMonths(1);

        template.setCreatedInMonth(pastMonth);
        FixedExpenseTemplate saved = fixedExpenseTemplateRepository.save(template);

        mockMvc.perform(delete("/api/fixed-transactions/00000000-0000-0000-0000-000000000000")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/fixed-transactions/" + saved.getId())
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/fixed-transactions/" + saved.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/fixed-transactions/" + saved.getId()).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void fixedExpenseTemplateWritesShouldPopulateAuditFields() throws Exception {
        var createResult = mockMvc.perform(post("/api/fixed-transactions")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new CreateFixedExpenseTemplateRequest(
                                "Audited Template",
                                TransactionType.EXPENSE,
                                new BigDecimal("90.00"),
                                category.getId(),
                                account.getId(),
                                10))))
                .andExpect(status().isCreated())
                .andReturn();

        var createdId =
                JsonTestUtils.extractJsonValue(createResult.getResponse().getContentAsString(), "id");
        var created = fixedExpenseTemplateRepository
                .findById(java.util.UUID.fromString(createdId))
                .orElseThrow();
        assertThat(created.getCreatedBy()).isEqualTo(adminMemberId());
        assertThat(created.getUpdatedBy()).isEqualTo(adminMemberId());

        mockMvc.perform(put("/api/fixed-transactions/" + createdId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new UpdateFixedExpenseTemplateRequest(
                                "Audited Template Updated",
                                TransactionType.EXPENSE,
                                new BigDecimal("95.00"),
                                category.getId(),
                                account.getId(),
                                11))))
                .andExpect(status().isOk());

        var updated = fixedExpenseTemplateRepository
                .findById(java.util.UUID.fromString(createdId))
                .orElseThrow();
        assertThat(updated.getUpdatedBy()).isEqualTo(adminMemberId());
    }

    @Test
    void deleteDetachesPastTransactionsAndRemovesCurrentAndFuture() throws Exception {
        LocalDate twoMonthsAgo = currentReferenceMonth().minusMonths(2);
        LocalDate pastMonth = currentReferenceMonth().minusMonths(1);
        LocalDate currentMonth = currentReferenceMonth();

        template.setCreatedInMonth(twoMonthsAgo);
        FixedExpenseTemplate saved = fixedExpenseTemplateRepository.save(template);

        effectiveMonthlyTransactionService.ensureMaterializedForMonth(pastMonth);
        effectiveMonthlyTransactionService.ensureMaterializedForMonth(currentMonth);

        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(delete("/api/fixed-transactions/" + saved.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(get("/api/fixed-transactions/" + saved.getId()).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());

        List<Transaction> pastTransactions =
                transactionRepository.findByReferenceMonthOrderByTransactionDateAscCreatedAtAsc(pastMonth);
        org.assertj.core.api.Assertions.assertThat(pastTransactions).isNotEmpty();

        org.assertj.core.api.Assertions.assertThat(pastTransactions.get(0).getFixedExpenseTemplate())
                .isNull();

        List<Transaction> currentTransactions =
                transactionRepository.findByReferenceMonthOrderByTransactionDateAscCreatedAtAsc(currentMonth);
        org.assertj.core.api.Assertions.assertThat(currentTransactions.stream()
                        .filter(t ->
                                t.getSourceType() == com.mymoney.api.transaction.TransactionSourceType.FIXED_EXPENSE))
                .isEmpty();
    }

    @Test
    void deleteWithoutAuthReturns401() throws Exception {
        mockMvc.perform(delete("/api/fixed-transactions/" + template.getId())).andExpect(status().isUnauthorized());
    }

    @Test
    void listFixedTransactionsRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/fixed-transactions")).andExpect(status().isUnauthorized());
    }

    @Test
    void listFixedTransactionsAsUserIsForbidden() throws Exception {
        mockMvc.perform(get("/api/fixed-transactions").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void getFixedTransactionRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/fixed-transactions/" + template.getId())).andExpect(status().isUnauthorized());
    }

    @Test
    void getFixedTransactionAsUserIsForbidden() throws Exception {
        mockMvc.perform(get("/api/fixed-transactions/" + template.getId())
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void createFixedTransactionRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/fixed-transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {"name": "Test", "type": "EXPENSE", "amount": 100.0, "categoryId": "%s", "accountId": "%s", "dueDay": 10}
                                """
                                        .formatted(category.getId(), account.getId())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createFixedTransactionAsUserIsForbidden() throws Exception {
        mockMvc.perform(post("/api/fixed-transactions")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {"name": "Test", "type": "EXPENSE", "amount": 100.0, "categoryId": "%s", "accountId": "%s", "dueDay": 10}
                                """
                                        .formatted(category.getId(), account.getId())))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateFixedTransactionRequiresAuthentication() throws Exception {
        mockMvc.perform(put("/api/fixed-transactions/" + template.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {"name": "Updated", "type": "EXPENSE", "amount": 200.0, "categoryId": "%s", "accountId": "%s", "dueDay": 15}
                                """
                                        .formatted(category.getId(), account.getId())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateFixedTransactionAsUserIsForbidden() throws Exception {
        mockMvc.perform(put("/api/fixed-transactions/" + template.getId())
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {"name": "Updated", "type": "EXPENSE", "amount": 200.0, "categoryId": "%s", "accountId": "%s", "dueDay": 15}
                                """
                                        .formatted(category.getId(), account.getId())))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateFixedTransactionReturns404ForNonExistentId() throws Exception {
        mockMvc.perform(put("/api/fixed-transactions/00000000-0000-0000-0000-000000000000")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {"name": "Updated", "type": "EXPENSE", "amount": 200.0, "categoryId": "%s", "accountId": "%s", "dueDay": 15}
                                """
                                        .formatted(category.getId(), account.getId())))
                .andExpect(status().isNotFound());
    }

    @Test
    void createFixedTransactionWithEmptyNameReturns400() throws Exception {
        mockMvc.perform(post("/api/fixed-transactions")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {"name": "", "type": "EXPENSE", "amount": 100.0, "categoryId": "%s", "accountId": "%s", "dueDay": 10}
                                """
                                        .formatted(category.getId(), account.getId())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createFixedTransactionWithZeroAmountReturns400() throws Exception {
        mockMvc.perform(post("/api/fixed-transactions")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {"name": "Test", "type": "EXPENSE", "amount": 0, "categoryId": "%s", "accountId": "%s", "dueDay": 10}
                                """
                                        .formatted(category.getId(), account.getId())))
                .andExpect(status().isBadRequest());
    }

    private String toJson(Object value) throws Exception {
        return fixtures().writeJson(value);
    }

    private java.util.UUID adminMemberId() {
        return fixtures().ensureAdminCanUseProtectedApis().getId();
    }
}
