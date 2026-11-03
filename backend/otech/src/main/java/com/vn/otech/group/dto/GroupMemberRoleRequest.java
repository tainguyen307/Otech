package com.vn.otech.group.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record GroupMemberRoleRequest(
        @NotBlank @Pattern(regexp = "MEMBER|MODERATOR") String role) {
}
