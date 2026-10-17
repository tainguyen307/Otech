package com.vn.otech.market.dto;

import com.vn.otech.entity.ProductCondition;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ProductRequest(
        @NotBlank @Size(max = 255) String title,
        @Size(max = 10000) String description,
        @NotNull @DecimalMin("0.01") @Digits(integer = 13, fraction = 2) BigDecimal price,
        UUID categoryId,
        @NotNull ProductCondition condition,
        @Size(max = 255) String location,
        @Size(max = 10) List<@NotBlank @Size(max = 2000)
                @Pattern(regexp = "^https://[^\\s]+$", message = "Image URLs must use HTTPS") String> imageUrls) {
}
