package com.pos.service;

import com.pos.dto.returns.CreateReturnRequest;
import com.pos.dto.returns.ReturnResponse;
import com.pos.dto.returns.ReturnableLineResponse;
import com.pos.entity.*;
import com.pos.entity.enums.InvoiceStatus;
import com.pos.exception.BadRequestException;
import com.pos.exception.NotFoundException;
import com.pos.repository.InvoiceRepository;
import com.pos.repository.ProductRepository;
import com.pos.repository.SalesReturnItemRepository;
import com.pos.repository.SalesReturnRepository;
import com.pos.repository.UserRepository;
import com.pos.security.SecurityUtils;
import com.pos.util.Money;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Trả hàng / hoàn tiền (sau bán). Trả từng phần theo dòng; hàng trả về KHO (view tự cộng lại tồn);
 * hoàn tiền = Σ số lượng × đơn giá bán. Hóa đơn gốc giữ nguyên COMPLETED, chứng từ trả là tài liệu riêng.
 */
@Service
@Transactional(readOnly = true)
public class ReturnService {

    private final InvoiceRepository invoiceRepository;
    private final SalesReturnRepository returnRepository;
    private final SalesReturnItemRepository returnItemRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final AuditService auditService;
    private final LoyaltyService loyaltyService;

    public ReturnService(InvoiceRepository invoiceRepository,
                         SalesReturnRepository returnRepository,
                         SalesReturnItemRepository returnItemRepository,
                         UserRepository userRepository,
                         ProductRepository productRepository,
                         AuditService auditService,
                         LoyaltyService loyaltyService) {
        this.invoiceRepository = invoiceRepository;
        this.returnRepository = returnRepository;
        this.returnItemRepository = returnItemRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.auditService = auditService;
        this.loyaltyService = loyaltyService;
    }

