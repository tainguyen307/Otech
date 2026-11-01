package com.vn.otech.management.dto;

import com.vn.otech.entity.ProductStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record ManagedProductResponse(
        UUID id,
        String title,
        String sellerName,
        BigDecimal price,
        ProductStatus status,
        LocalDateTime createdAt) {
}
