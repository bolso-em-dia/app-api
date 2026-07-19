package com.mymoney.api.transaction;

import com.mymoney.api.account.CurrencyType;
import com.mymoney.api.error.CodedResponseStatusException;
import com.mymoney.api.error.ErrorCode;
import com.mymoney.api.exchangerate.ExchangeRate;
import com.mymoney.api.exchangerate.ExchangeRateRepository;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CurrencyConversionService {

    private static final String BRL = "BRL";
    private static final String USD = "USD";

    private final ExchangeRateRepository exchangeRateRepository;

    public ConvertedAmount convert(BigDecimal amount, CurrencyType currency, boolean throwIfMissing) {
        if (currency != CurrencyType.USD) {
            return new ConvertedAmount(amount, null, BRL);
        }

        BigDecimal rate = exchangeRateRepository
                .findFirstByCurrencyOrderByFetchedAtDesc(USD)
                .map(ExchangeRate::getRate)
                .orElseGet(() -> missingRate(throwIfMissing));
        return new ConvertedAmount(amount.multiply(rate), rate, USD);
    }

    private BigDecimal missingRate(boolean throwIfMissing) {
        if (throwIfMissing) {
            throw new CodedResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY, ErrorCode.EXCHANGE_RATE_NOT_AVAILABLE);
        }
        return BigDecimal.ONE;
    }

    public record ConvertedAmount(BigDecimal convertedAmount, BigDecimal exchangeRate, String currency) {}
}
