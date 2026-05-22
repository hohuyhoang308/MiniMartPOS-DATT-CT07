package com.pos.service;

import com.pos.dto.invoice.InvoiceResponse;
import com.pos.dto.sale.CreateInvoiceRequest;
import com.pos.dto.sale.SaleItemRequest;
import com.pos.entity.*;
import com.pos.entity.enums.PaymentMethod;
import com.pos.entity.enums.PaymentStatus;
import com.pos.entity.enums.ShiftStatus;
import com.pos.entity.view.BatchStockView;
import com.pos.exception.BadRequestException;
import com.pos.exception.ConflictException;
import com.pos.exception.NotFoundException;
import com.pos.repository.*;
import com.pos.repository.view.BatchStockViewRepository;
import com.pos.security.SecurityUtils;
import com.pos.util.CodeGenerator;
import com.pos.util.VietQrUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Nghiệp vụ bán hàng tại quầy (UC09/UC10) — toàn bộ trong MỘT transaction:
 * lưu hóa đơn + chi tiết, chọn lô FIFO theo HSD ghi invoice_item_batches,
 * tích điểm khách, tăng lượt dùng KM, tạo giao dịch QR (nếu thanh toán QR).
 * Nếu một sản phẩm không đủ tồn → rollback toàn bộ.
 */
@Service
@Transactional(readOnly = true)
public class SaleService {

    private static final String CODE_PREFIX = "HD";
    private static final BigDecimal POINT_RATE = BigDecimal.valueOf(10000); // 1 điểm / 10.000đ
    private static final int QR_EXPIRE_MINUTES = 15;

    private final InvoiceRepository invoiceRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final WorkShiftRepository shiftRepository;
    private final BatchStockViewRepository batchStockRepository;
    private final GoodsReceiptItemRepository batchRepository;
    private final PaymentTransactionRepository paymentRepository;
    private final StoreConfigRepository storeConfigRepository;
    private final PromotionService promotionService;
    private final PromotionRepository promotionRepository;

    public SaleService(InvoiceRepository invoiceRepository,
                       ProductRepository productRepository,
                       CustomerRepository customerRepository,
                       WorkShiftRepository shiftRepository,
                       BatchStockViewRepository batchStockRepository,
                       GoodsReceiptItemRepository batchRepository,
                       PaymentTransactionRepository paymentRepository,
                       StoreConfigRepository storeConfigRepository,
                       PromotionService promotionService,
                       PromotionRepository promotionRepository) {
        this.invoiceRepository = invoiceRepository;
        this.productRepository = productRepository;
        this.customerRepository = customerRepository;
        this.shiftRepository = shiftRepository;
        this.batchStockRepository = batchStockRepository;
        this.batchRepository = batchRepository;
        this.paymentRepository = paymentRepository;
        this.storeConfigRepository = storeConfigRepository;
        this.promotionService = promotionService;
        this.promotionRepository = promotionRepository;
    }

