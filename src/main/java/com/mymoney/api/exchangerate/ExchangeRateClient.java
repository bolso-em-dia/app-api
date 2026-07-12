package com.mymoney.api.exchangerate;

import java.math.BigDecimal;

public interface ExchangeRateClient {

    BigDecimal fetchUsdBrlRate() throws Exception;
}
