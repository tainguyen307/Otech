package com.vn.otech.entity;

import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import java.io.Serializable;
import java.util.UUID;

@Embeddable
@EqualsAndHashCode
public class PostLikeId implements Serializable {
    private UUID userId;
    private UUID postId;

    public PostLikeId() {
    }

    public PostLikeId(UUID userId, UUID postId) {
        this.userId = userId;
        this.postId = postId;
    }
}
