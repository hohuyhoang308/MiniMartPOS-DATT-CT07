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
import com.pos.repository.PromotionRepository;
import com.pos.repository.StoreConfigRepository;
import org.springframework.data.domain.PageRequest;
import com.pos.security.CustomUserDetails;
import com.pos.security.SecurityUtils;
import com.pos.security.StoreContext;
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
    private final AuditService auditService;
    private final LoyaltyService loyaltyService;
    private final PromotionRepository promotionRepository;

    public InvoiceService(InvoiceRepository invoiceRepository,
                          PaymentTransactionRepository paymentRepository,
                          StoreConfigRepository storeConfigRepository,
                          AuditService auditService,
                          LoyaltyService loyaltyService,
                          PromotionRepository promotionRepository) {
        this.invoiceRepository = invoiceRepository;
        this.paymentRepository = paymentRepository;
        this.storeConfigRepository = storeConfigRepository;
        this.auditService = auditService;
        this.loyaltyService = loyaltyService;
        this.promotionRepository = promotionRepository;
    }

    /** Tối đa số hóa đơn trả về 1 lần (giới hạn payload khi dữ liệu lớn). */
    private static final int MAX_INVOICES = 500;

    /** Lọc HĐ (tối đa {@value MAX_INVOICES} mới nhất). Lọc theo CHI NHÁNH; cashier chỉ thấy HĐ ca của mình. */
    public List<InvoiceResponse> search(LocalDate date, Long customerId, InvoiceStatus status) {
        LocalDateTime from = date != null ? date.atStartOfDay() : null;
        LocalDateTime to = date != null ? date.plusDays(1).atStartOfDay() : null;

        // Nhân viên (STAFF) chỉ xem HĐ thuộc ca CỦA MÌNH → đẩy điều kiện vào truy vấn (không bị cắt bởi trần 500).
        CustomUserDetails me = SecurityUtils.currentUser();
        Long onlyUserId = "STAFF".equals(me.getRole()) ? me.getId() : null;

        List<Invoice> list = invoiceRepository.search(from, to, customerId, status, null,
                StoreContext.currentStoreId(), onlyUserId, PageRequest.of(0, MAX_INVOICES));
        return list.stream().map(InvoiceResponse::from).toList();
    }

    public InvoiceResponse findById(Long id) {
        Invoice inv = getOrThrow(id);
        PaymentTransaction pt = paymentRepository.findFirstByInvoiceIdOrderByCreatedAtDesc(id).orElse(null);
        String qrUrl = null;
        if (pt != null) {
            StoreConfig cfg = storeConfigRepository.findById(inv.getStore().getId()).orElse(null);
            qrUrl = VietQrUtil.buildQrUrl(cfg, pt.getAmount(), pt.getTransferContent());
        }
        return InvoiceResponse.from(inv, pt, qrUrl);
    }

    /**
     * Hủy hóa đơn (UC14): đặt status=CANCELLED → tồn kho TỰ HOÀN (view v_batch_stock bỏ qua HĐ CANCELLED).
     * BẮT BUỘC nêu LÝ DO; ghi vết ai/khi nào/lý do (audit) — chống lạm dụng void để gian lận.
     * Hoàn tay phần KHÔNG tự suy ra: trừ lại điểm tích cho khách & giảm lượt dùng KM.
     */
    @Transactional
    public InvoiceResponse cancel(Long id, String reason) {
        if (reason == null || reason.trim().length() < 3) {
            throw new BadRequestException("Phải nhập lý do hủy hóa đơn (tối thiểu 3 ký tự)");
        }
        Invoice inv = getOrThrow(id);
        if (inv.getStatus() == InvoiceStatus.CANCELLED) {
            throw new BadRequestException("Hóa đơn này đã bị hủy trước đó");
        }
        inv.setStatus(InvoiceStatus.CANCELLED);
        inv.setCancelReason(reason.trim());
        inv.setCancelledAt(LocalDateTime.now());
        CustomUserDetails me = SecurityUtils.currentUser();
        if (me != null) inv.setCancelledBy(me.getId());

        reverseLoyaltyAndPromo(inv, "CANCEL_REVERSAL");

        Invoice saved = invoiceRepository.save(inv);
        auditService.log("CANCEL_INVOICE", "INVOICE", saved.getId(),
                "Hủy HĐ " + saved.getCode() + " (tổng " + saved.getTotalAmount() + "đ). Lý do: " + reason.trim());
        return InvoiceResponse.from(saved);
    }

    /**
     * Tự HỦY hóa đơn QR CHỜ THANH TOÁN khi quá hạn (gọi từ job dọn QR quá hạn).
     * HĐ chưa từng COMPLETED nên không thể có phiếu trả hàng → an toàn hoàn điểm/KM + tự hoàn tồn (view).
     * Chỉ tác động lên HĐ đang ở PENDING_PAYMENT (idempotent: bỏ qua nếu đã chuyển trạng thái khác).
     */
    @Transactional
    public void voidUnpaidInvoice(Invoice inv) {
        if (inv.getStatus() != InvoiceStatus.PENDING_PAYMENT) return;
        inv.setStatus(InvoiceStatus.CANCELLED);
        inv.setCancelReason("QR quá hạn — tự hủy (chưa nhận được thanh toán)");
        inv.setCancelledAt(LocalDateTime.now());
        reverseLoyaltyAndPromo(inv, "EXPIRE_REVERSAL");
        invoiceRepository.save(inv);
        auditService.log("EXPIRE_INVOICE", "INVOICE", inv.getId(),
                "Tự hủy HĐ " + inv.getCode() + " do QR quá hạn (chưa nhận tiền).");
    }

    /**
     * Đảo chiều ảnh hưởng phi-tồn-kho của một hóa đơn khi hủy: gỡ điểm đã tích + TRẢ LẠI điểm đã dùng
     * (không để âm), ghi sổ cái, và giảm lượt dùng khuyến mãi. Tồn kho tự hoàn qua view (HĐ CANCELLED).
     */
    private void reverseLoyaltyAndPromo(Invoice inv, String ledgerReason) {
        Customer customer = inv.getCustomer();
        if (customer != null) {
            int earned = inv.getPointsEarned() != null ? inv.getPointsEarned() : 0;
            int used = inv.getPointsUsed() != null ? inv.getPointsUsed() : 0;
            int restored = Math.max(0, customer.getLoyaltyPoints() - earned + used);
            customer.setLoyaltyPoints(restored);
            int netDelta = used - earned; // đảo chiều: trả lại điểm đã dùng, gỡ điểm đã tích
            if (netDelta != 0) {
                loyaltyService.record(customer.getId(), inv.getId(), netDelta, ledgerReason, restored);
            }
        }
        Promotion promo = inv.getPromotion();
        if (promo != null) {
            promotionRepository.releaseOne(promo.getId()); // giảm lượt dùng KM atomic (không để âm)
        }
    }

    private Invoice getOrThrow(Long id) {
        Invoice inv = invoiceRepository.findById(id).orElseThrow(() -> NotFoundException.of("hóa đơn", id));
        StoreContext.assertSameStore(inv.getStore().getId());   // chặn xem/hủy hóa đơn chéo cửa hàng
        return inv;
    }
}
