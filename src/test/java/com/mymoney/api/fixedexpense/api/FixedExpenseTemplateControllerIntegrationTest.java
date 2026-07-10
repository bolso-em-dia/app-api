package com.mymoney.api.fixedexpense.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mymoney.api.AuthenticatedIntegrationTestSupport;
import com.mymoney.api.account.Account;
import com.mymoney.api.account.AccountRepository;
import com.mymoney.api.account.AccountType;
import com.mymoney.api.category.Category;
import com.mymoney.api.category.CategoryRepository;
import com.mymoney.api.fixedexpense.FixedExpenseTemplate;
import com.mymoney.api.fixedexpense.FixedExpenseTemplateRepository;
import com.mymoney.api.member.FamilyMember;
import com.mymoney.api.member.FamilyMemberRepository;
import com.mymoney.api.member.FamilyRole;
import com.mymoney.api.transaction.EffectiveMonthlyTransactionService;
import com.mymoney.api.transaction.Transaction;
import com.mymoney.api.transaction.TransactionRepository;
import com.mymoney.api.transaction.TransactionType;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class FixedExpenseTemplateControllerIntegrationTest extends AuthenticatedIntegrationTestSupport {

    @Autowired
    private FamilyMemberRepository familyMemberRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private AccountRepository accountRepository;

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

        category = new Category();
        category.setName("Housing");
        category.setCreatedInMonth(LocalDate.of(2026, 6, 1));
        category = categoryRepository.save(category);

        account = new Account();
        account.setName("Main Checking");
        account.setType(AccountType.CHECKING);
        account.setCreatedInMonth(LocalDate.of(2026, 6, 1));
        account = accountRepository.save(account);

        template = new FixedExpenseTemplate();
        template.setName("Rent");
        template.setType(TransactionType.EXPENSE);
        template.setAmount(new BigDecimal("1800.00"));
        template.setCategory(category);
        template.setAccount(account);
        template.setDueDay((short) 5);
        template.setCreatedInMonth(LocalDate.of(2026, 6, 1));
        template.setActive(true);
        template = fixedExpenseTemplateRepository.save(template);

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
                        .content(
                                """
                                {
                                  "name": "Internet",
                                  "type": "INCOME",
                                  "amount": 120.50,
                                  "categoryId": "%s",
                                  "accountId": "%s",
                                  "dueDay": 12
                                }
                                """
                                        .formatted(category.getId(), account.getId())))
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
                        .content(
                                """
                                {
                                  "name": "Rent Updated",
                                  "type": "EXPENSE",
                                  "amount": 1850.00,
                                  "categoryId": "%s",
                                  "accountId": "%s",
                                  "dueDay": 7
                                }
                                """
                                        .formatted(category.getId(), account.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Rent Updated"))
                .andExpect(jsonPath("$.amount").value(1850.0))
                .andExpect(jsonPath("$.dueDay").value(7));
    }

    @Test
    void updateSynchronizesCurrentMonthMaterializedTransaction() throws Exception {
        LocalDate currentReferenceMonth = YearMonth.now().atDay(1);
        LocalDate previousReferenceMonth = currentReferenceMonth.minusMonths(1);

        template.setCreatedInMonth(previousReferenceMonth);
        fixedExpenseTemplateRepository.save(template);

        mockMvc.perform(put("/api/fixed-transactions/" + template.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {
                                  "name": "Rent August",
                                  "type": "INCOME",
                                  "amount": 2450.00,
                                  "categoryId": "%s",
                                  "accountId": "%s",
                                  "dueDay": 9
                                }
                                """
                                        .formatted(category.getId(), account.getId())))
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
        LocalDate currentMonth = YearMonth.now().atDay(1);
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
    void deleteDetachesPastTransactionsAndRemovesCurrentAndFuture() throws Exception {
        LocalDate twoMonthsAgo = YearMonth.now().minusMonths(2).atDay(1);
        LocalDate pastMonth = YearMonth.now().minusMonths(1).atDay(1);
        LocalDate currentMonth = YearMonth.now().atDay(1);

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
}
