package com.mymoney.api.account.api;

import static org.assertj.core.api.Assertions.assertThat;
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
import com.mymoney.api.account.api.request.CreateAccountRequest;
import com.mymoney.api.account.api.request.UpdateAccountRequest;
import com.mymoney.api.auth.api.JsonTestUtils;
import java.time.LocalDate;
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
class AccountControllerIntegrationTest extends AuthenticatedIntegrationTestSupport {

    @Autowired
    private AccountRepository accountRepository;

    private String adminToken;
    private String userToken;
    private Account accountA;
    private Account accountB;

    @BeforeEach
    void setUp() throws Exception {
        fixtures().ensureRegularUser();
        accountA = fixtures().persistAccount(created -> {
            created.setName("Main Checking");
            created.setType(AccountType.CHECKING);
            created.setColor("#2266aa");
            created.setCreatedInMonth(LocalDate.of(2026, 5, 1));
        });
        accountB = fixtures().persistAccount(created -> {
            created.setName("Visa Platinum");
            created.setType(AccountType.CREDIT_CARD);
            created.setBrand("Visa");
            created.setClosingDay((short) 10);
            created.setDueDay((short) 17);
            created.setCreatedInMonth(LocalDate.of(2026, 6, 1));
        });

        adminToken = loginAsAdmin();
        userToken = loginAsUser();
    }

