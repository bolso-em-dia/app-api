package com.mymoney.api.account.api;

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
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AccountControllerIntegrationTest extends AuthenticatedIntegrationTestSupport {

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
                .findByEmailIgnoreCase("user@bolso-em-dia.local")
                .orElseGet(FamilyMember::new);
        regularUser.setName("Regular User");
        regularUser.setEmail("user@bolso-em-dia.local");
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

        adminToken = loginAsAdmin();
        userToken = loginAsUser();
    }

    @Test
    void adminCanListCreateGetAndUpdateAccounts() throws Exception {
        mockMvc.perform(get("/api/accounts")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("search", "visa")
                        .param("status", "ACTIVE")
                        .param("type", "CREDIT_CARD")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].name").value("Visa Platinum"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));

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

        mockMvc.perform(get("/api/accounts")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("status", "ARCHIVED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].name").value("Main Checking"));

        mockMvc.perform(get("/api/accounts").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void listAccountsRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/accounts")).andExpect(status().isUnauthorized());
    }

    @Test
    void createAccountRequiresAuthentication() throws Exception {
        mockMvc.perform(
                        post("/api/accounts")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {"name": "Test", "type": "CHECKING"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createAccountAsUserIsForbidden() throws Exception {
        mockMvc.perform(
                        post("/api/accounts")
                                .header("Authorization", "Bearer " + userToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {"name": "Test", "type": "CHECKING"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void getAccountReturns404ForNonExistentId() throws Exception {
        mockMvc.perform(get("/api/accounts/00000000-0000-0000-0000-000000000000")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateAccountReturns404ForNonExistentId() throws Exception {
        mockMvc.perform(
                        put("/api/accounts/00000000-0000-0000-0000-000000000000")
                                .header("Authorization", "Bearer " + adminToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {"name": "Updated", "type": "CHECKING"}
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void archiveAccountReturns404ForNonExistentId() throws Exception {
        mockMvc.perform(patch("/api/accounts/00000000-0000-0000-0000-000000000000/archive")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void archiveAccountRequiresAuthentication() throws Exception {
        mockMvc.perform(patch("/api/accounts/" + accountA.getId() + "/archive")).andExpect(status().isUnauthorized());
    }

    @Test
    void archiveAccountAsUserIsForbidden() throws Exception {
        mockMvc.perform(patch("/api/accounts/" + accountA.getId() + "/archive")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void createAccountWithEmptyNameReturns400() throws Exception {
        mockMvc.perform(
                        post("/api/accounts")
                                .header("Authorization", "Bearer " + adminToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {"name": "", "type": "CHECKING"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createAccountWithNullTypeReturns400() throws Exception {
        mockMvc.perform(
                        post("/api/accounts")
                                .header("Authorization", "Bearer " + adminToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {"name": "Test"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createAccountWithCurrencyUSD() throws Exception {
        mockMvc.perform(
                        post("/api/accounts")
                                .header("Authorization", "Bearer " + adminToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {"name": "US Account", "type": "CHECKING", "currency": "USD"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.currency").value("USD"));
    }

    @Test
    void createAccountDefaultsToBRL() throws Exception {
        mockMvc.perform(
                        post("/api/accounts")
                                .header("Authorization", "Bearer " + adminToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {"name": "BR Account", "type": "CHECKING"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.currency").value("BRL"));
    }

    @Test
    void updateAccountChangesCurrency() throws Exception {
        mockMvc.perform(
                        put("/api/accounts/" + accountA.getId())
                                .header("Authorization", "Bearer " + adminToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {"name": "Updated", "type": "CHECKING", "currency": "USD"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currency").value("USD"));
    }

    @Test
    void listAccountsShowsCurrencyField() throws Exception {
        mockMvc.perform(get("/api/accounts").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].currency").value("BRL"));
    }
}
