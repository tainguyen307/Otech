package com.vn.otech.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record MessageSendRequest(
        @NotNull UUID clientMessageId,
        @NotBlank @Size(max = 5000) String content) {
}
