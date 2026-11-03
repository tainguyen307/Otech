package com.vn.otech.group.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record GroupPostRequest(
        @NotBlank @Size(max = 5000) String content,
        @Size(max = 2000) @Pattern(regexp = "^https://[^\\s]+$", message = "Image URLs must use HTTPS")
        String imageUrl) {
}
