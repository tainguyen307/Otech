package com.vn.otech.management.dto;

import jakarta.validation.constraints.NotNull;

public record ContentVisibilityRequest(@NotNull Boolean hidden) {
}
