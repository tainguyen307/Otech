package com.vn.otech.group.dto;

import com.vn.otech.entity.GroupPostStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record GroupPostResponse(
        UUID id,
        UUID groupId,
        UUID authorId,
        String authorName,
        String authorAvatarUrl,
        String content,
        List<String> imageUrls,
        GroupPostStatus status,
        LocalDateTime createdAt) {
}
