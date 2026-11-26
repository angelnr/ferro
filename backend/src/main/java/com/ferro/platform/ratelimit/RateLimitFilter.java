package com.ferro.platform.ratelimit;

import java.io.IOException;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.ferro.platform.api.RateLimiter;
import com.ferro.shared.domain.RateLimitExceededException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** Aplica el límite de 100/min por usuario a la API autenticada (RN-20). */
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimiter rateLimiter;

    public RateLimitFilter(RateLimiter rateLimiter) {
        this.rateLimiter = rateLimiter;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!isExcluded(request.getRequestURI())) {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null
                    && authentication.isAuthenticated()
                    && !(authentication instanceof AnonymousAuthenticationToken)) {
                try {
                    rateLimiter.checkAuthenticated(authentication.getName());
                } catch (RateLimitExceededException exception) {
                    writeTooManyRequests(response, exception.retryAfterSeconds());
                    return;
                }
            }
        }
        filterChain.doFilter(request, response);
    }

    private boolean isExcluded(String path) {
        return path.startsWith("/actuator/")
                || path.equals("/api/docs")
                || path.startsWith("/api/docs/")
                || path.equals("/api/api-docs")
                || path.startsWith("/api/api-docs/");
    }

    private void writeTooManyRequests(HttpServletResponse response, long retryAfterSeconds) throws IOException {
        response.setStatus(429);
        response.setHeader("Retry-After", Long.toString(retryAfterSeconds));
        response.setContentType("application/problem+json");
        response.getWriter()
                .write("{\"type\":\"https://ferro.dev/problems/rate-limited\","
                        + "\"title\":\"Demasiadas peticiones\",\"status\":429,\"code\":\"RATE_LIMITED\"}");
    }
}
