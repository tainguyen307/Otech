package com.vn.otech.management.dto;

import com.vn.otech.entity.ReportStatus;
import com.vn.otech.entity.ReportTargetType;
import java.time.LocalDateTime;
import java.util.UUID;

public record ManagedReportResponse(
        UUID id,
        ReportTargetType targetType,
        UUID targetId,
        String reason,
        String reporterName,
        ReportStatus status,
        LocalDateTime createdAt) {
}
