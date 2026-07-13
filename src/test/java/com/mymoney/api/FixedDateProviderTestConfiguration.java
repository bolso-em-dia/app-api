package com.mymoney.api;

import com.mymoney.api.shared.DateProvider;
import java.time.LocalDate;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class FixedDateProviderTestConfiguration {

    private static final LocalDate FIXED_REFERENCE_MONTH = LocalDate.of(2026, 6, 1);
    private static final LocalDate FIXED_TODAY = LocalDate.of(2026, 6, 15);

    @Bean
    @Primary
    DateProvider dateProvider() {
        return new DateProvider() {
            @Override
            public LocalDate currentReferenceMonth() {
                return FIXED_REFERENCE_MONTH;
            }

            @Override
            public LocalDate today() {
                return FIXED_TODAY;
            }
        };
    }
}
