package com.vn.otech.entity;

import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import java.io.Serializable;
import java.util.UUID;

@Embeddable
@EqualsAndHashCode
public class PageMemberId implements Serializable {
    private UUID pageId;
    private UUID userId;

    public PageMemberId() {
    }

    public PageMemberId(UUID pageId, UUID userId) {
        this.pageId = pageId;
        this.userId = userId;
    }
}
