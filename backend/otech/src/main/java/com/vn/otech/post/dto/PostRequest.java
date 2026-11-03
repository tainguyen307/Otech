package com.vn.otech.post.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PostRequest(
        @NotBlank @Size(max = 5000) String content,
        @Size(max = 2000)
        @Pattern(regexp = "^https://[^\\s]+$", message = "Image URLs must use HTTPS")
        String imageUrl) {
}
