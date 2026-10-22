package com.vn.otech.auth.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.nio.charset.StandardCharsets;

public class PasswordLengthValidator implements ConstraintValidator<PasswordLength, String> {
    private static final int MIN_CODE_POINTS = 8;
    private static final int MAX_UTF8_BYTES = 72;

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        if (password == null) {
            return true;
        }
        int codePointCount = password.codePointCount(0, password.length());
        return codePointCount >= MIN_CODE_POINTS
                && password.getBytes(StandardCharsets.UTF_8).length <= MAX_UTF8_BYTES;
    }
}
