package com.mymoney.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.exchange-rate")
public record AppExchangeRateProperties(boolean enabled) {}
