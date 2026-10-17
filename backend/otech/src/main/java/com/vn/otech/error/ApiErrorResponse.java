package com.vn.otech.error;

import java.util.List;

public record ApiErrorResponse(ErrorBody error) {
    public record ErrorBody(String code, String message, List<FieldError> details) {
    }

    public record FieldError(String field, String message) {
    }
}
