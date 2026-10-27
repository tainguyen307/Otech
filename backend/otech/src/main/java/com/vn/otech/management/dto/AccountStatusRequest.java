package com.vn.otech.management.dto;

import com.vn.otech.entity.AccountStatus;
import jakarta.validation.constraints.NotNull;

public record AccountStatusRequest(@NotNull AccountStatus status) {
}
