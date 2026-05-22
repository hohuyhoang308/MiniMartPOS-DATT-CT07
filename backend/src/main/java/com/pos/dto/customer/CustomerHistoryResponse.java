package com.pos.dto.customer;

import java.math.BigDecimal;
import java.util.List;

/** Lịch sử mua & tổng chi tiêu của khách (FR6.2). */
public record CustomerHistoryResponse(
        Long customerId,
        String fullName,
        String phone,
        Integer loyaltyPoints,
        BigDecimal totalSpent,
        Long invoiceCount,
        List<InvoiceBrief> invoices
) {
    public record InvoiceBrief(Long id, String code, BigDecimal totalAmount, String createdAt) {}
}
