package com.mymoney.api.config;

import com.mymoney.api.audit.AuditorResolver;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing
public class AuditConfig {

    @Bean
    public AuditorAware<UUID> auditorAware(AuditorResolver auditorResolver) {
        return () -> {
            try {
                return Optional.of(auditorResolver.resolveMemberId());
            } catch (RuntimeException exception) {
                return Optional.empty();
            }
        };
    }
}
