package com.pos.service;

import com.pos.dto.payment.PaymentInfoResponse;
import com.pos.entity.Invoice;
import com.pos.entity.PaymentTransaction;
import com.pos.entity.enums.InvoiceStatus;
import com.pos.entity.enums.PaymentStatus;
import com.pos.exception.BadRequestException;
import com.pos.exception.NotFoundException;
import com.pos.repository.InvoiceRepository;
import com.pos.repository.PaymentTransactionRepository;
import com.pos.security.StoreContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/** Thanh toán QR (FR-A1): tra trạng thái cho FE poll + xác nhận tay + dọn/hết hạn.
 *  (QR + nội dung CK được trả ngay khi tạo hóa đơn trong SaleService.) */
@Service
@Transactional(readOnly = true)
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final InvoiceRepository invoiceRepository;
    private final PaymentTransactionRepository paymentRepository;
    private final InvoiceService invoiceService;
    private final AuditService auditService;

    public PaymentService(InvoiceRepository invoiceRepository,
                          PaymentTransactionRepository paymentRepository,
                          InvoiceService invoiceService,
                          AuditService auditService) {
        this.invoiceRepository = invoiceRepository;
        this.paymentRepository = paymentRepository;
        this.invoiceService = invoiceService;
        this.auditService = auditService;
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
        StoreContext.assertSameStore(inv.getStore().getId());   // chặn xác nhận thanh toán chéo cửa hàng
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
            // Cô lập từng giao dịch: một hóa đơn lỗi không được làm hỏng cả đợt dọn (poison pill).
            try {
                pt.setStatus(PaymentStatus.EXPIRED);
                paymentRepository.save(pt);
                invoiceService.voidUnpaidInvoice(pt.getInvoice());
            } catch (Exception e) {
                log.warn("Bỏ qua giao dịch #{}: {}", pt.getId(), e.toString());
            }
        }
        return stale.size();
    }

    /** Trạng thái thanh toán mới nhất của hóa đơn (FE poll). */
    public PaymentInfoResponse getStatus(Long invoiceId) {
        Invoice inv = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> NotFoundException.of("hóa đơn", invoiceId));
        StoreContext.assertSameStore(inv.getStore().getId());   // chặn xem trạng thái thanh toán chéo cửa hàng
        return paymentRepository.findFirstByInvoiceIdOrderByCreatedAtDesc(invoiceId)
                .map(pt -> new PaymentInfoResponse(inv.getId(), inv.getCode(), pt.getAmount(),
                        pt.getTransferContent(), null, pt.getStatus().name()))
                .orElse(new PaymentInfoResponse(inv.getId(), inv.getCode(), inv.getTotalAmount(),
                        null, null, "NONE"));
    }
}
