package com.vn.otech.billing.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record BillingSettingRequest(
        @Size(max = 2000) @Pattern(regexp = "^https://[^\\s]+$", message = "QR image URL must use HTTPS")
                String qrImageUrl) {
}
