package com.mymoney.api.exchangerate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mymoney.api.config.AppExchangeRateProperties;
import com.mymoney.api.exchangerate.api.response.ExchangeRateResponse;
import com.mymoney.api.preference.UserPreferencesService;
import com.mymoney.api.preference.api.response.UserPreferencesResponse;
import com.mymoney.api.shared.DateProvider;
import com.mymoney.api.transaction.TransactionRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class ExchangeRateServiceTest {

    @Mock
    private ExchangeRateRepository exchangeRateRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AppExchangeRateProperties properties;

    @Mock
    private ExchangeRateClient exchangeRateClient;

    @Mock
    private DateProvider dateProvider;

    @Mock
    private UserPreferencesService userPreferencesService;

    @InjectMocks
    private ExchangeRateService exchangeRateService;

    private ExchangeRate savedRate;

    @BeforeEach
    void setUp() {
        savedRate = new ExchangeRate();
        savedRate.setCurrency("USD");
        savedRate.setRate(new BigDecimal("5.10"));
        savedRate.setFetchedAt(OffsetDateTime.now().minusMinutes(30));
        lenient()
                .when(userPreferencesService.getCurrentUserPreferences())
                .thenReturn(new UserPreferencesResponse(null, "pt-BR", false, true));
    }

    // BT01
    @Test
    void getLatest_returnsMostRecentRate() {
        when(properties.enabled()).thenReturn(true);
        when(exchangeRateRepository.findFirstByCurrencyOrderByFetchedAtDesc("USD"))
                .thenReturn(Optional.of(savedRate));

        ExchangeRateResponse response = exchangeRateService.getLatest();

        assertThat(response.rate()).isEqualByComparingTo("5.10");
        assertThat(response.stale()).isFalse();
    }

    // BT02
    @Test
    void getLatest_noData_throws404() {
        when(properties.enabled()).thenReturn(true);
        when(exchangeRateRepository.findFirstByCurrencyOrderByFetchedAtDesc("USD"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> exchangeRateService.getLatest())
                .isInstanceOf(ResponseStatusException.class)
                .extracting("status")
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    // BT03
    @Test
    void getLatest_fetchedOver2Hours_isStale() {
        when(properties.enabled()).thenReturn(true);
        savedRate.setFetchedAt(OffsetDateTime.now().minusHours(3));
        when(exchangeRateRepository.findFirstByCurrencyOrderByFetchedAtDesc("USD"))
                .thenReturn(Optional.of(savedRate));

        ExchangeRateResponse response = exchangeRateService.getLatest();

        assertThat(response.stale()).isTrue();
    }

    // BT04
    @Test
    void getLatest_fetchedWithin2Hours_isNotStale() {
        when(properties.enabled()).thenReturn(true);
        savedRate.setFetchedAt(OffsetDateTime.now().minusHours(1));
        when(exchangeRateRepository.findFirstByCurrencyOrderByFetchedAtDesc("USD"))
                .thenReturn(Optional.of(savedRate));

        ExchangeRateResponse response = exchangeRateService.getLatest();

        assertThat(response.stale()).isFalse();
    }

    // BT05
    @Test
    void getLatest_exactlyAt2Hours_isStale() {
        when(properties.enabled()).thenReturn(true);
        savedRate.setFetchedAt(OffsetDateTime.now().minusHours(2));
        when(exchangeRateRepository.findFirstByCurrencyOrderByFetchedAtDesc("USD"))
                .thenReturn(Optional.of(savedRate));

        ExchangeRateResponse response = exchangeRateService.getLatest();

        assertThat(response.stale()).isTrue();
    }

    // BT06
    @Test
    void getLatest_disabled_throws422() {
        when(properties.enabled()).thenReturn(false);

        assertThatThrownBy(() -> exchangeRateService.getLatest())
                .isInstanceOf(ResponseStatusException.class)
                .extracting("status")
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    // BT07
    @Test
    void refreshManually_disabled_throws422() {
        when(properties.enabled()).thenReturn(false);

        assertThatThrownBy(() -> exchangeRateService.refreshManually())
                .isInstanceOf(ResponseStatusException.class)
                .extracting("status")
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    void refreshManually_whenEnabled_savesRateAndUpdatesTransactions() throws Exception {
        when(properties.enabled()).thenReturn(true);
        when(exchangeRateClient.fetchUsdBrlRate()).thenReturn(new BigDecimal("5.25"));
        when(exchangeRateRepository.save(any(ExchangeRate.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(dateProvider.currentReferenceMonth()).thenReturn(LocalDate.of(2026, 6, 1));

        ExchangeRateResponse response = exchangeRateService.refreshManually();

        assertThat(response.rate()).isEqualByComparingTo("5.25");
        verify(transactionRepository).updateAmountsForCurrency("USD", new BigDecimal("5.25"), LocalDate.of(2026, 6, 1));
    }

    // BT09
    @Test
    void getLatest_returnsCorrectTimestamp() {
        when(properties.enabled()).thenReturn(true);
        when(exchangeRateRepository.findFirstByCurrencyOrderByFetchedAtDesc("USD"))
                .thenReturn(Optional.of(savedRate));

        ExchangeRateResponse response = exchangeRateService.getLatest();

        assertThat(response.fetchedAt()).isNotNull();
        assertThat(response.rate()).isEqualByComparingTo("5.10");
    }

    // BT10
    @Test
    void fetchAndUpdateRates_disabled_doesNothing() throws Exception {
        when(properties.enabled()).thenReturn(false);

        exchangeRateService.fetchAndUpdateRates();

        verify(exchangeRateRepository, never()).save(any());
        verify(transactionRepository, never()).updateAmountsForCurrency(anyString(), any(), any());
    }

    @Test
    void fetchAndUpdateRates_validRate_savesAndUpdatesTransactions() throws Exception {
        when(properties.enabled()).thenReturn(true);
        when(exchangeRateClient.fetchUsdBrlRate()).thenReturn(new BigDecimal("5.30"));
        when(exchangeRateRepository.save(any(ExchangeRate.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(dateProvider.currentReferenceMonth()).thenReturn(LocalDate.of(2026, 6, 1));

        exchangeRateService.fetchAndUpdateRates();

        verify(exchangeRateRepository).save(any(ExchangeRate.class));
        verify(transactionRepository).updateAmountsForCurrency("USD", new BigDecimal("5.30"), LocalDate.of(2026, 6, 1));
    }

    // BT12
    @Test
    void closeMonth_disabled_doesNothing() {
        when(properties.enabled()).thenReturn(false);

        exchangeRateService.closeMonth(java.time.LocalDate.of(2026, 6, 1));

        verify(transactionRepository, never()).freezeAmountsForMonth(anyString(), any(), any());
    }

    // BT13
    @Test
    void closeMonth_noRateAvailable_logsWarning() {
        when(properties.enabled()).thenReturn(true);
        when(exchangeRateRepository.findFirstByCurrencyOrderByFetchedAtDesc("USD"))
                .thenReturn(Optional.empty());

        exchangeRateService.closeMonth(java.time.LocalDate.of(2026, 6, 1));

        verify(transactionRepository, never()).freezeAmountsForMonth(anyString(), any(), any());
    }

    // BT14
    @Test
    void closeMonth_freezesTransactionsWithLatestRate() {
        when(properties.enabled()).thenReturn(true);
        when(exchangeRateRepository.findFirstByCurrencyOrderByFetchedAtDesc("USD"))
                .thenReturn(Optional.of(savedRate));

        exchangeRateService.closeMonth(java.time.LocalDate.of(2026, 6, 1));

        verify(transactionRepository)
                .freezeAmountsForMonth("USD", savedRate.getRate(), java.time.LocalDate.of(2026, 6, 1));
    }
}
