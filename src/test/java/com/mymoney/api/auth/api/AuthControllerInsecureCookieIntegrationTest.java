package com.mymoney.api.auth.api;

import static org.assertj.core.api.Assertions.assertThat;
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

@SpringBootTest(properties = "app.security.refresh-cookie-secure=false")
@AutoConfigureMockMvc
class AuthControllerInsecureCookieIntegrationTest extends PostgresIntegrationTestSupport {

    private static final String REFRESH_COOKIE_NAME = "bolso_em_dia_refresh_token";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void loginRefreshAndLogoutOmitSecureAttributeWhenConfiguredOff() throws Exception {
        var loginResult = mockMvc.perform(
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

        assertInsecureRefreshCookie(loginResult.getResponse().getHeader("Set-Cookie"));

        Cookie loginRefreshCookie = loginResult.getResponse().getCookie(REFRESH_COOKIE_NAME);
        if (loginRefreshCookie == null) {
            throw new IllegalStateException("Refresh cookie was not set on login.");
        }

        var refreshResult = mockMvc.perform(post("/api/auth/refresh").cookie(loginRefreshCookie))
                .andExpect(status().isOk())
                .andExpect(cookie().exists(REFRESH_COOKIE_NAME))
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andReturn();

        assertInsecureRefreshCookie(refreshResult.getResponse().getHeader("Set-Cookie"));

        var refreshedCookie = refreshResult.getResponse().getCookie(REFRESH_COOKIE_NAME);
        if (refreshedCookie == null) {
            throw new IllegalStateException("Refresh cookie was not rotated on refresh.");
        }

        var logoutResult = mockMvc.perform(post("/api/auth/logout").cookie(refreshedCookie))
                .andExpect(status().isNoContent())
                .andExpect(cookie().maxAge(REFRESH_COOKIE_NAME, 0))
                .andReturn();

        assertInsecureRefreshCookie(logoutResult.getResponse().getHeader("Set-Cookie"));
    }

    private void assertInsecureRefreshCookie(String setCookieHeader) {
        assertThat(setCookieHeader).isNotBlank();
        assertThat(setCookieHeader).contains("HttpOnly");
        assertThat(setCookieHeader).contains("SameSite=Lax");
        assertThat(setCookieHeader).doesNotContain("Secure");
    }
}
