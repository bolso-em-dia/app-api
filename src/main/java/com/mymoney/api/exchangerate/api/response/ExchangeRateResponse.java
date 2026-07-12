package com.mymoney.api.exchangerate.api.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record ExchangeRateResponse(BigDecimal rate, OffsetDateTime fetchedAt, boolean stale) {}
