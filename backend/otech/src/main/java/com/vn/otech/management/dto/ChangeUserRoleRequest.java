package com.vn.otech.management.dto;

import com.vn.otech.entity.RoleName;
import jakarta.validation.constraints.NotNull;

public record ChangeUserRoleRequest(@NotNull RoleName role) {
}
