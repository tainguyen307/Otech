package com.vn.otech.market.repository;

import com.vn.otech.entity.ProductCondition;
import com.vn.otech.entity.ProductStatus;
import java.math.BigDecimal;
import java.util.UUID;

public record ProductSearchFilter(
        String search,
        UUID categoryId,
        UUID sellerId,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        ProductCondition condition,
        String location,
        ProductStatus status,
        String sort,
        String cursor,
        int limit) {
}
