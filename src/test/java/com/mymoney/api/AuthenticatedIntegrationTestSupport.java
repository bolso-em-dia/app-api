package com.mymoney.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mymoney.api.auth.api.JsonTestUtils;
import com.mymoney.api.support.IntegrationTestFixtureSupport;
import java.time.LocalDate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

public abstract class AuthenticatedIntegrationTestSupport extends PostgresIntegrationTestSupport {

    private static final String REFRESH_COOKIE_NAME = "bolso_em_dia_refresh_token";

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    private IntegrationTestFixtureSupport fixtureSupport;

    protected String loginAsAdmin() throws Exception {
        fixtureSupport.ensureAdminCanUseProtectedApis();
        return login(IntegrationTestFixtureSupport.ADMIN_EMAIL, IntegrationTestFixtureSupport.ADMIN_PASSWORD);
    }

    protected String loginAsUser() throws Exception {
        fixtureSupport.ensureRegularUser();
        return login(IntegrationTestFixtureSupport.USER_EMAIL, IntegrationTestFixtureSupport.USER_PASSWORD);
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

    protected IntegrationTestFixtureSupport fixtures() {
        return fixtureSupport;
    }

    protected String bearerToken(String token) {
        return "Bearer " + token;
    }

    protected LocalDate currentReferenceMonth() {
        return fixtureSupport.currentReferenceMonth();
    }

    protected LocalDate today() {
        return fixtureSupport.today();
    }
}
