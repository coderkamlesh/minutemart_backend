package com.minutemart.quickcommerce.identity.infrastructure.security;

import tools.jackson.databind.ObjectMapper;
import com.minutemart.quickcommerce.identity.api.AuthenticatedIdentity;
import com.minutemart.quickcommerce.identity.api.IdentityAuthentication;
import com.minutemart.quickcommerce.identity.domain.IdentityException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Component
public class IdentityBearerAuthenticationFilter extends OncePerRequestFilter {

    @org.springframework.context.annotation.Bean
    static FilterRegistrationBean<IdentityBearerAuthenticationFilter> disableContainerRegistration(
            IdentityBearerAuthenticationFilter filter
    ) {
        FilterRegistrationBean<IdentityBearerAuthenticationFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    private final IdentityAuthentication authentication;
    private final ObjectMapper objectMapper;

    public IdentityBearerAuthenticationFilter(
            IdentityAuthentication authentication,
            ObjectMapper objectMapper
    ) {
        this.authentication = authentication;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring("Bearer ".length()).trim();
        if (token.isBlank()) {
            writeProblem(response, request, HttpStatus.UNAUTHORIZED,
                    "AUTH_INVALID_TOKEN", "Authentication failed", "The access token is invalid.");
            return;
        }

        try {
            AuthenticatedIdentity identity = authentication.authenticateAccessToken(token);
            UsernamePasswordAuthenticationToken current = UsernamePasswordAuthenticationToken.authenticated(
                    identity,
                    null,
                    List.of(
                            new SimpleGrantedAuthority("ROLE_" + identity.role().name()),
                            new SimpleGrantedAuthority("SCOPE_" + identity.role().name())
                    )
            );
            SecurityContextHolder.getContext().setAuthentication(current);
            filterChain.doFilter(request, response);
        } catch (IdentityException exception) {
            writeProblem(response, request, HttpStatus.UNAUTHORIZED,
                    "AUTH_INVALID_TOKEN", "Authentication failed", "The access token is invalid.");
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private void writeProblem(
            HttpServletResponse response,
            HttpServletRequest request,
            HttpStatus status,
            String code,
            String title,
            String detail
    ) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), Map.of(
                "type", "https://api.minutemart.com/problems/" + code.toLowerCase(),
                "title", title,
                "status", status.value(),
                "detail", detail,
                "code", code,
                "traceId", traceId(request)
        ));
    }

    private String traceId(HttpServletRequest request) {
        String traceId = request.getHeader("X-Trace-Id");
        return traceId == null || traceId.isBlank() ? request.getRequestId() : traceId;
    }
}
