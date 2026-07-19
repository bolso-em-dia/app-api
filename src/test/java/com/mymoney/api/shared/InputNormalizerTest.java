package com.mymoney.api.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mymoney.api.error.CodedResponseStatusException;
import com.mymoney.api.error.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class InputNormalizerTest {

    @Test
    void requireNonBlankThrowsCodedExceptionWithFieldSpecificMessage() {
        assertThatThrownBy(() -> InputNormalizer.requireNonBlank("   ", "Name"))
                .isInstanceOf(CodedResponseStatusException.class)
                .satisfies(exception -> {
                    var coded = (CodedResponseStatusException) exception;
                    assertThat(coded.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(coded.getCode()).isEqualTo(ErrorCode.FIELD_CANNOT_BE_EMPTY.code());
                    assertThat(coded.getReason()).isEqualTo("Name cannot be empty.");
                });
    }
}
