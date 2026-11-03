package com.vn.otech.group.dto;

import com.vn.otech.entity.GroupVisibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record GroupRequest(
        @NotBlank @Size(max = 255) String name,
        @Size(max = 2000) String description,
        @NotNull UUID categoryId,
        @NotNull GroupVisibility visibility,
        Boolean requirePostApproval,
        @Size(max = 2000) @Pattern(regexp = "^https://[^\\s]+$", message = "Image URLs must use HTTPS")
        String avatarUrl,
        @Size(max = 2000) @Pattern(regexp = "^https://[^\\s]+$", message = "Image URLs must use HTTPS")
        String coverUrl) {
}
