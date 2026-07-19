package com.mymoney.api.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final String HEADER = "X-Request-ID";
    private static final String MDC_KEY = "requestId";
    private static final Pattern REQUEST_ID_PATTERN = Pattern.compile("[A-Za-z0-9._-]{1,64}");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        var requestId = resolveRequestId(request);

        MDC.put(MDC_KEY, requestId);
        response.setHeader(HEADER, requestId);

        var start = System.currentTimeMillis();
        try {
            chain.doFilter(request, response);
        } finally {
            log.info(
                    "{} {} status={} elapsed={}ms",
                    request.getMethod(),
                    SecurityLoggingHelper.sanitizePath(request),
                    response.getStatus(),
                    System.currentTimeMillis() - start);
            MDC.remove(MDC_KEY);
        }
    }

    private String resolveRequestId(HttpServletRequest request) {
        var requestId = request.getHeader(HEADER);
        if (requestId != null && REQUEST_ID_PATTERN.matcher(requestId).matches()) {
            return requestId;
        }
        return UUID.randomUUID().toString();
    }
}
