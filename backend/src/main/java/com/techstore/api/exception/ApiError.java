package com.techstore.api.exception;

import java.time.OffsetDateTime;
import java.util.List;

public record ApiError(
        OffsetDateTime timestamp,
        int status,
        String error,
        String message,
        String path,
        List<FieldErrorDetail> details
) {
    public record FieldErrorDetail(String field, String message) {}
}