    @Test
    void adminCanListCreateGetAndUpdateAccounts() throws Exception {
        mockMvc.perform(get("/api/accounts")
                        .header("Authorization", bearerToken(adminToken))
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

        mockMvc.perform(post("/api/accounts")
                        .header("Authorization", bearerToken(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new CreateAccountRequest(
                                "Nubank", AccountType.CREDIT_CARD, null, "Mastercard", "#7d2bd9", 8, 15))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Nubank"))
                .andExpect(jsonPath("$.type").value("CREDIT_CARD"))
                .andExpect(jsonPath("$.brand").value("Mastercard"));

        mockMvc.perform(get("/api/accounts/" + accountA.getId()).header("Authorization", bearerToken(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Main Checking"));

        mockMvc.perform(put("/api/accounts/" + accountB.getId())
                        .header("Authorization", bearerToken(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new UpdateAccountRequest(
                                "Visa Infinite", AccountType.CREDIT_CARD, null, "Visa", "#111111", 12, 20))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Visa Infinite"))
                .andExpect(jsonPath("$.closingDay").value(12))
                .andExpect(jsonPath("$.dueDay").value(20));
    }

    @Test
    void accountValidationAndOptionsWork() throws Exception {
        mockMvc.perform(post("/api/accounts")
                        .header("Authorization", bearerToken(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new CreateAccountRequest(
                                "Checking With Card Fields", AccountType.CHECKING, null, null, null, 5, 10))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value(42203))
                .andExpect(jsonPath("$.message").value("Only credit cards accept closing day and due day."));

        mockMvc.perform(post("/api/accounts")
                        .header("Authorization", bearerToken(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new CreateAccountRequest(
                                "Card Missing Days", AccountType.CREDIT_CARD, null, null, null, null, null))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value(42202))
                .andExpect(jsonPath("$.message").value("Credit cards require both closing day and due day."));

        mockMvc.perform(post("/api/accounts")
                        .header("Authorization", bearerToken(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new CreateAccountRequest(
                                "Card Invalid Closing Day", AccountType.CREDIT_CARD, null, null, null, 0, 10))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value(42220))
                .andExpect(jsonPath("$.message").value("Closing day must be between 1 and 31."));

        accountA.setArchivedFromMonth(LocalDate.of(2026, 8, 1));
        accountRepository.save(accountA);

        mockMvc.perform(get("/api/accounts/options")
                        .header("Authorization", bearerToken(adminToken))
                        .param("referenceMonth", "2026-07-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name=='Main Checking')]").isNotEmpty())
                .andExpect(jsonPath("$[?(@.name=='Visa Platinum')]").isNotEmpty());

        mockMvc.perform(get("/api/accounts/options")
                        .header("Authorization", bearerToken(adminToken))
                        .param("referenceMonth", "2026-08-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name=='Main Checking')]").isEmpty())
                .andExpect(jsonPath("$[?(@.name=='Visa Platinum')]").isNotEmpty());
    }

    @Test
    void adminCanArchiveAndUserCannotAccessAccountsApi() throws Exception {
        mockMvc.perform(
                        patch("/api/accounts/" + accountA.getId() + "/archive")
                                .header("Authorization", bearerToken(adminToken))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {
                                  "archivedFromMonth": "2026-07-01"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.archivedFromMonth")
                        .value(currentReferenceMonth().toString()));

        mockMvc.perform(get("/api/accounts")
                        .header("Authorization", bearerToken(adminToken))
                        .param("status", "ARCHIVED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].name").value("Main Checking"));

        mockMvc.perform(get("/api/accounts").header("Authorization", bearerToken(userToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40301))
                .andExpect(jsonPath("$.message").value("Access denied."))
                .andExpect(jsonPath("$.path").value("/api/accounts"));
    }

    @Test
    void accountWritesShouldPopulateAuditFields() throws Exception {
        var createResult = mockMvc.perform(post("/api/accounts")
                        .header("Authorization", bearerToken(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new CreateAccountRequest(
                                "Audited Account", AccountType.CHECKING, null, null, null, null, null))))
                .andExpect(status().isCreated())
                .andReturn();

        var createdId =
                JsonTestUtils.extractJsonValue(createResult.getResponse().getContentAsString(), "id");
        var created =
                accountRepository.findById(java.util.UUID.fromString(createdId)).orElseThrow();
        assertThat(created.getCreatedBy()).isEqualTo(adminMemberId());
        assertThat(created.getUpdatedBy()).isEqualTo(adminMemberId());

        mockMvc.perform(put("/api/accounts/" + createdId)
                        .header("Authorization", bearerToken(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new UpdateAccountRequest(
                                "Audited Account Updated",
                                AccountType.CHECKING,
                                CurrencyType.USD,
                                null,
                                null,
                                null,
                                null))))
                .andExpect(status().isOk());

        var updated =
                accountRepository.findById(java.util.UUID.fromString(createdId)).orElseThrow();
        assertThat(updated.getUpdatedBy()).isEqualTo(adminMemberId());

        mockMvc.perform(patch("/api/accounts/" + createdId + "/archive")
                        .header("Authorization", bearerToken(adminToken)))
                .andExpect(status().isOk());

        var archived =
                accountRepository.findById(java.util.UUID.fromString(createdId)).orElseThrow();
        assertThat(archived.getUpdatedBy()).isEqualTo(adminMemberId());
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
        mockMvc.perform(post("/api/accounts")
                        .header("Authorization", bearerToken(userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(
                                new CreateAccountRequest("Test", AccountType.CHECKING, null, null, null, null, null))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40301));
    }

    @Test
    void getAccountReturns404ForNonExistentId() throws Exception {
        mockMvc.perform(get("/api/accounts/00000000-0000-0000-0000-000000000000")
                        .header("Authorization", bearerToken(adminToken)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40401));
    }

    @Test
    void updateAccountReturns404ForNonExistentId() throws Exception {
        mockMvc.perform(put("/api/accounts/00000000-0000-0000-0000-000000000000")
                        .header("Authorization", bearerToken(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new UpdateAccountRequest(
                                "Updated", AccountType.CHECKING, null, null, null, null, null))))
                .andExpect(status().isNotFound());
    }

    @Test
    void archiveAccountReturns404ForNonExistentId() throws Exception {
        mockMvc.perform(patch("/api/accounts/00000000-0000-0000-0000-000000000000/archive")
                        .header("Authorization", bearerToken(adminToken)))
                .andExpect(status().isNotFound());
    }

    @Test
    void archiveAccountRequiresAuthentication() throws Exception {
        mockMvc.perform(patch("/api/accounts/" + accountA.getId() + "/archive")).andExpect(status().isUnauthorized());
    }

    @Test
    void archiveAccountAsUserIsForbidden() throws Exception {
        mockMvc.perform(patch("/api/accounts/" + accountA.getId() + "/archive")
                        .header("Authorization", bearerToken(userToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    void createAccountWithEmptyNameReturns400() throws Exception {
        mockMvc.perform(
                        post("/api/accounts")
                                .header("Authorization", bearerToken(adminToken))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {"name": "", "type": "CHECKING"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40001));
    }

    @Test
    void createAccountWithNullTypeReturns400() throws Exception {
        mockMvc.perform(
                        post("/api/accounts")
                                .header("Authorization", bearerToken(adminToken))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {"name": "Test"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createAccountWithCurrencyUSD() throws Exception {
        mockMvc.perform(post("/api/accounts")
                        .header("Authorization", bearerToken(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new CreateAccountRequest(
                                "US Account", AccountType.CHECKING, CurrencyType.USD, null, null, null, null))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.currency").value("USD"));
    }

    @Test
    void createAccountDefaultsToBRL() throws Exception {
        mockMvc.perform(post("/api/accounts")
                        .header("Authorization", bearerToken(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new CreateAccountRequest(
                                "BR Account", AccountType.CHECKING, null, null, null, null, null))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.currency").value("BRL"));
    }

    @Test
    void updateAccountChangesCurrency() throws Exception {
        mockMvc.perform(put("/api/accounts/" + accountA.getId())
                        .header("Authorization", bearerToken(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new UpdateAccountRequest(
                                "Updated", AccountType.CHECKING, CurrencyType.USD, null, null, null, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currency").value("USD"));
    }

    @Test
    void listAccountsShowsCurrencyField() throws Exception {
        mockMvc.perform(get("/api/accounts").header("Authorization", bearerToken(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].currency").value("BRL"));
    }

    private String toJson(Object value) throws Exception {
        return fixtures().writeJson(value);
    }

    private java.util.UUID adminMemberId() {
        return fixtures().ensureAdminCanUseProtectedApis().getId();
    }
}
