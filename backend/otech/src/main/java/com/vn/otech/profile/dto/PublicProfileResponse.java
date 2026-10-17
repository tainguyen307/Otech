package com.vn.otech.profile.dto;

import com.vn.otech.post.dto.PostResponse;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record PublicProfileResponse(
        UUID userId,
        String fullName,
        String avatarUrl,
        String bio,
        LocalDateTime joinedAt,
        long postCount,
        long followerCount,
        long followingCount,
        boolean followedByCurrentUser,
        List<PostResponse> posts) {
}
