package com.mymoney.api.auth.api;

import static org.assertj.core.api.Assertions.assertThat;
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

@SpringBootTest(properties = "app.security.refresh-cookie-secure=true")
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
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40102))
                .andExpect(jsonPath("$.message").value("Invalid email or password."));
    }

    @Test
    void meRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40106))
                .andExpect(jsonPath("$.message").value("Authentication is required."));
    }

    @Test
    void refreshRequiresCookieAndRejectsInvalidToken() throws Exception {
        mockMvc.perform(post("/api/auth/refresh"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40103));

        mockMvc.perform(post("/api/auth/refresh").cookie(new Cookie(REFRESH_COOKIE_NAME, "invalid-token")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40104));
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

        assertSecureRefreshCookie(loginResult.getResponse().getHeader("Set-Cookie"));

        Cookie loginRefreshCookie = loginResult.getResponse().getCookie(REFRESH_COOKIE_NAME);
        if (loginRefreshCookie == null) {
            throw new IllegalStateException("Refresh cookie was not set on login.");
        }

        MvcResult refreshResult = mockMvc.perform(post("/api/auth/refresh").cookie(loginRefreshCookie))
                .andExpect(status().isOk())
                .andExpect(cookie().exists(REFRESH_COOKIE_NAME))
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value("admin@bolso-em-dia.local"))
                .andExpect(jsonPath("$.user.mustChangePassword").value(true))
                .andReturn();

        assertSecureRefreshCookie(refreshResult.getResponse().getHeader("Set-Cookie"));

        Cookie refreshedCookie = refreshResult.getResponse().getCookie(REFRESH_COOKIE_NAME);
        if (refreshedCookie == null) {
            throw new IllegalStateException("Refresh cookie was not rotated on refresh.");
        }

        MvcResult logoutResult = mockMvc.perform(post("/api/auth/logout").cookie(refreshedCookie))
                .andExpect(status().isNoContent())
                .andExpect(cookie().maxAge(REFRESH_COOKIE_NAME, 0))
                .andReturn();

        assertSecureRefreshCookie(logoutResult.getResponse().getHeader("Set-Cookie"));

        mockMvc.perform(post("/api/auth/refresh").cookie(refreshedCookie)).andExpect(status().isUnauthorized());
    }

    @Test
    void refreshIgnoresInvalidAuthorizationHeaderWhenRefreshCookieIsValid() throws Exception {
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

        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(loginRefreshCookie)
                        .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isOk())
                .andExpect(cookie().exists(REFRESH_COOKIE_NAME))
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value("admin@bolso-em-dia.local"));
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
                .andExpect(jsonPath("$.user.mustChangePassword").value(true))
                .andExpect(jsonPath("$.user.preferences.locale").value("pt-BR"))
                .andExpect(jsonPath("$.user.preferences.defaultAccountId").value(nullValue()))
                .andExpect(jsonPath("$.user.preferences.showBalanceWithBudgets").value(false))
                .andReturn();

        String token = JsonTestUtils.extractJsonValue(result.getResponse().getContentAsString(), "accessToken");

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("admin@bolso-em-dia.local"))
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.mustChangePassword").value(true))
                .andExpect(jsonPath("$.preferences.locale").value("pt-BR"))
                .andExpect(jsonPath("$.preferences.defaultAccountId").value(nullValue()))
                .andExpect(jsonPath("$.preferences.showBalanceWithBudgets").value(false));
    }

    @Test
    void flaggedAdminCanOnlyChangePasswordBeforeUsingProtectedApis() throws Exception {
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
                .andReturn();

        String token = JsonTestUtils.extractJsonValue(result.getResponse().getContentAsString(), "accessToken");

        mockMvc.perform(get("/api/me/preferences").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40302));

        mockMvc.perform(
                        post("/api/auth/change-password")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {
                                  "currentPassword": "admin123456",
                                  "newPassword": "admin12345678",
                                  "confirmPassword": "admin12345678"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mustChangePassword").value(false));

        mockMvc.perform(get("/api/me/preferences").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(
                        post("/api/auth/change-password")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {
                                  "currentPassword": "admin12345678",
                                  "newPassword": "admin123456",
                                  "confirmPassword": "admin123456"
                                }
                                """))
                .andExpect(status().isOk());
    }

    @Test
    void loginWithEmptyEmailReturns400() throws Exception {
        mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {"email": "", "password": "admin123456"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40001));
    }

    @Test
    void loginWithInvalidEmailReturns400() throws Exception {
        mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {"email": "not-an-email", "password": "admin123456"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40001));
    }

    @Test
    void loginWithNonExistentEmailReturns401() throws Exception {
        mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {"email": "nobody@example.com", "password": "admin123456"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void changePasswordRequiresAuthentication() throws Exception {
        mockMvc.perform(
                        post("/api/auth/change-password")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {"currentPassword": "admin123456", "newPassword": "admin12345678", "confirmPassword": "admin12345678"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40106));
    }

    @Test
    void changePasswordWithShortNewPasswordReturns400() throws Exception {
        MvcResult loginResult = mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {"email": "admin@bolso-em-dia.local", "password": "admin123456"}
                                """))
                .andExpect(status().isOk())
                .andReturn();
        String token = JsonTestUtils.extractJsonValue(loginResult.getResponse().getContentAsString(), "accessToken");

        mockMvc.perform(
                        post("/api/auth/change-password")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {"currentPassword": "admin123456", "newPassword": "123", "confirmPassword": "123"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void changePasswordRejectsIncorrectCurrentPasswordWithCode() throws Exception {
        MvcResult loginResult = mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {"email": "admin@bolso-em-dia.local", "password": "admin123456"}
                                """))
                .andExpect(status().isOk())
                .andReturn();
        String token = JsonTestUtils.extractJsonValue(loginResult.getResponse().getContentAsString(), "accessToken");

        mockMvc.perform(
                        post("/api/auth/change-password")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {"currentPassword": "wrong", "newPassword": "admin12345678", "confirmPassword": "admin12345678"}
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value(42216))
                .andExpect(jsonPath("$.message").value("Current password is incorrect."));
    }

    @Test
    void changePasswordRejectsConfirmationMismatchWithCode() throws Exception {
        MvcResult loginResult = mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {"email": "admin@bolso-em-dia.local", "password": "admin123456"}
                                """))
                .andExpect(status().isOk())
                .andReturn();
        String token = JsonTestUtils.extractJsonValue(loginResult.getResponse().getContentAsString(), "accessToken");

        mockMvc.perform(
                        post("/api/auth/change-password")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {"currentPassword": "admin123456", "newPassword": "admin12345678", "confirmPassword": "different12345678"}
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value(42217))
                .andExpect(jsonPath("$.message").value("Password confirmation does not match."));
    }

    private void assertSecureRefreshCookie(String setCookieHeader) {
        assertThat(setCookieHeader).isNotBlank();
        assertThat(setCookieHeader).contains("HttpOnly");
        assertThat(setCookieHeader).contains("SameSite=Lax");
        assertThat(setCookieHeader).contains("Secure");
    }
}
