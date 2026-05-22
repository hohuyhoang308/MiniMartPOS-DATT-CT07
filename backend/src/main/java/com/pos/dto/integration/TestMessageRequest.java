package com.pos.dto.integration;

import jakarta.validation.constraints.NotBlank;

/** Gửi tin nhắn Telegram thử (FR-A5). */
public record TestMessageRequest(
        @NotBlank(message = "Phải có Chat ID nhận thử") String chatId,
        String text
) {}
