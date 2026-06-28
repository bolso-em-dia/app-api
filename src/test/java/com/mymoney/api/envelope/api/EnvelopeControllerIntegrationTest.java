package com.mymoney.api.envelope.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
class EnvelopeControllerIntegrationTest extends PostgresIntegrationTestSupport {

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
    private FamilyMember allowanceMember;
    private Category groceries;
    private Category transport;
    private Account account;
    private EnvelopeModel globalEnvelope;
    private EnvelopeModel allowanceEnvelope;

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

        allowanceMember = new FamilyMember();
        allowanceMember.setName("Karol");
        allowanceMember.setEmail("karol-envelope@my-money.local");
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

        globalEnvelope = new EnvelopeModel();
        globalEnvelope.setName("Family Essentials");
        globalEnvelope.setType(EnvelopeType.GLOBAL);
        globalEnvelope.setMonthlyLimit(new BigDecimal("1000.00"));
        globalEnvelope.setCreatedInMonth(LocalDate.of(2026, 6, 1));
        globalEnvelope.setActive(true);
        globalEnvelope.setCategories(new LinkedHashSet<>(java.util.List.of(groceries, transport)));
        globalEnvelope = envelopeModelRepository.save(globalEnvelope);

        allowanceEnvelope = new EnvelopeModel();
        allowanceEnvelope.setName("Karol Allowance");
        allowanceEnvelope.setType(EnvelopeType.ALLOWANCE);
        allowanceEnvelope.setOwnerMember(allowanceMember);
        allowanceEnvelope.setMonthlyLimit(new BigDecimal("400.00"));
        allowanceEnvelope.setCreatedInMonth(LocalDate.of(2026, 6, 1));
        allowanceEnvelope.setActive(true);
        allowanceEnvelope = envelopeModelRepository.save(allowanceEnvelope);

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

        adminToken = login("admin@my-money.local", "admin123456");
        userToken = login("user@my-money.local", "user123456");
    }

    @Test
    void listDetailTransactionsAndBreakdownWork() throws Exception {
        mockMvc.perform(get("/api/envelopes")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("referenceMonth", "2026-06-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name=='Family Essentials')].consumedAmount")
                        .isNotEmpty())
                .andExpect(jsonPath("$[?(@.name=='Karol Allowance')].consumedAmount")
                        .isNotEmpty());

        mockMvc.perform(get("/api/envelopes/" + globalEnvelope.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .param("referenceMonth", "2026-06-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.consumedAmount").value(150.0))
                .andExpect(jsonPath("$.transactions.length()").value(1));

        mockMvc.perform(get("/api/envelopes/" + allowanceEnvelope.getId() + "/transactions")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("referenceMonth", "2026-06-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].memberName").value("Karol"));

        mockMvc.perform(get("/api/envelopes/" + allowanceEnvelope.getId() + "/category-breakdown")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("referenceMonth", "2026-06-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].categoryName").value("Transport"))
                .andExpect(jsonPath("$[0].amount").value(45.0));
    }

    @Test
    void createUpdateArchiveAndAuthorizationWork() throws Exception {
        mockMvc.perform(post("/api/envelopes")
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

        MvcResult createResult = mockMvc.perform(post("/api/envelopes")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {
                                  "name": "Transport Envelope",
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

        mockMvc.perform(put("/api/envelopes/" + createdId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {
                                  "name": "Transport Envelope Updated",
                                  "type": "GLOBAL",
                                  "categoryIds": ["%s"],
                                  "monthlyLimit": 250.00
                                }
                                """
                                        .formatted(transport.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Transport Envelope Updated"));

        mockMvc.perform(
                        patch("/api/envelopes/" + createdId + "/archive")
                                .header("Authorization", "Bearer " + adminToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {
                                  "archivedFromMonth": "2026-07-01"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(get("/api/envelopes")
                        .header("Authorization", "Bearer " + userToken)
                        .param("referenceMonth", "2026-06-01"))
                .andExpect(status().isForbidden());
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
