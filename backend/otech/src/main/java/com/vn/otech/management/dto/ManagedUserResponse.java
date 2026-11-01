package com.vn.otech.management.dto;

import com.vn.otech.entity.AccountStatus;
import com.vn.otech.entity.RoleName;
import java.time.LocalDateTime;
import java.util.UUID;

public record ManagedUserResponse(
        UUID id, String fullName, String email, RoleName role, AccountStatus status, LocalDateTime createdAt) {
}
