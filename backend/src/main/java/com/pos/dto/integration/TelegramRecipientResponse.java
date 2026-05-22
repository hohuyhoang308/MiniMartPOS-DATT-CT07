package com.pos.dto.integration;

import com.pos.entity.TelegramRecipient;

public record TelegramRecipientResponse(Long id, String chatId, String label, Boolean isActive) {

    public static TelegramRecipientResponse from(TelegramRecipient r) {
        return new TelegramRecipientResponse(r.getId(), r.getChatId(), r.getLabel(), r.getIsActive());
    }
}
