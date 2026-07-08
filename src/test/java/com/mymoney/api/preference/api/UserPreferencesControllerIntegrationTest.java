package com.mymoney.api.preference.api;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mymoney.api.PostgresIntegrationTestSupport;
import com.mymoney.api.account.Account;
import com.mymoney.api.account.AccountRepository;
import com.mymoney.api.account.AccountType;
import com.mymoney.api.auth.api.JsonTestUtils;
import java.time.LocalDate;
import java.time.YearMonth;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UserPreferencesControllerIntegrationTest extends PostgresIntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountRepository accountRepository;

    private String adminToken;
    private Account activeAccount;

    @BeforeEach
    void setUp() throws Exception {
        activeAccount = new Account();
        activeAccount.setName("Everyday Checking");
        activeAccount.setType(AccountType.CHECKING);
        activeAccount.setCreatedInMonth(LocalDate.of(2026, 1, 1));
        activeAccount = accountRepository.save(activeAccount);
        adminToken = login("admin@bolso-em-dia.local", "admin123456");
    }

    @Test
    void preferencesEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/me/preferences")).andExpect(status().isUnauthorized());

        mockMvc.perform(
                        put("/api/me/preferences")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {
                                  "defaultAccountId": null,
                                  "locale": "pt-BR",
                                  "showBalanceWithBudgets": false
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void returnsDefaultsWhenPreferencesWereNotSavedYet() throws Exception {
        mockMvc.perform(get("/api/me/preferences").header(HttpHeaders.AUTHORIZATION, bearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.defaultAccountId").value(nullValue()))
                .andExpect(jsonPath("$.locale").value("pt-BR"))
                .andExpect(jsonPath("$.showBalanceWithBudgets").value(false));
    }

    @Test
    void savesPreferencesLazily() throws Exception {
        mockMvc.perform(put("/api/me/preferences")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {
                                  "defaultAccountId": "%s",
                                  "locale": "en-US",
                                  "showBalanceWithBudgets": true
                                }
                                """
                                        .formatted(activeAccount.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.defaultAccountId")
                        .value(activeAccount.getId().toString()))
                .andExpect(jsonPath("$.locale").value("en-US"))
                .andExpect(jsonPath("$.showBalanceWithBudgets").value(true));

        mockMvc.perform(get("/api/me/preferences").header(HttpHeaders.AUTHORIZATION, bearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.defaultAccountId")
                        .value(activeAccount.getId().toString()))
                .andExpect(jsonPath("$.locale").value("en-US"))
                .andExpect(jsonPath("$.showBalanceWithBudgets").value(true));
    }

    @Test
    void allowsClearingDefaultAccountAfterSaving() throws Exception {
        mockMvc.perform(put("/api/me/preferences")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {
                                  "defaultAccountId": "%s",
                                  "locale": "en-US",
                                  "showBalanceWithBudgets": true
                                }
                                """
                                        .formatted(activeAccount.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.defaultAccountId")
                        .value(activeAccount.getId().toString()));

        mockMvc.perform(
                        put("/api/me/preferences")
                                .header(HttpHeaders.AUTHORIZATION, bearerToken())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {
                                  "defaultAccountId": null,
                                  "locale": "pt-BR",
                                  "showBalanceWithBudgets": false
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.defaultAccountId").value(nullValue()))
                .andExpect(jsonPath("$.locale").value("pt-BR"))
                .andExpect(jsonPath("$.showBalanceWithBudgets").value(false));
    }

    @Test
    void rejectsUnknownDefaultAccount() throws Exception {
        mockMvc.perform(
                        put("/api/me/preferences")
                                .header(HttpHeaders.AUTHORIZATION, bearerToken())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {
                                  "defaultAccountId": "11111111-1111-1111-1111-111111111111",
                                  "locale": "pt-BR",
                                  "showBalanceWithBudgets": false
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Account was not found."));
    }

    @Test
    void rejectsDefaultAccountCreatedInFuture() throws Exception {
        activeAccount.setCreatedInMonth(YearMonth.now().plusMonths(1).atDay(1));
        accountRepository.save(activeAccount);

        mockMvc.perform(put("/api/me/preferences")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {
                                  "defaultAccountId": "%s",
                                  "locale": "pt-BR",
                                  "showBalanceWithBudgets": false
                                }
                                """
                                        .formatted(activeAccount.getId())))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("Default account must be active for the current month."));
    }

    @Test
    void rejectsDefaultAccountThatIsInactiveForCurrentMonth() throws Exception {
        activeAccount.setArchivedFromMonth(YearMonth.now().atDay(1));
        accountRepository.save(activeAccount);

        mockMvc.perform(put("/api/me/preferences")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {
                                  "defaultAccountId": "%s",
                                  "locale": "pt-BR",
                                  "showBalanceWithBudgets": false
                                }
                                """
                                        .formatted(activeAccount.getId())))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("Default account must be active for the current month."));
    }

    @Test
    void rejectsUnsupportedLocale() throws Exception {
        mockMvc.perform(
                        put("/api/me/preferences")
                                .header(HttpHeaders.AUTHORIZATION, bearerToken())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {
                                  "defaultAccountId": null,
                                  "locale": "es-ES",
                                  "showBalanceWithBudgets": false
                                }
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("Locale is not supported."));
    }

    private String bearerToken() {
        return "Bearer " + adminToken;
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
                .andExpect(cookie().exists("bolso_em_dia_refresh_token"))
                .andReturn();

        return JsonTestUtils.extractJsonValue(result.getResponse().getContentAsString(), "accessToken");
    }
}
