package com.mymoney.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mymoney.api.auth.api.JsonTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

public abstract class AuthenticatedIntegrationTestSupport extends PostgresIntegrationTestSupport {

    private static final String ADMIN_EMAIL = "admin@bolso-em-dia.local";
    private static final String ADMIN_PASSWORD = "admin123456";
    private static final String ADMIN_TEMPORARY_PASSWORD = "admin12345678";
    private static final String USER_EMAIL = "user@bolso-em-dia.local";
    private static final String USER_PASSWORD = "user123456";
    private static final String REFRESH_COOKIE_NAME = "bolso_em_dia_refresh_token";

    @Autowired
    protected MockMvc mockMvc;

    protected String loginAsAdmin() throws Exception {
        String token = login(ADMIN_EMAIL, ADMIN_PASSWORD);
        clearForcedPasswordChangeForSeededAdmin(token);
        return token;
    }

    protected String loginAsUser() throws Exception {
        return login(USER_EMAIL, USER_PASSWORD);
    }

    protected String login(String email, String password) throws Exception {
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
                .andExpect(cookie().exists(REFRESH_COOKIE_NAME))
                .andReturn();

        return JsonTestUtils.extractJsonValue(result.getResponse().getContentAsString(), "accessToken");
    }

    private void clearForcedPasswordChangeForSeededAdmin(String token) throws Exception {
        changePassword(token, ADMIN_PASSWORD, ADMIN_TEMPORARY_PASSWORD);
        changePassword(token, ADMIN_TEMPORARY_PASSWORD, ADMIN_PASSWORD);
    }

    private void changePassword(String token, String currentPassword, String newPassword) throws Exception {
        mockMvc.perform(post("/api/auth/change-password")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {
                                  "currentPassword": "%s",
                                  "newPassword": "%s",
                                  "confirmPassword": "%s"
                                }
                                """
                                        .formatted(currentPassword, newPassword, newPassword)))
                .andExpect(status().isOk());
    }
}
