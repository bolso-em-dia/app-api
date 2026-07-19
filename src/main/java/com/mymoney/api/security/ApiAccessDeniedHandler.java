package com.mymoney.api.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mymoney.api.error.ApiErrorResponse;
import com.mymoney.api.error.ErrorCode;
import com.mymoney.api.logging.SecurityLoggingHelper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ApiAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(
            HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException)
            throws IOException {
        log.warn(
                "Access denied on {} {} by user={} message={}",
                request.getMethod(),
                SecurityLoggingHelper.sanitizePath(request),
                SecurityLoggingHelper.currentUser(),
                SecurityLoggingHelper.sanitize(accessDeniedException.getMessage()));

        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(
                response.getWriter(),
                ApiErrorResponse.coded(
                        HttpStatus.FORBIDDEN,
                        ErrorCode.FILTER_ACCESS_DENIED,
                        SecurityLoggingHelper.sanitizePath(request)));
    }
}
