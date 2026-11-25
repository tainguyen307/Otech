package com.vn.otech.billing.dto;

import com.vn.otech.billing.entity.BillingPackageType;
import java.math.BigDecimal;
import java.util.UUID;

public record BillingPackageResponse(
        UUID id,
        String code,
        String name,
        String description,
        BillingPackageType packageType,
        BigDecimal price,
        Integer listingCredits,
        Integer durationHours,
        boolean active) {
}
