package com.vn.otech.billing.dto;

import com.vn.otech.billing.entity.BillingPackageType;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record BillingPackageRequest(
        @NotBlank @Size(max = 80) String code,
        @NotBlank @Size(max = 150) String name,
        @Size(max = 1000) String description,
        @NotNull BillingPackageType packageType,
        @NotNull @Positive @Digits(integer = 13, fraction = 2) BigDecimal price,
        @Min(1) Integer listingCredits,
        @NotNull @Min(1) Integer durationHours,
        boolean active) {
}
