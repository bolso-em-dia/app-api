package com.mymoney.api.exchangerate;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AwesomeApiExchangeRateClient implements ExchangeRateClient {

    private static final String API_URL = "https://economia.awesomeapi.com.br/json/last/USD-BRL";
    private static final String CURRENCY_PAIR = "USDBRL";
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient =
            HttpClient.newBuilder().connectTimeout(REQUEST_TIMEOUT).build();

    @Override
    public BigDecimal fetchUsdBrlRate() throws Exception {
        var request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .timeout(REQUEST_TIMEOUT)
                .GET()
                .build();

        try {
            var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.warn("Awesome API returned non-200 status: status={}", response.statusCode());
                throw new RuntimeException("API returned HTTP " + response.statusCode());
            }

            var root = objectMapper.readTree(response.body());
            var pair = root.get(CURRENCY_PAIR);
            if (pair == null || !pair.has("bid")) {
                log.warn("Awesome API response missing bid field for pair={}", CURRENCY_PAIR);
                throw new RuntimeException("API response missing USDBRL.bid field.");
            }

            var rate = new BigDecimal(pair.get("bid").asText());
            log.info("Awesome API exchange rate fetched successfully: pair={}, rate={}", CURRENCY_PAIR, rate);
            return rate;
        } catch (Exception exception) {
            log.error("Awesome API exchange rate fetch failed.", exception);
            throw exception;
        }
    }
}
