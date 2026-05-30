package com.pos.dto.invoice;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Yêu cầu hủy hóa đơn — bắt buộc lý do (audit chống lạm dụng void). */
public record CancelInvoiceRequest(
        @NotBlank(message = "Phải nhập lý do hủy")
        @Size(min = 3, max = 255, message = "Lý do hủy 3–255 ký tự")
        String reason
) {}
