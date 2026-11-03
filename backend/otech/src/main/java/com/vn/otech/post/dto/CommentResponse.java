package com.vn.otech.post.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record CommentResponse(
        UUID id,
        UUID parentCommentId,
        UUID authorId,
        String authorName,
        String authorAvatar,
        String content,
        LocalDateTime createdAt) {
}
