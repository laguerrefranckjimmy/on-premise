package com.example;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Filter that generates or propagates a Correlation ID for each HTTP request.
 * The ID is stored in the SLF4J MDC and added to the response headers.
 */
@Component
public class CorrelationIdFilter extends OncePerRequestFilter {

    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // Get correlation ID from request header or generate a new one
        String correlationId = request.getHeader(CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.isEmpty()) {
            correlationId = UUID.randomUUID().toString();
        }

        // Put correlation ID into MDC so that it can be used in logging
        MDC.put("correlationId", correlationId);

        // Add correlation ID to response header
        response.setHeader(CORRELATION_ID_HEADER, correlationId);

        try {
            // Continue the filter chain
            filterChain.doFilter(request, response);
        } finally {
            // Remove correlation ID from MDC after request is complete
            MDC.remove("correlationId");
        }
    }
}
