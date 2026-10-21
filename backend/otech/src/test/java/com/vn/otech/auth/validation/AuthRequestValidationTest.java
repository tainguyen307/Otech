package com.vn.otech.auth.validation;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vn.otech.auth.dto.AuthRequest;
import com.vn.otech.auth.dto.RegisterRequest;
import com.vn.otech.auth.dto.ResetPasswordRequest;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.Test;

class AuthRequestValidationTest {
    @Test
    void loginRejectsMalformedEmailButAllowsExistingPasswordPolicy() {
        assertInvalid(new AuthRequest("not-an-email", "password123"), "email");
        assertValid(new AuthRequest("user@example.com", "password123"));
    }

    @Test
    void signupRejectsBlankFullNameAndPasswordsOverBcryptByteLimit() {
        assertInvalid(new RegisterRequest(" ", "user@example.com", "😀".repeat(19)),
                "fullName", "password");
    }

    @Test
    void passwordResetRequiresSixDigitToken() {
        assertInvalid(new ResetPasswordRequest("not-a-code", "password123"), "token");
    }

    @Test
    void signupRequiresUppercaseLowercaseDigitAndSpecialCharacter() {
        assertInvalid(new RegisterRequest("Test User", "user@example.com", "password1!"), "password");
        assertInvalid(new RegisterRequest("Test User", "user@example.com", "PASSWORD1!"), "password");
        assertInvalid(new RegisterRequest("Test User", "user@example.com", "Password!"), "password");
        assertInvalid(new RegisterRequest("Test User", "user@example.com", "Password1"), "password");
    }

    @Test
    void signupAcceptsPasswordWithAllRequiredCharacterTypes() {
        assertValid(new RegisterRequest("Test User", "user@example.com", "Password1!"));
    }

    private void assertInvalid(Object request, String... fields) {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();
            var invalidFields = validator.validate(request).stream()
                    .map(violation -> violation.getPropertyPath().toString())
                    .collect(java.util.stream.Collectors.toSet());
            for (String field : fields) {
                assertTrue(invalidFields.contains(field), "Expected validation error for " + field);
            }
        }
    }

    private void assertValid(Object request) {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            assertTrue(factory.getValidator().validate(request).isEmpty());
        }
    }
}
