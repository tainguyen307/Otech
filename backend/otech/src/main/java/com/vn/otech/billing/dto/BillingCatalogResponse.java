package com.vn.otech.billing.dto;

import java.util.List;

public record BillingCatalogResponse(
        List<BillingPackageResponse> packages,
        String qrImageUrl,
        int freeListingsPerDay,
        int freeListingsUsedToday,
        long availableListingCredits) {
}
