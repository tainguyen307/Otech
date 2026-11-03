package com.vn.otech.group.dto;

import jakarta.validation.constraints.NotNull;

public record GroupJoinDecisionRequest(@NotNull Boolean approved) {
}
