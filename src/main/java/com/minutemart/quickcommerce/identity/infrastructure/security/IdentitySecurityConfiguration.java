package com.minutemart.quickcommerce.identity.infrastructure.security;

import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;
import java.util.Map;

@Configuration
public class IdentitySecurityConfiguration {

    @Bean
    SecurityFilterChain identitySecurityFilterChain(
            HttpSecurity http,
            IdentityBearerAuthenticationFilter bearerFilter,
            ObjectMapper objectMapper
    ) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> {
                })
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .logout(logout -> logout.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(problemEntryPoint(objectMapper))
                        .accessDeniedHandler(problemAccessDeniedHandler(objectMapper)))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                "/actuator/**",
                                "/api/v1/auth/otp/challenges",
                                "/api/v1/auth/otp/verify",
                                "/api/v1/auth/password/login",
                                "/api/v1/auth/sessions/refresh",
                                "/api/v1/auth/invitations/accept"
                        ).permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(bearerFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    private AuthenticationEntryPoint problemEntryPoint(ObjectMapper objectMapper) {
        return (request, response, exception) -> writeProblem(
                objectMapper, request, response, HttpStatus.UNAUTHORIZED,
                "AUTHENTICATION_REQUIRED", "Authentication required", "A valid access token is required.");
    }

    private AccessDeniedHandler problemAccessDeniedHandler(ObjectMapper objectMapper) {
        return (request, response, exception) -> writeProblem(
                objectMapper, request, response, HttpStatus.FORBIDDEN,
                "AUTHORIZATION_REQUIRED", "Authorization failed", "The identity is not permitted to perform this action.");
    }

    private void writeProblem(
            ObjectMapper objectMapper,
            HttpServletRequest request,
            HttpServletResponse response,
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
