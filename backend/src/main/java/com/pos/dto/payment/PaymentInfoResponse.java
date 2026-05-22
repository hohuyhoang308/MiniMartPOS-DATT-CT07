package com.pos.dto.payment;

import java.math.BigDecimal;

/** Thông tin thanh toán QR cho FE hiển thị & poll trạng thái. */
public record PaymentInfoResponse(
        Long invoiceId,
        String invoiceCode,
        BigDecimal amount,
        String transferContent,
        String qrUrl,
        String status      // PENDING / PAID / EXPIRED / FAILED / NONE
) {}
