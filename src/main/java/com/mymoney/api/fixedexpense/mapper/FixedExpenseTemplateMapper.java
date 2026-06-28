package com.mymoney.api.fixedexpense.mapper;

import com.mymoney.api.fixedexpense.FixedExpenseTemplate;
import com.mymoney.api.fixedexpense.api.response.FixedExpenseTemplateResponse;
import org.springframework.stereotype.Component;

@Component
public class FixedExpenseTemplateMapper {

    public FixedExpenseTemplateResponse toResponse(FixedExpenseTemplate template) {
        return new FixedExpenseTemplateResponse(
                template.getId().toString(),
                template.getName(),
                template.getAmount(),
                template.getCategory().getId().toString(),
                template.getCategory().getName(),
                template.getAccount().getId().toString(),
                template.getAccount().getName(),
                template.getDueDay(),
                template.getCreatedInMonth(),
                template.getArchivedFromMonth(),
                template.isActive(),
                template.getCreatedAt(),
                template.getUpdatedAt());
    }
}
