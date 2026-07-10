package com.mymoney.api.exchangerate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mymoney.api.config.AppExchangeRateProperties;
import com.mymoney.api.transaction.TransactionRepository;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExchangeRateService {

    private static final String API_URL = "https://economia.awesomeapi.com.br/json/last/USD-BRL";
    private static final String CURRENCY_PAIR = "USDBRL";
    private static final String CURRENCY = "USD";
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);

    private final ExchangeRateRepository exchangeRateRepository;
    private final TransactionRepository transactionRepository;
    private final AppExchangeRateProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient =
            HttpClient.newBuilder().connectTimeout(REQUEST_TIMEOUT).build();

    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void fetchAndUpdateRates() {
        if (!properties.enabled()) {
            return;
        }

        try {
            BigDecimal rate = fetchRateFromApi();
            if (!isValidRate(rate)) {
                log.warn("Invalid exchange rate received: {}", rate);
                return;
            }

            ExchangeRate exchangeRate = new ExchangeRate();
            exchangeRate.setCurrency(CURRENCY);
            exchangeRate.setRate(rate);
            exchangeRate.setFetchedAt(OffsetDateTime.now());
            exchangeRateRepository.save(exchangeRate);

            LocalDate currentMonth = YearMonth.now().atDay(1);
            transactionRepository.updateAmountsForCurrency(CURRENCY, rate, currentMonth);

            log.info("Exchange rate updated: USD 1 = BRL {}", rate);
        } catch (Exception e) {
            log.warn("Failed to fetch exchange rate, keeping last saved value.", e);
        }
    }

    @Transactional
    public ExchangeRateResponse refreshManually() {
        if (!properties.enabled()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Foreign currency support is disabled.");
        }

        try {
            BigDecimal rate = fetchRateFromApi();
            if (!isValidRate(rate)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY, "Exchange rate API returned an invalid rate.");
            }

            ExchangeRate exchangeRate = new ExchangeRate();
            exchangeRate.setCurrency(CURRENCY);
            exchangeRate.setRate(rate);
            exchangeRate.setFetchedAt(OffsetDateTime.now());
            exchangeRateRepository.save(exchangeRate);

            LocalDate currentMonth = YearMonth.now().atDay(1);
            transactionRepository.updateAmountsForCurrency(CURRENCY, rate, currentMonth);

            return new ExchangeRateResponse(rate, exchangeRate.getFetchedAt(), false);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Manual exchange rate refresh failed.", e);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Exchange rate API unavailable.");
        }
    }

    @Transactional(readOnly = true)
    public ExchangeRateResponse getLatest() {
        if (!properties.enabled()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Foreign currency support is disabled.");
        }

        ExchangeRate latest = exchangeRateRepository
                .findFirstByCurrencyOrderByFetchedAtDesc(CURRENCY)
                .orElseThrow(
                        () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No exchange rate data available."));

        boolean stale = latest.getFetchedAt().isBefore(OffsetDateTime.now().minusHours(2));

        return new ExchangeRateResponse(latest.getRate(), latest.getFetchedAt(), stale);
    }

    @Transactional
    public void closeMonth(LocalDate referenceMonth) {
        if (!properties.enabled()) {
            return;
        }

        ExchangeRate latest = exchangeRateRepository
                .findFirstByCurrencyOrderByFetchedAtDesc(CURRENCY)
                .orElse(null);

        if (latest == null) {
            log.warn("No exchange rate available for month-end close of {}.", referenceMonth);
            return;
        }

        transactionRepository.freezeAmountsForMonth(CURRENCY, latest.getRate(), referenceMonth);
        log.info("Month {} closed: USD transactions frozen at rate {}.", referenceMonth, latest.getRate());
    }

    private BigDecimal fetchRateFromApi() throws Exception {
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

    private boolean isValidRate(BigDecimal rate) {
        return rate != null && rate.compareTo(BigDecimal.ZERO) > 0;
    }
}
