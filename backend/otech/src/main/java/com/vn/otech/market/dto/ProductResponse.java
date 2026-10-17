package com.vn.otech.market.dto;

import com.vn.otech.entity.ProductCondition;
import com.vn.otech.entity.ProductStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ProductResponse(
        UUID id,
        String title,
        String description,
        BigDecimal price,
        UUID categoryId,
        String categoryName,
        ProductCondition condition,
        String location,
        ProductStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<String> imageUrls,
        SellerResponse seller) {
    public record SellerResponse(UUID id, String fullName, String avatarUrl) {
    }
}
