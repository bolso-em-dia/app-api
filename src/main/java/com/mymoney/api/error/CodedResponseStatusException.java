package com.mymoney.api.error;

import org.springframework.http.HttpStatusCode;
import org.springframework.web.server.ResponseStatusException;

public class CodedResponseStatusException extends ResponseStatusException {

    private final ErrorCode errorCode;

    public CodedResponseStatusException(HttpStatusCode status, ErrorCode errorCode) {
        super(status, errorCode.description());
        this.errorCode = errorCode;
    }

    public CodedResponseStatusException(HttpStatusCode status, ErrorCode errorCode, String reason) {
        super(status, reason);
        this.errorCode = errorCode;
    }

    public CodedResponseStatusException(HttpStatusCode status, ErrorCode errorCode, Throwable cause) {
        super(status, errorCode.description(), cause);
        this.errorCode = errorCode;
    }

    public CodedResponseStatusException(HttpStatusCode status, ErrorCode errorCode, String reason, Throwable cause) {
        super(status, reason, cause);
        this.errorCode = errorCode;
    }

    public int getCode() {
        return errorCode.code();
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
