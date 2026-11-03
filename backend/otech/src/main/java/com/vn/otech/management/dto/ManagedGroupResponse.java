package com.vn.otech.management.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record ManagedGroupResponse(
        UUID id, String name, String ownerName, boolean hidden, LocalDateTime createdAt) {
}
