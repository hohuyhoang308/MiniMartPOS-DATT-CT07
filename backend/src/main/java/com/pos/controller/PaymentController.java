package com.pos.controller;

import com.pos.common.ApiResponse;
import com.pos.dto.payment.PaymentInfoResponse;
import com.pos.service.PaymentService;
import com.pos.service.Web2mSyncService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** Thanh toán QR + đối soát WEB2M (FR-A1, FR-A4). */
@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final Web2mSyncService web2mSyncService;

    public PaymentController(PaymentService paymentService, Web2mSyncService web2mSyncService) {
        this.paymentService = paymentService;
        this.web2mSyncService = web2mSyncService;
    }

    /** Hiển thị VietQR cho hóa đơn (trả URL ảnh QR + nội dung CK). */
    @GetMapping("/qr")
    public ApiResponse<PaymentInfoResponse> qr(@RequestParam Long invoiceId) {
        return ApiResponse.ok(paymentService.getQr(invoiceId));
    }

    /** FE poll trạng thái thanh toán. */
    @GetMapping("/{invoiceId}/status")
    public ApiResponse<PaymentInfoResponse> status(@PathVariable Long invoiceId) {
        return ApiResponse.ok(paymentService.getStatus(invoiceId));
    }

    /** Thu ngân xác nhận ĐÃ NHẬN tiền QR (khi chưa khớp tự động qua WEB2M) → hoàn tất hóa đơn. */
    @PostMapping("/{invoiceId}/confirm")
    public ApiResponse<PaymentInfoResponse> confirm(@PathVariable Long invoiceId) {
        return ApiResponse.ok("Đã xác nhận thanh toán", paymentService.confirmPaid(invoiceId));
    }

    /** Kích hoạt đối soát WEB2M thủ công (bình thường chạy job nền). */
    @PostMapping("/web2m/sync")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<Map<String, Integer>> sync() {
        int matched = web2mSyncService.sync();
        return ApiResponse.ok("Đối soát WEB2M xong", Map.of("matched", matched));
    }
}
