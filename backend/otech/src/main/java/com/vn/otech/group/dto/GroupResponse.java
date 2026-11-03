package com.vn.otech.group.dto;

import com.vn.otech.entity.GroupVisibility;
import java.time.LocalDateTime;
import java.util.UUID;

public record GroupResponse(
        UUID id,
        String name,
        String description,
        String avatarUrl,
        String coverUrl,
        UUID categoryId,
        String categoryName,
        GroupVisibility visibility,
        boolean requirePostApproval,
        String ownerName,
        long memberCount,
        long postCount,
        String viewerRole,
        boolean member,
        String joinRequestStatus,
        LocalDateTime createdAt) {
}
