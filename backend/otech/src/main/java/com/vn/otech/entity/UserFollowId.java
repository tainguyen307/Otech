package com.vn.otech.entity;

import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import java.io.Serializable;
import java.util.UUID;

@Embeddable
@EqualsAndHashCode
public class UserFollowId implements Serializable {
    private UUID followerId;
    private UUID followingId;

    public UserFollowId() {
    }

    public UserFollowId(UUID followerId, UUID followingId) {
        this.followerId = followerId;
        this.followingId = followingId;
    }
}
