package com.vn.otech.entity;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.util.UUID;

@Embeddable @NoArgsConstructor @AllArgsConstructor @EqualsAndHashCode
public class PostLikeId implements Serializable {
    private UUID userId;
    private UUID postId;
}
