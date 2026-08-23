package com.vn.otech.entity;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.util.UUID;

@Embeddable @NoArgsConstructor @AllArgsConstructor @EqualsAndHashCode
public class PageFollowId implements Serializable {
    private UUID pageId;
    private UUID userId;
}
