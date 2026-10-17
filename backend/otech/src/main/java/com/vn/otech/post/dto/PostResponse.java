package com.vn.otech.post.dto;

import java.util.UUID;

public record PostResponse(
        UUID id,
        UUID authorId,
        String name,
        String handle,
        String time,
        String avatar,
        String image,
        String title,
        String copy,
        String price,
        Integer likes,
        Integer comments,
        String tag) {
}
