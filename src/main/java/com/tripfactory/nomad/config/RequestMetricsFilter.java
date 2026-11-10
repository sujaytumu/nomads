package com.tripfactory.nomad.config;

import java.io.IOException;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerMapping;

import com.tripfactory.nomad.service.util.RequestMetrics;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/** Times every API request for the admin system health page. */
@Component
@Order(2)
@RequiredArgsConstructor
public class RequestMetricsFilter extends OncePerRequestFilter {

    private final RequestMetrics metrics;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return !uri.startsWith("/api/") || uri.startsWith("/api/admin/system");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        long start = System.nanoTime();
        int status = 500;
        try {
            chain.doFilter(request, response);
            status = response.getStatus();
        } finally {
            Object pattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
            String route = request.getMethod() + " " + (pattern != null ? pattern : "(unmatched)");
            metrics.record(route, status, (System.nanoTime() - start) / 1_000_000);
        }
    }
}
