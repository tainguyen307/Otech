package com.vn.otech.billing.dto;

import com.vn.otech.billing.entity.PaymentRequestStatus;
import jakarta.validation.constraints.Size;

public record BillingPaymentDecisionRequest(
        PaymentRequestStatus status,
        @Size(max = 500) String reviewNote) {
}
