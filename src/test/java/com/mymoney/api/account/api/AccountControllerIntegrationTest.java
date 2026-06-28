package com.mymoney.api.account.api;

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
import com.mymoney.api.member.FamilyMember;
import com.mymoney.api.member.FamilyMemberRepository;
import com.mymoney.api.member.FamilyRole;
import java.time.LocalDate;
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
class AccountControllerIntegrationTest extends PostgresIntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FamilyMemberRepository familyMemberRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AccountRepository accountRepository;

    private String adminToken;
    private String userToken;
    private Account accountA;
    private Account accountB;

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

        accountA = new Account();
        accountA.setName("Main Checking");
        accountA.setType(AccountType.CHECKING);
        accountA.setColor("#2266aa");
        accountA.setCreatedInMonth(LocalDate.of(2026, 5, 1));
        accountA = accountRepository.save(accountA);

        accountB = new Account();
        accountB.setName("Visa Platinum");
        accountB.setType(AccountType.CREDIT_CARD);
        accountB.setBrand("Visa");
        accountB.setClosingDay((short) 10);
        accountB.setDueDay((short) 17);
        accountB.setCreatedInMonth(LocalDate.of(2026, 6, 1));
        accountB = accountRepository.save(accountB);

        adminToken = login("admin@my-money.local", "admin123456");
        userToken = login("user@my-money.local", "user123456");
    }

    @Test
    void adminCanListCreateGetAndUpdateAccounts() throws Exception {
        mockMvc.perform(get("/api/accounts").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name").isArray());

        mockMvc.perform(
                        post("/api/accounts")
                                .header("Authorization", "Bearer " + adminToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {
                                  "name": "Nubank",
                                  "type": "CREDIT_CARD",
                                  "brand": "Mastercard",
                                  "color": "#7d2bd9",
                                  "closingDay": 8,
                                  "dueDay": 15
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Nubank"))
                .andExpect(jsonPath("$.type").value("CREDIT_CARD"))
                .andExpect(jsonPath("$.brand").value("Mastercard"));

        mockMvc.perform(get("/api/accounts/" + accountA.getId()).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Main Checking"));

        mockMvc.perform(
                        put("/api/accounts/" + accountB.getId())
                                .header("Authorization", "Bearer " + adminToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {
                                  "name": "Visa Infinite",
                                  "type": "CREDIT_CARD",
                                  "brand": "Visa",
                                  "color": "#111111",
                                  "closingDay": 12,
                                  "dueDay": 20
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Visa Infinite"))
                .andExpect(jsonPath("$.closingDay").value(12))
                .andExpect(jsonPath("$.dueDay").value(20));
    }

    @Test
    void accountValidationAndOptionsWork() throws Exception {
        mockMvc.perform(
                        post("/api/accounts")
                                .header("Authorization", "Bearer " + adminToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {
                                  "name": "Checking With Card Fields",
                                  "type": "CHECKING",
                                  "closingDay": 5,
                                  "dueDay": 10
                                }
                                """))
                .andExpect(status().isUnprocessableEntity());

        mockMvc.perform(
                        post("/api/accounts")
                                .header("Authorization", "Bearer " + adminToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {
                                  "name": "Card Missing Days",
                                  "type": "CREDIT_CARD"
                                }
                                """))
                .andExpect(status().isUnprocessableEntity());

        accountA.setArchivedFromMonth(LocalDate.of(2026, 8, 1));
        accountRepository.save(accountA);

        mockMvc.perform(get("/api/accounts/options")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("referenceMonth", "2026-07-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name=='Main Checking')]").isNotEmpty())
                .andExpect(jsonPath("$[?(@.name=='Visa Platinum')]").isNotEmpty());

        mockMvc.perform(get("/api/accounts/options")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("referenceMonth", "2026-08-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name=='Main Checking')]").isEmpty())
                .andExpect(jsonPath("$[?(@.name=='Visa Platinum')]").isNotEmpty());
    }

    @Test
    void adminCanArchiveAndUserCannotAccessAccountsApi() throws Exception {
        mockMvc.perform(
                        patch("/api/accounts/" + accountA.getId() + "/archive")
                                .header("Authorization", "Bearer " + adminToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {
                                  "archivedFromMonth": "2026-07-01"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.archivedFromMonth").value("2026-07-01"));

        mockMvc.perform(get("/api/accounts").header("Authorization", "Bearer " + userToken))
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
