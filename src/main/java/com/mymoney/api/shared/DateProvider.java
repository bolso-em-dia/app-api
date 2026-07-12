package com.mymoney.api.shared;

import java.time.LocalDate;
import java.time.YearMonth;
import org.springframework.stereotype.Component;

@Component
public class DateProvider {

    public LocalDate currentReferenceMonth() {
        return YearMonth.now().atDay(1);
    }

    public LocalDate today() {
        return LocalDate.now();
    }
}
