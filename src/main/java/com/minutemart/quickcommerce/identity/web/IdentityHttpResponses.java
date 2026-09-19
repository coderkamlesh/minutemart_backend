package com.minutemart.quickcommerce.identity.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import java.net.URI;
import java.util.UUID;

final class IdentityHttpResponses {

    private IdentityHttpResponses() {
    }

    static ProblemDetail problem(
            HttpStatus status,
            String code,
            String title,
            String detail,
            HttpServletRequest request
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setType(URI.create("https://api.minutemart.com/problems/" + code.toLowerCase()));
        problem.setProperty("code", code);
        problem.setProperty("traceId", traceId(request));
        return problem;
    }

    static String traceId(HttpServletRequest request) {
        String supplied = request.getHeader("X-Trace-Id");
        return supplied == null || supplied.isBlank() ? UUID.randomUUID().toString() : supplied;
    }
}
