package com.mymoney.api.shared;

import com.mymoney.api.error.CodedResponseStatusException;
import com.mymoney.api.error.ErrorCode;
import java.util.Optional;
import java.util.function.Supplier;
import org.springframework.http.HttpStatus;

public final class EntityResolver {

    private EntityResolver() {}

    public static <T> T resolveOrThrow(Supplier<Optional<T>> finder, ErrorCode errorCode) {
        return finder.get().orElseThrow(() -> new CodedResponseStatusException(HttpStatus.NOT_FOUND, errorCode));
    }
}
