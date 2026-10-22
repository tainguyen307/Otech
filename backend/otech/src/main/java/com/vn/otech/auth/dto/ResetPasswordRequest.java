package com.vn.otech.auth.dto;

import com.vn.otech.auth.validation.PasswordLength;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ResetPasswordRequest(
        @NotBlank @Pattern(regexp = "\\d{6}", message = "Reset code must contain 6 digits") String token,
        @NotBlank
        @PasswordLength
        @Pattern(
                regexp = "^(?=.*\\p{Ll})(?=.*\\p{Lu})(?=.*\\p{Nd})(?=.*[^\\p{L}\\p{N}\\s]).+$",
                message = "Password must contain an uppercase letter, a lowercase letter, a digit, and a special character")
        String newPassword) {
}