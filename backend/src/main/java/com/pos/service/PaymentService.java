package com.pos.service;

import com.pos.dto.payment.PaymentInfoResponse;
import com.pos.entity.Invoice;
import com.pos.entity.PaymentTransaction;
import com.pos.entity.StoreConfig;
import com.pos.entity.enums.InvoiceStatus;
import com.pos.entity.enums.PaymentStatus;
import com.pos.exception.BadRequestException;
import com.pos.exception.NotFoundException;
import com.pos.repository.InvoiceRepository;
import com.pos.repository.PaymentTransactionRepository;
import com.pos.repository.StoreConfigRepository;
import com.pos.security.SecurityUtils;
import com.pos.util.VietQrUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/** Thanh toán QR (FR-A1): tạo/hiển thị VietQR + tra trạng thái cho FE poll + xác nhận/hết hạn. */
@Service
@Transactional(readOnly = true)
public class PaymentService {

    private static final int QR_EXPIRE_MINUTES = 15;

    private final InvoiceRepository invoiceRepository;
    private final PaymentTransactionRepository paymentRepository;
    private final StoreConfigRepository storeConfigRepository;
    private final InvoiceService invoiceService;
    private final AuditService auditService;

    public PaymentService(InvoiceRepository invoiceRepository,
                          PaymentTransactionRepository paymentRepository,
                          StoreConfigRepository storeConfigRepository,
                          InvoiceService invoiceService,
                          AuditService auditService) {
        this.invoiceRepository = invoiceRepository;
        this.paymentRepository = paymentRepository;
        this.storeConfigRepository = storeConfigRepository;
        this.invoiceService = invoiceService;
        this.auditService = auditService;
    }

    /** Trả thông tin QR cho hóa đơn; tạo giao dịch PENDING nếu chưa có. */
    @Transactional
    public PaymentInfoResponse getQr(Long invoiceId) {
        Invoice inv = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> NotFoundException.of("hóa đơn", invoiceId));
        StoreConfig cfg = storeConfigRepository.findById(StoreConfig.SINGLETON_ID).orElse(null);

        PaymentTransaction pt = paymentRepository.findFirstByInvoiceIdOrderByCreatedAtDesc(invoiceId)
                .orElseGet(() -> {
                    String prefix = (cfg != null && cfg.getTransferPrefix() != null) ? cfg.getTransferPrefix() : "POS";
                    PaymentTransaction tx = new PaymentTransaction();
                    tx.setInvoice(inv);
                    tx.setAmount(inv.getTotalAmount());
                    tx.setTransferContent(prefix + " " + inv.getCode());
                    tx.setStatus(PaymentStatus.PENDING);
                    tx.setExpiredAt(LocalDateTime.now().plusMinutes(QR_EXPIRE_MINUTES));
                    return paymentRepository.save(tx);
                });

        String qrUrl = VietQrUtil.buildQrUrl(cfg, pt.getAmount(), pt.getTransferContent());
        return new PaymentInfoResponse(inv.getId(), inv.getCode(), pt.getAmount(),
                pt.getTransferContent(), qrUrl, pt.getStatus().name());
    }

    /**
     * Thu ngân XÁC NHẬN ĐÃ NHẬN tiền chuyển khoản QR (khi chưa bật/khớp WEB2M tự động):
     * đánh dấu giao dịch PAID và chuyển hóa đơn CHỜ THANH TOÁN → HOÀN TẤT (giờ mới tính doanh thu).
     * Idempotent: gọi lại khi đã PAID/đã COMPLETED không gây lỗi.
     */
    @Transactional
    public PaymentInfoResponse confirmPaid(Long invoiceId) {
        Invoice inv = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> NotFoundException.of("hóa đơn", invoiceId));
        if (inv.getStatus() == InvoiceStatus.CANCELLED) {
            throw new BadRequestException("Hóa đơn đã hủy — không thể xác nhận thanh toán.");
        }
        PaymentTransaction pt = paymentRepository.findFirstByInvoiceIdOrderByCreatedAtDesc(invoiceId)
                .orElseThrow(() -> new BadRequestException("Hóa đơn này không có giao dịch QR để xác nhận."));
        if (pt.getStatus() != PaymentStatus.PAID) {
            pt.setStatus(PaymentStatus.PAID);
            pt.setPaidAt(LocalDateTime.now());
            pt.setBankReference("XÁC NHẬN TAY");
            paymentRepository.save(pt);
        }
        if (inv.getStatus() == InvoiceStatus.PENDING_PAYMENT) {
            inv.setStatus(InvoiceStatus.COMPLETED);
            invoiceRepository.save(inv);
            auditService.log("CONFIRM_PAYMENT", "INVOICE", inv.getId(),
                    "Xác nhận tay đã nhận thanh toán QR cho HĐ " + inv.getCode()
                            + " (" + pt.getAmount() + "đ).");
        }
        return new PaymentInfoResponse(inv.getId(), inv.getCode(), pt.getAmount(),
                pt.getTransferContent(), null, pt.getStatus().name());
    }

    /**
     * Dọn định kỳ: giao dịch QR PENDING quá hạn → EXPIRED, đồng thời TỰ HỦY hóa đơn CHỜ THANH TOÁN
     * tương ứng (hoàn điểm/KM + tự hoàn tồn qua view). Trả về số hóa đơn đã xử lý.
     */
    @Transactional
    public int expireStalePending() {
        List<PaymentTransaction> stale =
                paymentRepository.findByStatusAndExpiredAtBefore(PaymentStatus.PENDING, LocalDateTime.now());
        for (PaymentTransaction pt : stale) {
            pt.setStatus(PaymentStatus.EXPIRED);
            paymentRepository.save(pt);
            invoiceService.voidUnpaidInvoice(pt.getInvoice());
        }
        return stale.size();
    }

    /** Trạng thái thanh toán mới nhất của hóa đơn (FE poll). */
    public PaymentInfoResponse getStatus(Long invoiceId) {
        Invoice inv = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> NotFoundException.of("hóa đơn", invoiceId));
        return paymentRepository.findFirstByInvoiceIdOrderByCreatedAtDesc(invoiceId)
                .map(pt -> new PaymentInfoResponse(inv.getId(), inv.getCode(), pt.getAmount(),
                        pt.getTransferContent(), null, pt.getStatus().name()))
                .orElse(new PaymentInfoResponse(inv.getId(), inv.getCode(), inv.getTotalAmount(),
                        null, null, "NONE"));
    }
}
