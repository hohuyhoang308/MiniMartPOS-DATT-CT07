package com.pos.dto.integration;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TelegramRecipientRequest(
        @NotBlank(message = "Chat ID không được để trống") @Size(max = 50) String chatId,
        @Size(max = 100) String label
) {}
