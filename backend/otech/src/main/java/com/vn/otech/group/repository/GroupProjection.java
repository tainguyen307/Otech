package com.vn.otech.group.repository;

import java.time.LocalDateTime;
import java.util.UUID;

public interface GroupProjection {
    UUID getId();
    String getName();
    String getDescription();
    String getAvatarUrl();
    String getCoverUrl();
    UUID getCategoryId();
    String getCategoryName();
    String getVisibility();
    Boolean getRequirePostApproval();
    String getOwnerName();
    Long getMemberCount();
    Long getPostCount();
    LocalDateTime getCreatedAt();
}
