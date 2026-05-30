package com.pos.dto.sale;

import com.pos.entity.enums.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

/** Yêu cầu tạo hóa đơn (UC10). */
public record CreateInvoiceRequest(
        @NotEmpty(message = "Hóa đơn phải có ít nhất 1 sản phẩm")
        @Valid List<SaleItemRequest> items,
        Long customerId,            // tùy chọn — khách thân thiết
        String promotionCode,       // tùy chọn — mã giảm giá
        @NotNull(message = "Phải chọn hình thức thanh toán") PaymentMethod paymentMethod,
        BigDecimal customerPaid,    // bắt buộc khi thanh toán tiền mặt
        Integer pointsToRedeem,     // tùy chọn — số điểm khách muốn dùng để giảm trừ (cần có khách)
        String idempotencyKey       // tùy chọn — khóa chống tạo trùng khi mất phản hồi mạng (FE sinh UUID/lần thanh toán)
) {}
