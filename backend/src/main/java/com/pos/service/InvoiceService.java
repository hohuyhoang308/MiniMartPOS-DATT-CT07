package com.pos.service;

import com.pos.dto.invoice.InvoiceResponse;
import com.pos.entity.Customer;
import com.pos.entity.Invoice;
import com.pos.entity.PaymentTransaction;
import com.pos.entity.Promotion;
import com.pos.entity.StoreConfig;
import com.pos.entity.enums.InvoiceStatus;
import com.pos.exception.BadRequestException;
import com.pos.exception.NotFoundException;
import com.pos.repository.InvoiceRepository;
import com.pos.repository.PaymentTransactionRepository;
import com.pos.repository.StoreConfigRepository;
import com.pos.security.CustomUserDetails;
import com.pos.security.SecurityUtils;
import com.pos.util.VietQrUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Quản lý hóa đơn & lịch sử (FR5 - UC13/UC14). */
@Service
@Transactional(readOnly = true)
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final PaymentTransactionRepository paymentRepository;
    private final StoreConfigRepository storeConfigRepository;

    public InvoiceService(InvoiceRepository invoiceRepository,
                          PaymentTransactionRepository paymentRepository,
                          StoreConfigRepository storeConfigRepository) {
        this.invoiceRepository = invoiceRepository;
        this.paymentRepository = paymentRepository;
        this.storeConfigRepository = storeConfigRepository;
    }

    /** Lọc HĐ. Cashier chỉ thấy hóa đơn thuộc ca của chính mình. */
    public List<InvoiceResponse> search(LocalDate date, Long customerId, InvoiceStatus status) {
        LocalDateTime from = date != null ? date.atStartOfDay() : null;
        LocalDateTime to = date != null ? date.plusDays(1).atStartOfDay() : null;

        List<Invoice> list = invoiceRepository.search(from, to, customerId, status, null);

        CustomUserDetails me = SecurityUtils.currentUser();
        if ("CASHIER".equals(me.getRole())) {
            list = list.stream()
                    .filter(i -> i.getShift().getUser().getId().equals(me.getId()))
                    .toList();
        }
        return list.stream().map(InvoiceResponse::from).toList();
    }

    public InvoiceResponse findById(Long id) {
        Invoice inv = getOrThrow(id);
        PaymentTransaction pt = paymentRepository.findFirstByInvoiceIdOrderByCreatedAtDesc(id).orElse(null);
        String qrUrl = null;
        if (pt != null) {
            StoreConfig cfg = storeConfigRepository.findById(StoreConfig.SINGLETON_ID).orElse(null);
            qrUrl = VietQrUtil.buildQrUrl(cfg, pt.getAmount(), pt.getTransferContent());
        }
        return InvoiceResponse.from(inv, pt, qrUrl);
    }

    /**
     * Hủy hóa đơn (UC14): đặt status=CANCELLED → tồn kho TỰ HOÀN (view v_batch_stock bỏ qua HĐ CANCELLED).
     * Hoàn tay phần KHÔNG tự suy ra: trừ lại điểm tích cho khách & giảm lượt dùng KM.
     */
    @Transactional
    public InvoiceResponse cancel(Long id) {
        Invoice inv = getOrThrow(id);
        if (inv.getStatus() == InvoiceStatus.CANCELLED) {
            throw new BadRequestException("Hóa đơn này đã bị hủy trước đó");
        }
        inv.setStatus(InvoiceStatus.CANCELLED);

        // Hoàn điểm tích lũy cho khách (không để âm)
        Customer customer = inv.getCustomer();
        if (customer != null && inv.getPointsEarned() != null && inv.getPointsEarned() > 0) {
            int restored = Math.max(0, customer.getLoyaltyPoints() - inv.getPointsEarned());
            customer.setLoyaltyPoints(restored);
        }

        // Giảm lượt dùng khuyến mãi (không để âm)
        Promotion promo = inv.getPromotion();
        if (promo != null && promo.getUsedCount() != null && promo.getUsedCount() > 0) {
            promo.setUsedCount(promo.getUsedCount() - 1);
        }

        return InvoiceResponse.from(invoiceRepository.save(inv));
    }

    private Invoice getOrThrow(Long id) {
        return invoiceRepository.findById(id).orElseThrow(() -> NotFoundException.of("hóa đơn", id));
    }
}
