package com.mymoney.api.auth.api;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mymoney.api.PostgresIntegrationTestSupport;
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

    @Autowired
    private MockMvc mockMvc;

    @Test
    void loginAndMeFlowWorks() throws Exception {
        MvcResult result = mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {
                                  "email": "admin@my-money.local",
                                  "password": "admin123456"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("my_money_refresh_token"))
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value("admin@my-money.local"))
                .andExpect(jsonPath("$.user.preferences.locale").value("pt-BR"))
                .andExpect(jsonPath("$.user.preferences.defaultAccountId").value(nullValue()))
                .andExpect(jsonPath("$.user.preferences.showBalanceWithBudgets").value(false))
                .andReturn();

        String token = JsonTestUtils.extractJsonValue(result.getResponse().getContentAsString(), "accessToken");

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("admin@my-money.local"))
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.preferences.locale").value("pt-BR"))
                .andExpect(jsonPath("$.preferences.defaultAccountId").value(nullValue()))
                .andExpect(jsonPath("$.preferences.showBalanceWithBudgets").value(false));
    }
}
