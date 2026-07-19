package com.mymoney.api.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mymoney.api.error.CodedResponseStatusException;
import com.mymoney.api.error.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class DayValidatorTest {

    @Test
    void validateDayRangeThrowsCodedExceptionWithFieldSpecificMessage() {
        assertThatThrownBy(() -> DayValidator.validateDayRange(0, "Closing day"))
                .isInstanceOf(CodedResponseStatusException.class)
                .satisfies(exception -> {
                    var coded = (CodedResponseStatusException) exception;
                    assertThat(coded.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
                    assertThat(coded.getCode()).isEqualTo(ErrorCode.DAY_OUT_OF_RANGE.code());
                    assertThat(coded.getReason()).isEqualTo("Closing day must be between 1 and 31.");
                });
    }

    @Test
    void requireDueDayThrowsCodedException() {
        assertThatThrownBy(() -> DayValidator.requireDueDay(null))
                .isInstanceOf(CodedResponseStatusException.class)
                .satisfies(exception -> {
                    var coded = (CodedResponseStatusException) exception;
                    assertThat(coded.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(coded.getCode()).isEqualTo(ErrorCode.DUE_DAY_REQUIRED.code());
                    assertThat(coded.getReason()).isEqualTo(ErrorCode.DUE_DAY_REQUIRED.description());
                });
    }
}
