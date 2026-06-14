package com.pos.service;

import com.pos.dto.inventory.StockAdjustmentRequest;
import com.pos.dto.inventory.StockAdjustmentResponse;
import com.pos.entity.GoodsReceiptItem;
import com.pos.entity.StockAdjustment;
import com.pos.entity.view.BatchStockView;
import com.pos.exception.BadRequestException;
import com.pos.exception.NotFoundException;
import com.pos.repository.GoodsReceiptItemRepository;
import com.pos.repository.ProductRepository;
import com.pos.repository.StockAdjustmentRepository;
import com.pos.repository.StoreRepository;
import com.pos.repository.UserRepository;
import com.pos.repository.view.BatchStockViewRepository;
import com.pos.security.SecurityUtils;
import com.pos.security.StoreContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * XUẤT HỦY / ĐIỀU CHỈNH GIẢM TỒN (FR8 — kiểm kê & hao hụt). Cho phép MANAGER rút hàng hết hạn / hư hỏng /
 * thất thoát ra khỏi tồn kho — việc mà trước đây không làm được (hàng hết hạn bị chặn bán nhưng vẫn
 * "kẹt" trong tồn mãi mãi, làm sai cảnh báo tồn/gợi ý nhập).
 *
 * <p>Quy ước: chỉ xuất hủy hàng đang TRONG KHO của lô (hàng trên kệ phải "lấy về kho" trước) để công thức
 * tồn kệ trong {@code v_batch_stock} luôn đúng. Thao tác giảm tồn nên chỉ ADMIN/MANAGER (xem controller).</p>
 */
@Service
@Transactional(readOnly = true)
public class StockAdjustmentService {

    private final StockAdjustmentRepository repository;
    private final GoodsReceiptItemRepository batchRepository;
    private final BatchStockViewRepository batchStockRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final StoreRepository storeRepository;
    private final AuditService auditService;

    public StockAdjustmentService(StockAdjustmentRepository repository,
                                  GoodsReceiptItemRepository batchRepository,
                                  BatchStockViewRepository batchStockRepository,
                                  ProductRepository productRepository,
                                  UserRepository userRepository,
                                  StoreRepository storeRepository,
                                  AuditService auditService) {
        this.repository = repository;
        this.batchRepository = batchRepository;
        this.batchStockRepository = batchStockRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.storeRepository = storeRepository;
        this.auditService = auditService;
    }

    /** Lịch sử xuất hủy của chi nhánh đang làm việc. */
    public List<StockAdjustmentResponse> recent() {
        Long storeId = StoreContext.requireStoreId();
        return repository.findTop100ByStore_IdOrderByIdDesc(storeId).stream()
                .map(StockAdjustmentResponse::from).toList();
    }

    @Transactional
    public StockAdjustmentResponse create(StockAdjustmentRequest req) {
        Long storeId = StoreContext.requireStoreId();
        GoodsReceiptItem batch = batchRepository.findById(req.batchId())
                .orElseThrow(() -> NotFoundException.of("lô", req.batchId()));
        // Chặn xuất hủy chéo chi nhánh: lô phải thuộc chi nhánh đang làm việc.
        StoreContext.assertSameStore(batch.getReceipt().getStore().getId());

        // Khoá tồn sản phẩm TRƯỚC khi đọc tồn từ view → tránh đua với bán/lên kệ (không cho tồn âm).
        productRepository.findByIdForUpdate(batch.getProduct().getId());
        BatchStockView v = batchStockRepository.findById(req.batchId())
                .orElseThrow(() -> NotFoundException.of("lô", req.batchId()));
        long inWarehouse = v.getInWarehouse() != null ? v.getInWarehouse() : 0L;
        if (inWarehouse <= 0) {
            throw new BadRequestException("Lô này không còn hàng trong kho để xuất hủy. "
                    + "Nếu hàng đang trên kệ, hãy lấy về kho trước.");
        }
        if (req.quantity() > inWarehouse) {
            throw new BadRequestException("Số lượng xuất hủy (" + req.quantity()
                    + ") vượt tồn trong kho của lô (chỉ còn " + inWarehouse + ").");
        }

        StockAdjustment adj = new StockAdjustment();
        adj.setStore(storeRepository.getReferenceById(storeId));
        adj.setBatch(batch);
        adj.setQuantity(req.quantity());
        adj.setReason(req.reason());
        adj.setNote(req.note());
        adj.setCreatedBy(userRepository.findById(SecurityUtils.currentUserId()).orElse(null));
        StockAdjustment saved = repository.save(adj);

        String productName = batch.getProduct() != null ? batch.getProduct().getName() : "#" + req.batchId();
        auditService.log("WRITEOFF_STOCK", "BATCH", batch.getId(),
                "Xuất hủy " + req.quantity() + " \"" + productName + "\" (lô #" + batch.getId()
                        + ", lý do " + req.reason() + ")"
                        + (req.note() != null && !req.note().isBlank() ? " — " + req.note() : ""));
        return StockAdjustmentResponse.from(saved);
    }
}
