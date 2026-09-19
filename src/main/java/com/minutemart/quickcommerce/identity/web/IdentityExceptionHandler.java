package com.minutemart.quickcommerce.identity.web;

import com.minutemart.quickcommerce.identity.domain.IdentityError;
import com.minutemart.quickcommerce.identity.domain.IdentityException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice(basePackageClasses = IdentityController.class)
public class IdentityExceptionHandler {

    @ExceptionHandler(IdentityException.class)
    ProblemDetail handleIdentityException(IdentityException exception, HttpServletRequest request) {
        IdentityError error = exception.error();
        HttpStatus status = statusFor(error);
        return IdentityHttpResponses.problem(status, error.name(), titleFor(error), detailFor(error), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleValidation(MethodArgumentNotValidException exception, HttpServletRequest request) {
        ProblemDetail problem = IdentityHttpResponses.problem(
                HttpStatus.BAD_REQUEST,
                "AUTH_INVALID_REQUEST",
                "Invalid request",
                "One or more request fields are invalid.",
                request
        );
        problem.setProperty("errors", exception.getBindingResult().getFieldErrors().stream()
                .map(error -> Map.of("field", error.getField(), "message", error.getDefaultMessage()))
                .toList());
        return problem;
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpectedException(Exception exception, HttpServletRequest request) {
        return IdentityHttpResponses.problem(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "AUTH_INTERNAL_ERROR",
                "Authentication service error",
                "The request could not be completed.",
                request
        );
    }

    private HttpStatus statusFor(IdentityError error) {
        return switch (error) {
            case IDENTITY_BLOCKED, IDENTITY_DELETED, INVALID_CREDENTIALS,
                    SESSION_INVALID, REFRESH_TOKEN_INVALID -> HttpStatus.UNAUTHORIZED;
            case ROLE_NOT_ACTIVE, ROLE_SELECTION_REQUIRED -> HttpStatus.FORBIDDEN;
            case IDENTITY_NOT_FOUND, OTP_CHALLENGE_NOT_FOUND, INVITATION_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case OTP_RESEND_TOO_SOON -> HttpStatus.TOO_MANY_REQUESTS;
            case ROLE_ALREADY_ASSIGNED, OTP_ATTEMPTS_EXCEEDED, INVITATION_ALREADY_ACCEPTED -> HttpStatus.CONFLICT;
            default -> HttpStatus.BAD_REQUEST;
        };
    }

    private String titleFor(IdentityError error) {
        return switch (error) {
            case INVALID_CREDENTIALS, SESSION_INVALID, REFRESH_TOKEN_INVALID -> "Authentication failed";
            case ROLE_NOT_ACTIVE, ROLE_SELECTION_REQUIRED -> "Authorization failed";
            case OTP_RESEND_TOO_SOON -> "OTP request rate limited";
            default -> "Identity request failed";
        };
    }

    private String detailFor(IdentityError error) {
        return switch (error) {
            case INVALID_PHONE_NUMBER -> "The phone number must use E.164 format.";
            case OTP_EXPIRED -> "The OTP has expired.";
            case OTP_INVALID -> "The OTP is invalid.";
            case OTP_ATTEMPTS_EXCEEDED -> "The OTP verification limit has been exceeded.";
            case OTP_RESEND_TOO_SOON -> "Please wait before requesting another OTP.";
            case ROLE_NOT_ACTIVE -> "The requested role is not active for this identity.";
            case ROLE_SELECTION_REQUIRED -> "Select one active role and scope.";
            case IDENTITY_BLOCKED, IDENTITY_DELETED -> "The identity cannot authenticate.";
            default -> "The identity request could not be completed.";
        };
    }
}