    /** Các dòng của hóa đơn kèm số CÒN ĐƯỢC TRẢ (đã trừ phần đã trả trước đó). */
    public List<ReturnableLineResponse> returnableLines(Long invoiceId) {
        Invoice inv = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> NotFoundException.of("hóa đơn", invoiceId));
        Map<Long, Long> returned = returnedPerItem(invoiceId);
        return inv.getItems().stream().map(it -> {
            long already = returned.getOrDefault(it.getId(), 0L);
            return new ReturnableLineResponse(it.getId(), it.getProduct().getId(), it.getProduct().getName(),
                    it.getQuantity(), (int) already, (int) (it.getQuantity() - already), it.getUnitPrice());
        }).toList();
    }

    public List<ReturnResponse> byInvoice(Long invoiceId) {
        return returnRepository.findByInvoiceIdOrderByCreatedAtDesc(invoiceId).stream()
                .map(ReturnResponse::from).toList();
    }

    public List<ReturnResponse> recent() {
        return returnRepository.findTop200ByOrderByCreatedAtDesc().stream().map(ReturnResponse::from).toList();
    }

    @Transactional
    public ReturnResponse createReturn(CreateReturnRequest req) {
        Invoice inv = invoiceRepository.findById(req.invoiceId())
                .orElseThrow(() -> NotFoundException.of("hóa đơn", req.invoiceId()));
        if (inv.getStatus() != InvoiceStatus.COMPLETED) {
            throw new BadRequestException("Chỉ trả hàng cho hóa đơn đã hoàn tất (COMPLETED).");
        }

        Map<Long, InvoiceItem> itemMap = inv.getItems().stream()
                .collect(Collectors.toMap(InvoiceItem::getId, it -> it));

        // Khóa tồn các sản phẩm bị ảnh hưởng (id TĂNG DẦN để tránh deadlock) TRƯỚC khi ghi phiếu trả:
        // trả hàng làm THAY ĐỔI tồn (view cộng hàng trả về kho) nên phải tuần tự hóa như bán/lên kệ.
        req.items().stream()
                .map(l -> itemMap.get(l.invoiceItemId()))
                .filter(Objects::nonNull)
                .map(it -> it.getProduct().getId())
                .distinct().sorted()
                .forEach(productRepository::findByIdForUpdate);

        Map<Long, Long> returnedPerItem = returnedPerItem(inv.getId());

        User user = userRepository.findById(SecurityUtils.currentUserId()).orElse(null);
        SalesReturn ret = new SalesReturn();
        ret.setInvoice(inv);
        ret.setReason(req.reason().trim());
        ret.setCreatedBy(user);

        BigDecimal refund = BigDecimal.ZERO;
        for (CreateReturnRequest.Line line : req.items()) {
            InvoiceItem item = itemMap.get(line.invoiceItemId());
            if (item == null) throw new BadRequestException("Dòng bán không thuộc hóa đơn này");
            long already = returnedPerItem.getOrDefault(item.getId(), 0L);
            long returnable = item.getQuantity() - already;
            int qty = line.quantity();
            if (qty > returnable) {
                throw new BadRequestException("Sản phẩm \"" + item.getProduct().getName()
                        + "\" chỉ còn được trả " + returnable + " (đã bán " + item.getQuantity()
                        + ", đã trả " + already + ").");
            }

            // Phân bổ số trả vào các LÔ đã bán của dòng (không vượt phần còn lại của từng lô)
            Map<Long, Long> retByBatch = new HashMap<>();
            for (var rb : returnItemRepository.returnedByItemAndBatch(item.getId())) {
                retByBatch.put(rb.getBatchId(), rb.getQty());
            }
            int need = qty;
            for (InvoiceItemBatch alloc : item.getBatches()) {
                if (need <= 0) break;
                long batchId = alloc.getBatch().getId();
                long avail = alloc.getQuantity() - retByBatch.getOrDefault(batchId, 0L);
                if (avail <= 0) continue;
                int take = (int) Math.min(avail, need);
                SalesReturnItem ri = new SalesReturnItem();
                ri.setInvoiceItem(item);
                ri.setBatch(alloc.getBatch());
                ri.setQuantity(take);
                ri.setUnitPrice(item.getUnitPrice());
                ret.addItem(ri);
                need -= take;
            }
            if (need > 0) throw new BadRequestException("Không phân bổ được số trả cho các lô (dữ liệu lệch).");
            refund = refund.add(item.getUnitPrice().multiply(BigDecimal.valueOf(qty)));
        }

        // Hoàn theo số tiền THỰC TRẢ: nếu HĐ gốc có giảm giá (KM/đổi điểm) thì hoàn theo tỉ lệ total/subtotal,
        // tránh hoàn dư so với số khách đã trả.
        BigDecimal sub = inv.getSubtotal();
        if (sub != null && sub.signum() > 0 && inv.getTotalAmount() != null) {
            refund = Money.prorate(refund, inv.getTotalAmount(), sub);
        }
        ret.setRefundAmount(refund);
        SalesReturn saved = returnRepository.save(ret);

        // Thu hồi ĐIỂM TÍCH đã thưởng cho phần hàng trả (theo tỉ lệ tiền hoàn / tổng HĐ).
        Customer customer = inv.getCustomer();
        int earned = inv.getPointsEarned() != null ? inv.getPointsEarned() : 0;
        if (customer != null && earned > 0 && inv.getTotalAmount() != null && inv.getTotalAmount().signum() > 0) {
            int clawback = refund.multiply(BigDecimal.valueOf(earned))
                    .divide(inv.getTotalAmount(), 0, java.math.RoundingMode.FLOOR).intValue();
            if (clawback > 0) {
                int newBalance = Math.max(0, customer.getLoyaltyPoints() - clawback);
                customer.setLoyaltyPoints(newBalance);
                loyaltyService.record(customer.getId(), inv.getId(), -clawback, "RETURN", newBalance);
            }
        }

        auditService.log("RETURN", "INVOICE", inv.getId(),
                "Trả hàng HĐ " + inv.getCode() + ", hoàn " + refund + "đ. Lý do: " + req.reason().trim());
        return ReturnResponse.from(saved);
    }

    private Map<Long, Long> returnedPerItem(Long invoiceId) {
        Map<Long, Long> m = new HashMap<>();
        for (var r : returnItemRepository.returnedByInvoiceItem(invoiceId)) {
            m.put(r.getInvoiceItemId(), r.getQty());
        }
        return m;
    }
}
