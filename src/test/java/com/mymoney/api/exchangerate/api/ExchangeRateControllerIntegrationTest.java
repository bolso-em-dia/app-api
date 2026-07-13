package com.mymoney.api.exchangerate.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mymoney.api.AuthenticatedIntegrationTestSupport;
import com.mymoney.api.exchangerate.ExchangeRate;
import com.mymoney.api.exchangerate.ExchangeRateClient;
import com.mymoney.api.exchangerate.ExchangeRateRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = "app.exchange-rate.enabled=true")
@AutoConfigureMockMvc
@Transactional
class ExchangeRateControllerIntegrationTest extends AuthenticatedIntegrationTestSupport {

    @Autowired
    private ExchangeRateRepository exchangeRateRepository;

    @MockitoBean
    private ExchangeRateClient exchangeRateClient;

    private String adminToken;

    @BeforeEach
    void setUp() throws Exception {
        adminToken = loginAsAdmin();
        exchangeRateRepository.deleteAll();
    }

    @Test
    void latestEndpoint_returns404WhenNoData() throws Exception {
        enableForeignCurrency();

        mockMvc.perform(get("/api/exchange-rate/latest").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void latestEndpoint_returnsRateWhenDataExists() throws Exception {
        enableForeignCurrency();

        ExchangeRate rate = new ExchangeRate();
        rate.setCurrency("USD");
        rate.setRate(new BigDecimal("5.1064"));
        rate.setFetchedAt(OffsetDateTime.now());
        exchangeRateRepository.save(rate);

        mockMvc.perform(get("/api/exchange-rate/latest").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rate").value(5.1064))
                .andExpect(jsonPath("$.stale").value(false))
                .andExpect(jsonPath("$.fetchedAt").isNotEmpty());
    }

    @Test
    void latestEndpoint_returnsStaleWhenOld() throws Exception {
        enableForeignCurrency();

        ExchangeRate rate = new ExchangeRate();
        rate.setCurrency("USD");
        rate.setRate(new BigDecimal("5.10"));
        rate.setFetchedAt(OffsetDateTime.now().minusHours(3));
        exchangeRateRepository.save(rate);

        mockMvc.perform(get("/api/exchange-rate/latest").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stale").value(true));
    }

    @Test
    void latestEndpoint_requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/exchange-rate/latest")).andExpect(status().isUnauthorized());
    }

    @Test
    void latestEndpoint_rejectsWhenForeignCurrencyPreferenceIsDisabled() throws Exception {
        mockMvc.perform(get("/api/exchange-rate/latest").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("Foreign currency support is disabled."));
    }

    @Test
    void refreshEndpoint_rejectsWhenForeignCurrencyPreferenceIsDisabled() throws Exception {
        mockMvc.perform(post("/api/exchange-rate/refresh").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("Foreign currency support is disabled."));
    }

    @Test
    void refreshEndpoint_requiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/exchange-rate/refresh")).andExpect(status().isUnauthorized());
    }

    private void enableForeignCurrency() throws Exception {
        mockMvc.perform(
                        put("/api/me/preferences")
                                .header("Authorization", "Bearer " + adminToken)
                                .contentType("application/json")
                                .content(
                                        """
                                {
                                  "defaultAccountId": null,
                                  "locale": "pt-BR",
                                  "showBalanceWithBudgets": false,
                                  "showForeignCurrency": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.showForeignCurrency").value(true));
    }
}
