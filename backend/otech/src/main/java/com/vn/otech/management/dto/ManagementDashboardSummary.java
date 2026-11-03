package com.vn.otech.management.dto;

public record ManagementDashboardSummary(
        long users,
        long activeUsers,
        long lockedUsers,
        long products,
        long hiddenProducts,
        long posts,
        long hiddenPosts,
        long groups,
        long hiddenGroups,
        long pendingReports,
        long managers) {
}
