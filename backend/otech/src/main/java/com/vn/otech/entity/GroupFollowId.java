package com.vn.otech.entity;

import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import java.io.Serializable;
import java.util.UUID;

@Embeddable
@EqualsAndHashCode
public class GroupFollowId implements Serializable {
    private UUID groupId;
    private UUID userId;

    public GroupFollowId() {
    }

    public GroupFollowId(UUID groupId, UUID userId) {
        this.groupId = groupId;
        this.userId = userId;
    }
}
