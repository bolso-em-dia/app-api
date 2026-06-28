package com.mymoney.api.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({AppSecurityProperties.class, AdminSeedProperties.class})
public class AppConfig {}
