package com.mymoney.api.shared;

import java.util.Optional;
import java.util.function.Supplier;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public final class EntityResolver {

    private EntityResolver() {}

    public static <T> T resolveOrThrow(Supplier<Optional<T>> finder, String message) {
        return finder.get().orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, message));
    }
}
