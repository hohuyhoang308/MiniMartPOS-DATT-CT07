package com.pos.dto.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Cập nhật cấu hình cửa hàng + ngân hàng + tích hợp (UC20/UC24). */
public record StoreConfigRequest(
        @NotBlank(message = "Tên cửa hàng không được để trống") @Size(max = 150) String name,
        @Size(max = 255) String address,
        @Size(max = 20) String phone,
        @Size(max = 30) String taxCode,
        @Size(max = 255) String logoUrl,
        // Ngân hàng (VietQR)
        @Size(max = 50) String bankName,
        @Size(max = 20) String bankBin,
        @Size(max = 30) String bankAccountNo,
        @Size(max = 100) String bankAccountName,
        @Size(max = 20) String transferPrefix,
        // Tích hợp (nhạy cảm)
        @Size(max = 255) String web2mApiUrl,
        @Size(max = 255) String telegramBotToken,
        Boolean telegramEnabled,
        Boolean notifyPayment,
        Boolean notifyLowStock,
        Boolean notifyNewInvoice
) {}
