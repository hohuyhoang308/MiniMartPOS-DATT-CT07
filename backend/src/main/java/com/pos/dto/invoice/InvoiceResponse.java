package com.pos.dto.invoice;

import com.pos.entity.Invoice;
import com.pos.entity.PaymentTransaction;
import com.pos.entity.enums.InvoiceStatus;
import com.pos.entity.enums.PaymentMethod;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Hóa đơn đầy đủ (dùng cho cả sau khi tạo & xem chi tiết). */
public record InvoiceResponse(
        Long id,
        String code,
        Long shiftId,
        String cashierName,
        Long customerId,
        String customerName,
        String promotionCode,
        BigDecimal subtotal,
        BigDecimal discountAmount,
        BigDecimal totalAmount,
        PaymentMethod paymentMethod,
        BigDecimal customerPaid,
        BigDecimal changeAmount,
        Integer pointsEarned,
        Integer pointsUsed,
        InvoiceStatus status,
        LocalDateTime createdAt,
        List<Item> items,
        Payment payment
) {
    public record Item(Long productId, String productName, Integer quantity,
                       BigDecimal unitPrice, BigDecimal subtotal) {}

    /** Thông tin thanh toán QR (chỉ có khi payment_method = QR). */
    public record Payment(String transferContent, BigDecimal amount, String status, String qrUrl) {}

    public static InvoiceResponse from(Invoice inv, PaymentTransaction pt, String qrUrl) {
        List<Item> items = inv.getItems().stream()
                .map(it -> new Item(it.getProduct().getId(), it.getProduct().getName(),
                        it.getQuantity(), it.getUnitPrice(), it.getSubtotal()))
                .toList();
        Payment payment = (pt == null) ? null
                : new Payment(pt.getTransferContent(), pt.getAmount(), pt.getStatus().name(), qrUrl);
        return new InvoiceResponse(
                inv.getId(), inv.getCode(),
                inv.getShift().getId(), inv.getShift().getUser().getFullName(),
                inv.getCustomer() != null ? inv.getCustomer().getId() : null,
                inv.getCustomer() != null ? inv.getCustomer().getFullName() : null,
                inv.getPromotion() != null ? inv.getPromotion().getCode() : null,
                inv.getSubtotal(), inv.getDiscountAmount(), inv.getTotalAmount(),
                inv.getPaymentMethod(), inv.getCustomerPaid(), inv.getChangeAmount(),
                inv.getPointsEarned(), inv.getPointsUsed(), inv.getStatus(), inv.getCreatedAt(),
                items, payment);
    }

    public static InvoiceResponse from(Invoice inv) {
        return from(inv, null, null);
    }
}
