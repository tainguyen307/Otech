package com.vn.otech.management.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record ManagedPostResponse(
        UUID id, String content, String authorName, boolean hidden, LocalDateTime createdAt) {
}
