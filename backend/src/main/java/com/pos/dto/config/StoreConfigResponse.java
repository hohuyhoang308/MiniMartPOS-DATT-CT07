package com.pos.dto.config;

import com.pos.entity.StoreConfig;

/** Trả cấu hình; token nhạy cảm chỉ trả cờ "đã cấu hình" thay vì giá trị thật. */
public record StoreConfigResponse(
        String name, String address, String phone, String taxCode, String logoUrl,
        String bankName, String bankBin, String bankAccountNo, String bankAccountName, String transferPrefix,
        boolean web2mConfigured, boolean telegramConfigured,
        Boolean telegramEnabled, Boolean notifyPayment, Boolean notifyLowStock, Boolean notifyNewInvoice
) {
    public static StoreConfigResponse from(StoreConfig c) {
        return new StoreConfigResponse(
                c.getName(), c.getAddress(), c.getPhone(), c.getTaxCode(), c.getLogoUrl(),
                c.getBankName(), c.getBankBin(), c.getBankAccountNo(), c.getBankAccountName(), c.getTransferPrefix(),
                c.getWeb2mApiUrl() != null && !c.getWeb2mApiUrl().isBlank(),
                c.getTelegramBotToken() != null && !c.getTelegramBotToken().isBlank(),
                c.getTelegramEnabled(), c.getNotifyPayment(), c.getNotifyLowStock(), c.getNotifyNewInvoice());
    }
}
