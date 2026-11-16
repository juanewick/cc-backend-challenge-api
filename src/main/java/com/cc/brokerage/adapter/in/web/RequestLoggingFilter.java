package com.cc.brokerage.adapter.in.web;

import com.cc.brokerage.config.ApiProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerMapping;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Gives every API request an id, puts it on each log line of that request and on the response, and writes one
 * line with the outcome. Status and documentation endpoints are left alone.
 */
@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    public static final String REQUEST_ID_KEY = "requestId";

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);
    private static final Pattern ACCEPTED_ID = Pattern.compile("[A-Za-z0-9_-]{1,64}");
    private static final String UNMATCHED_ROUTE = "(unmatched route)";

    private final String basePath;

    public RequestLoggingFilter(ApiProperties api) {
        this.basePath = api.basePath();
    }

    /**
     * The route pattern the request matched, for example /v1/accounts/{accountNumber}/orders. Never the URI,
     * which carries the account number.
     */
    public static String routeOf(HttpServletRequest request) {
        Object pattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        return pattern == null ? UNMATCHED_ROUTE : pattern.toString();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !path.equals(basePath) && !path.startsWith(basePath + "/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String requestId = requestIdOf(request);
        long start = System.nanoTime();
        MDC.put(REQUEST_ID_KEY, requestId);
        response.setHeader(REQUEST_ID_HEADER, requestId);
        try {
            chain.doFilter(request, response);
        } finally {
            log.info("{} {} -> {} in {} ms",
                    request.getMethod(), routeOf(request), response.getStatus(), (System.nanoTime() - start) / 1_000_000);
            MDC.remove(REQUEST_ID_KEY);
        }
    }

    /**
     * The caller's id when it is short and plain, so it can be written to the log as is; a new one otherwise.
     */
    private static String requestIdOf(HttpServletRequest request) {
        String sent = request.getHeader(REQUEST_ID_HEADER);
        if (sent != null && ACCEPTED_ID.matcher(sent).matches()) {
            return sent;
        }
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
