package com.mymoney.api.support;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mymoney.api.shared.DateProvider;
import java.time.LocalDate;
import org.springframework.stereotype.Component;

@Component
public class TestSharedSupport {

    private final ObjectMapper objectMapper;
    private final DateProvider dateProvider;

    public TestSharedSupport(ObjectMapper objectMapper, DateProvider dateProvider) {
        this.objectMapper = objectMapper;
        this.dateProvider = dateProvider;
    }

    public LocalDate currentReferenceMonth() {
        return dateProvider.currentReferenceMonth();
    }

    public LocalDate today() {
        return dateProvider.today();
    }

    public String writeJson(Object value) throws JsonProcessingException {
        return objectMapper.writeValueAsString(value);
    }
}
