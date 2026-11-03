package com.vn.otech.entity;

import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import java.io.Serializable;
import java.util.UUID;

@Embeddable
@EqualsAndHashCode
public class GroupMemberId implements Serializable {
    private UUID groupId;
    private UUID userId;

    public GroupMemberId() {
    }

    public GroupMemberId(UUID groupId, UUID userId) {
        this.groupId = groupId;
        this.userId = userId;
    }
}
