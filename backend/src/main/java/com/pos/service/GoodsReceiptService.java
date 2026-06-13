package com.pos.service;

import com.pos.dto.receipt.GoodsReceiptItemRequest;
import com.pos.dto.receipt.GoodsReceiptRequest;
import com.pos.dto.receipt.GoodsReceiptResponse;
import com.pos.entity.*;
import com.pos.exception.NotFoundException;
import com.pos.repository.GoodsReceiptRepository;
import com.pos.repository.ProductRepository;
import com.pos.repository.StoreRepository;
import com.pos.repository.SupplierRepository;
import com.pos.repository.UserRepository;
import com.pos.security.SecurityUtils;
import com.pos.security.StoreContext;
import com.pos.util.CodeGenerator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/** Lập phiếu nhập kho (FR3.2, FR3.3 - UC07). Lưu phiếu + lô trong một transaction. */
@Service
@Transactional(readOnly = true)
public class GoodsReceiptService {

    private static final String CODE_PREFIX = "PN";

    private final GoodsReceiptRepository receiptRepository;
    private final SupplierRepository supplierRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final StoreRepository storeRepository;
    private final AuditService auditService;

    public GoodsReceiptService(GoodsReceiptRepository receiptRepository,
                               SupplierRepository supplierRepository,
                               ProductRepository productRepository,
                               UserRepository userRepository,
                               StoreRepository storeRepository,
                               AuditService auditService) {
        this.receiptRepository = receiptRepository;
        this.supplierRepository = supplierRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.storeRepository = storeRepository;
        this.auditService = auditService;
    }

    public List<GoodsReceiptResponse> findAll() {
        // Lọc TẠI CSDL theo cửa hàng đang làm việc (đa cửa hàng); ADMIN chưa chọn cửa hàng (null) → toàn chuỗi.
        Long storeId = StoreContext.currentStoreId();
        var receipts = storeId == null
                ? receiptRepository.findAll().stream().sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt())).toList()
                : receiptRepository.findByStoreIdOrderByCreatedAtDesc(storeId);
        return receipts.stream().map(GoodsReceiptResponse::from).toList();
    }

    public GoodsReceiptResponse findById(Long id) {
        GoodsReceipt r = receiptRepository.findById(id)
                .orElseThrow(() -> NotFoundException.of("phiếu nhập", id));
        StoreContext.assertSameStore(r.getStore().getId());   // chặn xem phiếu nhập chéo cửa hàng
        return GoodsReceiptResponse.from(r);
    }

    @Transactional
    public GoodsReceiptResponse create(GoodsReceiptRequest req) {
        Supplier supplier = supplierRepository.findById(req.supplierId())
                .orElseThrow(() -> NotFoundException.of("nhà cung cấp", req.supplierId()));
        User createdBy = userRepository.findById(SecurityUtils.currentUserId())
                .orElseThrow(() -> NotFoundException.of("tài khoản", SecurityUtils.currentUserId()));

        // Chi nhánh nhập = chi nhánh đang làm việc (đa chuỗi). LÔ hàng sẽ thừa hưởng chi nhánh này.
        Store store = storeRepository.getReferenceById(StoreContext.requireStoreId());

        GoodsReceipt receipt = new GoodsReceipt();
        receipt.setStore(store);
        receipt.setSupplier(supplier);
        receipt.setCreatedBy(createdBy);
        receipt.setNote(req.note());
        receipt.setCode(generateCode());

        BigDecimal total = BigDecimal.ZERO;
        boolean updateCost = Boolean.TRUE.equals(req.updateCostPrice());

        for (GoodsReceiptItemRequest itemReq : req.items()) {
            Product product = productRepository.findById(itemReq.productId())
                    .orElseThrow(() -> NotFoundException.of("sản phẩm", itemReq.productId()));

            GoodsReceiptItem item = new GoodsReceiptItem();
            item.setProduct(product);
            item.setQuantity(itemReq.quantity());
            item.setImportPrice(itemReq.importPrice());
            item.setExpiryDate(itemReq.expiryDate());
            receipt.addItem(item);

            total = total.add(itemReq.importPrice().multiply(BigDecimal.valueOf(itemReq.quantity())));

            if (updateCost) {
                product.setCostPrice(itemReq.importPrice());
                productRepository.save(product);
            }
        }
        receipt.setTotalAmount(total);

        // Lưu phiếu + các lô (cascade). Tồn kho tự tăng vì view tính từ lô mới.
        GoodsReceipt saved = receiptRepository.save(receipt);
        auditService.log("CREATE_RECEIPT", "GOODS_RECEIPT", saved.getId(),
                "Lập phiếu nhập kho " + saved.getCode() + " từ nhà cung cấp \"" + supplier.getName()
                        + "\" — " + req.items().size() + " mặt hàng, tổng tiền " + total + "đ"
                        + (updateCost ? " (có cập nhật giá vốn sản phẩm)" : " (không cập nhật giá vốn)"));
        return GoodsReceiptResponse.from(saved);
    }

    /** Sinh mã PN<yyyyMMdd>-<seq 3 chữ số>. */
    private String generateCode() {
        long seq = receiptRepository.countByCodeStartingWith(CodeGenerator.datePrefix(CODE_PREFIX)) + 1;
        return CodeGenerator.build(CODE_PREFIX, seq, 3);
    }
}
