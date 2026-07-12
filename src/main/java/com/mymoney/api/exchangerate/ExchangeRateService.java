package com.mymoney.api.exchangerate;

import com.mymoney.api.config.AppExchangeRateProperties;
import com.mymoney.api.exchangerate.api.response.ExchangeRateResponse;
import com.mymoney.api.shared.DateProvider;
import com.mymoney.api.transaction.TransactionRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExchangeRateService {

    private static final String CURRENCY = "USD";

    private final ExchangeRateRepository exchangeRateRepository;
    private final TransactionRepository transactionRepository;
    private final AppExchangeRateProperties properties;
    private final ExchangeRateClient exchangeRateClient;
    private final DateProvider dateProvider;

    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void fetchAndUpdateRates() {
        if (!properties.enabled()) {
            return;
        }

        try {
            BigDecimal rate = exchangeRateClient.fetchUsdBrlRate();
            if (!isValidRate(rate)) {
                log.warn("Invalid exchange rate received: {}", rate);
                return;
            }
            saveRateAndUpdateTransactions(rate);
        } catch (Exception e) {
            log.warn("Failed to fetch exchange rate, keeping last saved value.", e);
        }
    }

    @Transactional
    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        if (!properties.enabled()) {
            return;
        }
        if (exchangeRateRepository
                .findFirstByCurrencyOrderByFetchedAtDesc(CURRENCY)
                .isPresent()) {
            return;
        }
        fetchAndUpdateRates();
    }

    @Transactional
    public ExchangeRateResponse refreshManually() {
        if (!properties.enabled()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Foreign currency support is disabled.");
        }

        try {
            BigDecimal rate = exchangeRateClient.fetchUsdBrlRate();
            if (!isValidRate(rate)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY, "Exchange rate API returned an invalid rate.");
            }
            ExchangeRate exchangeRate = saveRateAndUpdateTransactions(rate);
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

    private ExchangeRate saveRateAndUpdateTransactions(BigDecimal rate) {
        ExchangeRate exchangeRate = new ExchangeRate();
        exchangeRate.setCurrency(CURRENCY);
        exchangeRate.setRate(rate);
        exchangeRate.setFetchedAt(OffsetDateTime.now());
        ExchangeRate saved = exchangeRateRepository.save(exchangeRate);

        LocalDate currentMonth = dateProvider.currentReferenceMonth();
        transactionRepository.updateAmountsForCurrency(CURRENCY, rate, currentMonth);

        log.info("Exchange rate updated: USD 1 = BRL {}", rate);
        return saved;
    }

    private boolean isValidRate(BigDecimal rate) {
        return rate != null && rate.compareTo(BigDecimal.ZERO) > 0;
    }
}
