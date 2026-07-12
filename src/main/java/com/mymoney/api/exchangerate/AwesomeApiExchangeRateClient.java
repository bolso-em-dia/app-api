package com.mymoney.api.exchangerate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.springframework.stereotype.Component;

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
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .timeout(REQUEST_TIMEOUT)
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new RuntimeException("API returned HTTP " + response.statusCode());
        }

        JsonNode root = objectMapper.readTree(response.body());
        JsonNode pair = root.get(CURRENCY_PAIR);
        if (pair == null || !pair.has("bid")) {
            throw new RuntimeException("API response missing USDBRL.bid field.");
        }

        return new BigDecimal(pair.get("bid").asText());
    }
}
