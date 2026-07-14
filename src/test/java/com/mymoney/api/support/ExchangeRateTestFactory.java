package com.mymoney.api.support;

import com.mymoney.api.exchangerate.ExchangeRate;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.function.Consumer;

public final class ExchangeRateTestFactory {

    private ExchangeRateTestFactory() {}

    public static ExchangeRate create() {
        return create(rate -> {});
    }

    public static ExchangeRate create(Consumer<ExchangeRate> customizer) {
        var rate = ExchangeRate.builder()
                .currency("USD")
                .rate(new BigDecimal("5.00"))
                .fetchedAt(OffsetDateTime.parse("2026-06-15T12:00:00Z"))
                .build();
        customizer.accept(rate);
        return rate;
    }
}
