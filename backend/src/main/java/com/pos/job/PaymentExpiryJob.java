package com.pos.job;

import com.pos.service.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Dọn rác giao dịch QR: định kỳ chuyển các giao dịch còn PENDING nhưng đã quá hạn → EXPIRED.
 * Chạy độc lập với job đối soát WEB2M (không phụ thuộc app.web2m.enabled) vì đây chỉ là housekeeping,
 * không gọi API ngoài. Tránh để v_pending_payments tích lũy giao dịch treo vô thời hạn.
 */
@Component
public class PaymentExpiryJob {

    private static final Logger log = LoggerFactory.getLogger(PaymentExpiryJob.class);

    private final PaymentService paymentService;

    public PaymentExpiryJob(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @Scheduled(fixedDelay = 300_000, initialDelay = 60_000) // mỗi 5 phút, hoãn 1 phút sau khởi động
    public void expirePending() {
        try {
            int expired = paymentService.expireStalePending();
            if (expired > 0) log.info("Đã đánh dấu hết hạn {} giao dịch QR treo", expired);
        } catch (Exception e) {
            log.debug("Bỏ qua lần dọn giao dịch hết hạn: {}", e.getMessage());
        }
    }
}
