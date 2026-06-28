package com.mymoney.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.admin")
public record AdminSeedProperties(String name, String email, String password) {}
