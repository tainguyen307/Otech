package com.vn.otech.billing.dto;

import com.vn.otech.billing.entity.PaymentRequestStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record BillingPaymentResponse(
        UUID id,
        String packageName,
        String packageType,
        String productTitle,
        String paymentCode,
        BigDecimal amount,
        String transferReference,
        String proofImageUrl,
        PaymentRequestStatus status,
        String reviewNote,
        String userName,
        String userEmail,
        LocalDateTime createdAt,
        LocalDateTime reviewedAt) {
}
