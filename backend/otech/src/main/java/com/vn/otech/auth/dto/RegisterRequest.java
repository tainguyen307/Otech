package com.vn.otech.auth.dto;

import com.vn.otech.auth.validation.PasswordLength;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Size(max = 255) String fullName,
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank
        @PasswordLength
        @Pattern(
                regexp = "^(?=.*\\p{Ll})(?=.*\\p{Lu})(?=.*\\p{Nd})(?=.*[^\\p{L}\\p{N}\\s]).+$",
                message = "Password must contain an uppercase letter, a lowercase letter, a digit, and a special character")
        String password) {
}