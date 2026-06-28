package com.mymoney.api.dashboard.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mymoney.api.PostgresIntegrationTestSupport;
import com.mymoney.api.account.Account;
import com.mymoney.api.account.AccountRepository;
import com.mymoney.api.account.AccountType;
import com.mymoney.api.auth.api.JsonTestUtils;
import com.mymoney.api.category.Category;
import com.mymoney.api.category.CategoryRepository;
import com.mymoney.api.envelope.EnvelopeModel;
import com.mymoney.api.envelope.EnvelopeModelRepository;
import com.mymoney.api.envelope.EnvelopeType;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DashboardControllerIntegrationTest extends PostgresIntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

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
    private EnvelopeModelRepository envelopeModelRepository;

    private String adminToken;
    private String userToken;

    @BeforeEach
    void setUp() throws Exception {
        FamilyMember regularUser = familyMemberRepository
                .findByEmailIgnoreCase("user@my-money.local")
                .orElseGet(FamilyMember::new);
        regularUser.setName("Regular User");
        regularUser.setEmail("user@my-money.local");
        regularUser.setPasswordHash(passwordEncoder.encode("user123456"));
        regularUser.setRole(FamilyRole.USER);
        regularUser.setActive(true);
        regularUser.setAllowanceEnabled(false);
        familyMemberRepository.save(regularUser);

        FamilyMember allowanceMember = new FamilyMember();
        allowanceMember.setName("Karol");
        allowanceMember.setEmail("karol-dashboard@my-money.local");
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

        EnvelopeModel familyEnvelope = new EnvelopeModel();
        familyEnvelope.setName("Family Essentials");
        familyEnvelope.setType(EnvelopeType.GLOBAL);
        familyEnvelope.setMonthlyLimit(new BigDecimal("1000.00"));
        familyEnvelope.setCreatedInMonth(LocalDate.of(2026, 6, 1));
        familyEnvelope.setActive(true);
        familyEnvelope.setCategories(new LinkedHashSet<>(java.util.List.of(groceries, transport)));
        envelopeModelRepository.save(familyEnvelope);

        EnvelopeModel allowanceEnvelope = new EnvelopeModel();
        allowanceEnvelope.setName("Karol Allowance");
        allowanceEnvelope.setType(EnvelopeType.ALLOWANCE);
        allowanceEnvelope.setOwnerMember(allowanceMember);
        allowanceEnvelope.setMonthlyLimit(new BigDecimal("400.00"));
        allowanceEnvelope.setCreatedInMonth(LocalDate.of(2026, 6, 1));
        allowanceEnvelope.setActive(true);
        envelopeModelRepository.save(allowanceEnvelope);

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

        adminToken = login("admin@my-money.local", "admin123456");
        userToken = login("user@my-money.local", "user123456");
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
                .andExpect(jsonPath("$.envelopes.length()").value(2))
                .andExpect(jsonPath("$.recentTransactions[0].description").value("Ride app"))
                .andExpect(jsonPath("$.categoryBreakdown[0].categoryName").value("Groceries"));

        mockMvc.perform(get("/api/dashboard")
                        .header("Authorization", "Bearer " + userToken)
                        .param("referenceMonth", "2026-06-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.totalExpense").value(195.0));
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

    private String login(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """
                                        .formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn();

        return JsonTestUtils.extractJsonValue(result.getResponse().getContentAsString(), "accessToken");
    }
}
