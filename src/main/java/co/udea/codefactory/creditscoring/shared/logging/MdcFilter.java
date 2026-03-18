package co.udea.codefactory.creditscoring.shared.logging;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Servlet filter that populates the MDC with a {@code traceId} for every request.
 *
 * <p>If the incoming request includes an {@code X-Trace-Id} header (e.g., from an
 * API gateway or load balancer), that value is reused. Otherwise, a new UUID is
 * generated. The traceId is also set as a response header for client-side correlation.</p>
 *
 * <p>Runs at highest precedence to ensure the traceId is available for all downstream
 * filters, controllers, and exception handlers.</p>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MdcFilter extends OncePerRequestFilter {

    private static final String TRACE_ID_HEADER = "X-Trace-Id";
    private static final String TRACE_ID_MDC_KEY = "traceId";
    private static final String REQUEST_PATH_MDC_KEY = "requestPath";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String traceId = Optional.ofNullable(request.getHeader(TRACE_ID_HEADER))
                .filter(h -> !h.isBlank())
                .orElse(UUID.randomUUID().toString());

        MDC.put(TRACE_ID_MDC_KEY, traceId);
        MDC.put(REQUEST_PATH_MDC_KEY, request.getMethod() + " " + request.getRequestURI());
        response.setHeader(TRACE_ID_HEADER, traceId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }
}
