package com.vn.otech.market.dto;

import com.vn.otech.entity.ProductStatus;
import jakarta.validation.constraints.NotNull;

public record ProductStatusRequest(@NotNull ProductStatus status) {
}
