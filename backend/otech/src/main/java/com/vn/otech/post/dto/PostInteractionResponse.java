package com.vn.otech.post.dto;

public record PostInteractionResponse(
        long likes,
        long comments,
        long shares,
        boolean likedByCurrentUser) {
}
