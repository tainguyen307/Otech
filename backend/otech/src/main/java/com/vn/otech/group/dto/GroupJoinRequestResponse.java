package com.vn.otech.group.dto;

import java.util.UUID;

public record GroupJoinRequestResponse(
        UUID id,
        UUID userId,
        String fullName,
        String avatarUrl,
        String status) {
}
