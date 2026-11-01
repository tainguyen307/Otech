package com.vn.otech.management.dto;

import com.vn.otech.entity.ReportStatus;
import jakarta.validation.constraints.NotNull;

public record ReportDecisionRequest(@NotNull ReportStatus status) {
}
