package com.mymoney.api.auth.api;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mymoney.api.PostgresIntegrationTestSupport;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerIntegrationTest extends PostgresIntegrationTestSupport {

    private static final String REFRESH_COOKIE_NAME = "bolso_em_dia_refresh_token";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void loginRejectsInvalidCredentials() throws Exception {
        mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {
                                  "email": "admin@bolso-em-dia.local",
                                  "password": "wrong-password"
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void meRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void refreshRequiresCookieAndRejectsInvalidToken() throws Exception {
        mockMvc.perform(post("/api/auth/refresh")).andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/auth/refresh").cookie(new Cookie(REFRESH_COOKIE_NAME, "invalid-token")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutWithoutCookieStillClearsRefreshCookie() throws Exception {
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isNoContent())
                .andExpect(cookie().maxAge(REFRESH_COOKIE_NAME, 0));
    }

    @Test
    void refreshAndLogoutFlowWorks() throws Exception {
        MvcResult loginResult = mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {
                                  "email": "admin@bolso-em-dia.local",
                                  "password": "admin123456"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(cookie().exists(REFRESH_COOKIE_NAME))
                .andReturn();

        Cookie loginRefreshCookie = loginResult.getResponse().getCookie(REFRESH_COOKIE_NAME);
        if (loginRefreshCookie == null) {
            throw new IllegalStateException("Refresh cookie was not set on login.");
        }

        MvcResult refreshResult = mockMvc.perform(post("/api/auth/refresh").cookie(loginRefreshCookie))
                .andExpect(status().isOk())
                .andExpect(cookie().exists(REFRESH_COOKIE_NAME))
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value("admin@bolso-em-dia.local"))
                .andReturn();

        Cookie refreshedCookie = refreshResult.getResponse().getCookie(REFRESH_COOKIE_NAME);
        if (refreshedCookie == null) {
            throw new IllegalStateException("Refresh cookie was not rotated on refresh.");
        }

        mockMvc.perform(post("/api/auth/logout").cookie(refreshedCookie))
                .andExpect(status().isNoContent())
                .andExpect(cookie().maxAge(REFRESH_COOKIE_NAME, 0));

        mockMvc.perform(post("/api/auth/refresh").cookie(refreshedCookie)).andExpect(status().isUnauthorized());
    }

    @Test
    void loginAndMeFlowWorks() throws Exception {
        MvcResult result = mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {
                                  "email": "admin@bolso-em-dia.local",
                                  "password": "admin123456"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("bolso_em_dia_refresh_token"))
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value("admin@bolso-em-dia.local"))
                .andExpect(jsonPath("$.user.preferences.locale").value("pt-BR"))
                .andExpect(jsonPath("$.user.preferences.defaultAccountId").value(nullValue()))
                .andExpect(jsonPath("$.user.preferences.showBalanceWithBudgets").value(false))
                .andReturn();

        String token = JsonTestUtils.extractJsonValue(result.getResponse().getContentAsString(), "accessToken");

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("admin@bolso-em-dia.local"))
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.preferences.locale").value("pt-BR"))
                .andExpect(jsonPath("$.preferences.defaultAccountId").value(nullValue()))
                .andExpect(jsonPath("$.preferences.showBalanceWithBudgets").value(false));
    }
}
