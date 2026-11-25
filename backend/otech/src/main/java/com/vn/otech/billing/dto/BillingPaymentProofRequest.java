package com.vn.otech.billing.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record BillingPaymentProofRequest(
        @Size(max = 120) String transferReference,
        @Size(max = 2000) @Pattern(regexp = "^https://[^\\s]+$", message = "Proof image URLs must use HTTPS")
                String proofImageUrl) {
}
