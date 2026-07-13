package com.mymoney.api.preference.api;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mymoney.api.AuthenticatedIntegrationTestSupport;
import com.mymoney.api.account.Account;
import com.mymoney.api.account.AccountRepository;
import com.mymoney.api.account.AccountType;
import com.mymoney.api.preference.api.request.UpdateUserPreferencesRequest;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UserPreferencesControllerIntegrationTest extends AuthenticatedIntegrationTestSupport {

    @Autowired
    private AccountRepository accountRepository;

    private String adminToken;
    private Account activeAccount;

    @BeforeEach
    void setUp() throws Exception {
        activeAccount = fixtures().persistAccount(created -> {
            created.setName("Everyday Checking");
            created.setType(AccountType.CHECKING);
            created.setCreatedInMonth(LocalDate.of(2026, 1, 1));
        });
        adminToken = loginAsAdmin();
    }

    @Test
    void preferencesEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/me/preferences")).andExpect(status().isUnauthorized());

        mockMvc.perform(put("/api/me/preferences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new UpdateUserPreferencesRequest(null, "pt-BR", false, false))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void returnsDefaultsWhenPreferencesWereNotSavedYet() throws Exception {
        mockMvc.perform(get("/api/me/preferences").header(HttpHeaders.AUTHORIZATION, bearerToken(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.defaultAccountId").value(nullValue()))
                .andExpect(jsonPath("$.locale").value("pt-BR"))
                .andExpect(jsonPath("$.showBalanceWithBudgets").value(false))
                .andExpect(jsonPath("$.showForeignCurrency").value(false));
    }

    @Test
    void getPreferences_defaultsShowForeignCurrencyFalse() throws Exception {
        mockMvc.perform(get("/api/me/preferences").header(HttpHeaders.AUTHORIZATION, bearerToken(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.showForeignCurrency").value(false));
    }

    @Test
    void savesPreferencesLazily() throws Exception {
        mockMvc.perform(put("/api/me/preferences")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new UpdateUserPreferencesRequest(activeAccount.getId(), "en-US", true, false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.defaultAccountId")
                        .value(activeAccount.getId().toString()))
                .andExpect(jsonPath("$.locale").value("en-US"))
                .andExpect(jsonPath("$.showBalanceWithBudgets").value(true))
                .andExpect(jsonPath("$.showForeignCurrency").value(false));

        mockMvc.perform(get("/api/me/preferences").header(HttpHeaders.AUTHORIZATION, bearerToken(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.defaultAccountId")
                        .value(activeAccount.getId().toString()))
                .andExpect(jsonPath("$.locale").value("en-US"))
                .andExpect(jsonPath("$.showBalanceWithBudgets").value(true))
                .andExpect(jsonPath("$.showForeignCurrency").value(false));
    }

    @Test
    void updatePreferences_enablesForeignCurrency() throws Exception {
        mockMvc.perform(put("/api/me/preferences")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new UpdateUserPreferencesRequest(null, "pt-BR", false, true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.showForeignCurrency").value(true));

        mockMvc.perform(get("/api/me/preferences").header(HttpHeaders.AUTHORIZATION, bearerToken(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.showForeignCurrency").value(true));
    }

    @Test
    void updatePreferences_disablesForeignCurrency() throws Exception {
        mockMvc.perform(put("/api/me/preferences")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new UpdateUserPreferencesRequest(null, "pt-BR", false, true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.showForeignCurrency").value(true));

        mockMvc.perform(put("/api/me/preferences")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new UpdateUserPreferencesRequest(null, "pt-BR", false, false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.showForeignCurrency").value(false));

        mockMvc.perform(get("/api/me/preferences").header(HttpHeaders.AUTHORIZATION, bearerToken(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.showForeignCurrency").value(false));
    }

    @Test
    void allowsClearingDefaultAccountAfterSaving() throws Exception {
        mockMvc.perform(put("/api/me/preferences")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new UpdateUserPreferencesRequest(activeAccount.getId(), "en-US", true, false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.defaultAccountId")
                        .value(activeAccount.getId().toString()));

        mockMvc.perform(put("/api/me/preferences")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new UpdateUserPreferencesRequest(null, "pt-BR", false, false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.defaultAccountId").value(nullValue()))
                .andExpect(jsonPath("$.locale").value("pt-BR"))
                .andExpect(jsonPath("$.showBalanceWithBudgets").value(false))
                .andExpect(jsonPath("$.showForeignCurrency").value(false));
    }

    @Test
    void rejectsUnknownDefaultAccount() throws Exception {
        mockMvc.perform(put("/api/me/preferences")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new UpdateUserPreferencesRequest(
                                java.util.UUID.fromString("11111111-1111-1111-1111-111111111111"),
                                "pt-BR",
                                false,
                                false))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Account was not found."));
    }

    @Test
    void rejectsDefaultAccountCreatedInFuture() throws Exception {
        activeAccount.setCreatedInMonth(currentReferenceMonth().plusMonths(1));
        accountRepository.save(activeAccount);

        mockMvc.perform(put("/api/me/preferences")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                toJson(new UpdateUserPreferencesRequest(activeAccount.getId(), "pt-BR", false, false))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("Default account must be active for the current month."));
    }

    @Test
    void rejectsDefaultAccountThatIsInactiveForCurrentMonth() throws Exception {
        activeAccount.setArchivedFromMonth(currentReferenceMonth());
        accountRepository.save(activeAccount);

        mockMvc.perform(put("/api/me/preferences")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                toJson(new UpdateUserPreferencesRequest(activeAccount.getId(), "pt-BR", false, false))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("Default account must be active for the current month."));
    }

    @Test
    void rejectsUnsupportedLocale() throws Exception {
        mockMvc.perform(put("/api/me/preferences")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new UpdateUserPreferencesRequest(null, "es-ES", false, false))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("Locale is not supported."));
    }

    @Test
    void changesCurrentUserPasswordWhenCurrentPasswordMatches() throws Exception {
        mockMvc.perform(
                        post("/api/auth/change-password")
                                .header(HttpHeaders.AUTHORIZATION, bearerToken(adminToken))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {
                                  "currentPassword": "admin123456",
                                  "newPassword": "admin98765432",
                                  "confirmPassword": "admin98765432"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mustChangePassword").value(false));
    }

    private String toJson(Object value) throws Exception {
        return fixtures().writeJson(value);
    }
}
