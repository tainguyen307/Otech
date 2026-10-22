package com.vn.otech.auth.validation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PasswordLengthValidatorTest {
    private final PasswordLengthValidator validator = new PasswordLengthValidator();

    @Test
    void acceptsPasswordsWithinCharacterAndBcryptByteLimits() {
        assertTrue(validator.isValid("password", null));
        assertTrue(validator.isValid(" mật khẩu ", null));
    }

    @Test
    void rejectsPasswordsBelowMinimumCodePointCount() {
        assertFalse(validator.isValid("short", null));
    }

    @Test
    void rejectsPasswordsAboveBcryptUtf8ByteLimit() {
        assertFalse(validator.isValid("😀".repeat(19), null));
    }
}