    @Transactional
    public InvoiceResponse createInvoice(CreateInvoiceRequest req) {
        // 1) Ca làm việc đang mở của thu ngân hiện tại (tiền điều kiện UC09)
        WorkShift shift = shiftRepository
                .findFirstByUserIdAndStatusOrderByOpenedAtDesc(SecurityUtils.currentUserId(), ShiftStatus.OPEN)
                .orElseThrow(() -> new BadRequestException("Bạn chưa mở ca làm việc — hãy mở ca trước khi bán hàng"));

        // 2) Tạo các dòng hóa đơn (giá snapshot) + tính tổng tạm tính
        Invoice invoice = new Invoice();
        invoice.setShift(shift);
        invoice.setPaymentMethod(req.paymentMethod());

        BigDecimal subtotal = BigDecimal.ZERO;
        Map<Long, Integer> neededByProduct = new HashMap<>();
        for (SaleItemRequest line : req.items()) {
            Product product = productRepository.findById(line.productId())
                    .orElseThrow(() -> NotFoundException.of("sản phẩm", line.productId()));
            InvoiceItem item = new InvoiceItem();
            item.setProduct(product);
            item.setQuantity(line.quantity());
            item.setUnitPrice(product.getSalePrice());
            invoice.addItem(item);

            subtotal = subtotal.add(product.getSalePrice().multiply(BigDecimal.valueOf(line.quantity())));
            neededByProduct.merge(product.getId(), line.quantity(), Integer::sum);
        }
        invoice.setSubtotal(subtotal);

        // 3) Khuyến mãi (tùy chọn)
        BigDecimal discount = BigDecimal.ZERO;
        Promotion promotion = null;
        if (req.promotionCode() != null && !req.promotionCode().isBlank()) {
            PromotionService.ApplyResult applied = promotionService.validateForSale(req.promotionCode().trim(), subtotal);
            promotion = applied.promotion();
            discount = applied.discountAmount();
            invoice.setPromotion(promotion);
        }
        invoice.setDiscountAmount(discount);
        BigDecimal total = subtotal.subtract(discount);

        // 4) Thanh toán
        if (req.paymentMethod() == PaymentMethod.CASH) {
            if (req.customerPaid() == null) {
                throw new BadRequestException("Phải nhập số tiền khách đưa khi thanh toán tiền mặt");
            }
            if (req.customerPaid().compareTo(total) < 0) {
                throw new BadRequestException("Tiền khách đưa nhỏ hơn tổng tiền phải trả");
            }
            invoice.setCustomerPaid(req.customerPaid());
            invoice.setChangeAmount(req.customerPaid().subtract(total));
        }

        // 5) Khách thân thiết + tích điểm (tùy chọn)
        Customer customer = null;
        int pointsEarned = total.divide(POINT_RATE, 0, RoundingMode.FLOOR).intValue();
        if (req.customerId() != null) {
            customer = customerRepository.findById(req.customerId())
                    .orElseThrow(() -> NotFoundException.of("khách hàng", req.customerId()));
            invoice.setCustomer(customer);
            customer.setLoyaltyPoints(customer.getLoyaltyPoints() + pointsEarned);
        } else {
            pointsEarned = 0; // không có khách → không tích điểm
        }
        invoice.setPointsEarned(pointsEarned);
        invoice.setCode(generateCode());

        // 6) Trừ tồn FIFO theo HSD: phân bổ từng dòng bán vào các lô
        allocateStockFifo(invoice, neededByProduct);

        // 7) Tăng lượt dùng KM
        if (promotion != null) {
            promotion.setUsedCount(promotion.getUsedCount() + 1);
            promotionRepository.save(promotion);
        }

        // 8) Lưu hóa đơn (cascade items + batches). flush để CSDL tính total_amount/subtotal (GENERATED).
        Invoice saved = invoiceRepository.saveAndFlush(invoice);

        // 9) Thanh toán QR → tạo giao dịch chờ đối soát WEB2M
        PaymentTransaction pt = null;
        String qrUrl = null;
        if (req.paymentMethod() == PaymentMethod.QR) {
            StoreConfig cfg = storeConfigRepository.findById(StoreConfig.SINGLETON_ID).orElse(null);
            String prefix = (cfg != null && cfg.getTransferPrefix() != null) ? cfg.getTransferPrefix() : "POS";
            pt = new PaymentTransaction();
            pt.setInvoice(saved);
            pt.setAmount(total);
            pt.setTransferContent(prefix + " " + saved.getCode());
            pt.setStatus(PaymentStatus.PENDING);
            pt.setExpiredAt(LocalDateTime.now().plusMinutes(QR_EXPIRE_MINUTES));
            pt = paymentRepository.save(pt);
            qrUrl = VietQrUtil.buildQrUrl(cfg, total, pt.getTransferContent());
        }

        return InvoiceResponse.from(saved, pt, qrUrl);
    }

    /** Phân bổ FIFO: với mỗi sản phẩm, rút tồn theo thứ tự HSD gần nhất. Thiếu tồn → Conflict (rollback). */
    private void allocateStockFifo(Invoice invoice, Map<Long, Integer> neededByProduct) {
        // Nạp danh sách lô khả dụng (FIFO) cho từng sản phẩm + bộ đếm tồn còn lại trong bộ nhớ
        Map<Long, Deque<long[]>> batchesByProduct = new HashMap<>(); // productId -> deque[ batchId, remaining ]
        for (Long productId : neededByProduct.keySet()) {
            Deque<long[]> deque = new ArrayDeque<>();
            for (BatchStockView b : batchStockRepository.findAvailableBatchesFifo(productId)) {
                deque.addLast(new long[]{b.getBatchId(), b.getQuantityRemaining()});
            }
            batchesByProduct.put(productId, deque);
        }

        for (InvoiceItem item : invoice.getItems()) {
            Long productId = item.getProduct().getId();
            int need = item.getQuantity();
            Deque<long[]> deque = batchesByProduct.get(productId);

            while (need > 0 && deque != null && !deque.isEmpty()) {
                long[] head = deque.peekFirst();
                long batchId = head[0];
                long remaining = head[1];
                if (remaining <= 0) { deque.pollFirst(); continue; }

                int take = (int) Math.min(remaining, need);
                InvoiceItemBatch alloc = new InvoiceItemBatch();
                alloc.setBatch(batchRepository.getReferenceById(batchId));
                alloc.setQuantity(take);
                item.addBatch(alloc);

                head[1] = remaining - take;
                need -= take;
                if (head[1] <= 0) deque.pollFirst();
            }

            if (need > 0) {
                throw new ConflictException("Sản phẩm \"" + item.getProduct().getName()
                        + "\" không đủ tồn kho để bán (thiếu " + need + ")");
            }
        }
    }

    private String generateCode() {
        long seq = invoiceRepository.countByCodeStartingWith(CodeGenerator.datePrefix(CODE_PREFIX)) + 1;
        return CodeGenerator.build(CODE_PREFIX, seq, 4);
    }
}
