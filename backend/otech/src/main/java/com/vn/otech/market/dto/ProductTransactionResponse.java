package com.vn.otech.market.dto;

import com.vn.otech.market.entity.ProductTransactionStatus;
import java.time.LocalDateTime;
import java.util.UUID;

public record ProductTransactionResponse(
        UUID id,
        UUID productId,
        String productTitle,
        ProductTransactionStatus status,
        boolean currentUserIsBuyer,
        boolean currentUserIsSeller,
        boolean currentUserConfirmed,
        boolean otherPartyConfirmed,
        boolean reviewSubmitted,
        LocalDateTime completedAt) {
}
