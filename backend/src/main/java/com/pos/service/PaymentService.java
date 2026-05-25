package com.pos.service;

import com.pos.dto.payment.PaymentInfoResponse;
import com.pos.entity.Invoice;
import com.pos.entity.PaymentTransaction;
import com.pos.entity.StoreConfig;
import com.pos.entity.enums.PaymentStatus;
import com.pos.exception.NotFoundException;
import com.pos.repository.InvoiceRepository;
import com.pos.repository.PaymentTransactionRepository;
import com.pos.repository.StoreConfigRepository;
import com.pos.util.VietQrUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/** Thanh toán QR (FR-A1): tạo/hiển thị VietQR + tra trạng thái cho FE poll. */
@Service
@Transactional(readOnly = true)
public class PaymentService {

    private static final int QR_EXPIRE_MINUTES = 15;

    private final InvoiceRepository invoiceRepository;
    private final PaymentTransactionRepository paymentRepository;
    private final StoreConfigRepository storeConfigRepository;

    public PaymentService(InvoiceRepository invoiceRepository,
                          PaymentTransactionRepository paymentRepository,
                          StoreConfigRepository storeConfigRepository) {
        this.invoiceRepository = invoiceRepository;
        this.paymentRepository = paymentRepository;
        this.storeConfigRepository = storeConfigRepository;
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

    /** Dọn định kỳ: chuyển giao dịch QR PENDING đã quá hạn → EXPIRED. Trả số dòng cập nhật. */
    @Transactional
    public int expireStalePending() {
        return paymentRepository.expireStalePending(LocalDateTime.now());
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
