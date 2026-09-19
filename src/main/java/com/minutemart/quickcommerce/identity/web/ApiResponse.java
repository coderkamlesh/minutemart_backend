package com.minutemart.quickcommerce.identity.web;

import java.util.Map;

public record ApiResponse<T>(
        String code,
        String message,
        T data,
        Map<String, Object> meta
) {
}
