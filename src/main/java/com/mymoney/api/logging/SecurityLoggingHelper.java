package com.mymoney.api.logging;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.util.HtmlUtils;

public final class SecurityLoggingHelper {

    private SecurityLoggingHelper() {}

    public static String sanitize(String value) {
        return value == null ? null : HtmlUtils.htmlEscape(value);
    }

    public static String sanitizePath(HttpServletRequest request) {
        return sanitize(request.getRequestURI());
    }

    public static String currentUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            return "anonymous";
        }
        return authentication.getName();
    }
}
