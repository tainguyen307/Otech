package com.vn.otech.profile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProfileUpdateRequest(
        @NotBlank @Size(max = 255) String fullName,
        @Size(max = 2000) String bio,
        @Size(max = 2000) String contactInfo,
        String oldPassword,
        @Size(min = 8, max = 72) String newPassword,
        String confirmPassword) {
}
