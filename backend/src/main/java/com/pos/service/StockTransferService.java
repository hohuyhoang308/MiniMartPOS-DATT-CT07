package com.pos.service;

import com.pos.dto.transfer.CreateTransferRequest;
import com.pos.dto.transfer.StockTransferResponse;
import com.pos.entity.*;
import com.pos.entity.enums.AdjustmentReason;
import com.pos.entity.enums.ReceiptSource;
import com.pos.entity.enums.TransferStatus;
import com.pos.entity.view.BatchStockView;
import com.pos.exception.BadRequestException;
import com.pos.exception.ConflictException;
import com.pos.exception.NotFoundException;
import com.pos.repository.*;
import com.pos.repository.view.BatchStockViewRepository;
import com.pos.security.SecurityUtils;
import com.pos.security.StoreContext;
import com.pos.util.CodeGenerator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * ĐIỀU CHUYỂN HÀNG NỘI BỘ giữa chi nhánh (Obj 1.1) — state machine PENDING→SHIPPING→RECEIVED.
 *
 * <p>Nguyên tắc: KHÔNG mutate lô (giữ tính bất biến + view tồn kho). Thay vào đó:
 * <ul>
 *   <li><b>SHIP</b> (chi nhánh NGUỒN): ghi {@link StockAdjustment} reason=TRANSFER_OUT → view trừ tồn nguồn.</li>
 *   <li><b>RECEIVE</b> (chi nhánh ĐÍCH): tạo {@link GoodsReceipt} source=TRANSFER mang HSD/giá vốn gốc →
 *       tồn xuất hiện ở đích như lô mới, FEFO theo HSD vẫn đúng.</li>
 * </ul>
 * Mọi bước đều khóa phiếu (PESSIMISTIC_WRITE) + chốt phạm vi chi nhánh + kiểm bảng chuyển hợp lệ.</p>
 */
@Service
@Transactional(readOnly = true)
public class StockTransferService {

    private static final String CODE_PREFIX = "DC";

    /** Bảng chuyển trạng thái hợp lệ — chặn mọi nhảy trạng thái tùy tiện (defensive programming). */
    private static final Map<TransferStatus, Set<TransferStatus>> ALLOWED = new EnumMap<>(TransferStatus.class);
    static {
        ALLOWED.put(TransferStatus.PENDING, EnumSet.of(TransferStatus.SHIPPING, TransferStatus.CANCELLED));
        // SHIPPING có thể HỦY (thu hồi): hàng đã rời kho nguồn sẽ được hoàn lại bằng phiếu nhập bù — chống kẹt hàng.
        ALLOWED.put(TransferStatus.SHIPPING, EnumSet.of(TransferStatus.RECEIVED, TransferStatus.CANCELLED));
        ALLOWED.put(TransferStatus.RECEIVED, EnumSet.noneOf(TransferStatus.class));
        ALLOWED.put(TransferStatus.CANCELLED, EnumSet.noneOf(TransferStatus.class));
    }

    private final StockTransferRepository transferRepository;
    private final StoreRepository storeRepository;
    private final GoodsReceiptItemRepository batchRepository;
    private final GoodsReceiptRepository receiptRepository;
    private final BatchStockViewRepository batchStockRepository;
    private final StockAdjustmentRepository adjustmentRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    public StockTransferService(StockTransferRepository transferRepository,
                                StoreRepository storeRepository,
                                GoodsReceiptItemRepository batchRepository,
                                GoodsReceiptRepository receiptRepository,
                                BatchStockViewRepository batchStockRepository,
                                StockAdjustmentRepository adjustmentRepository,
                                ProductRepository productRepository,
                                UserRepository userRepository,
                                AuditService auditService) {
        this.transferRepository = transferRepository;
        this.storeRepository = storeRepository;
        this.batchRepository = batchRepository;
        this.receiptRepository = receiptRepository;
        this.batchStockRepository = batchStockRepository;
        this.adjustmentRepository = adjustmentRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    /** Phiếu liên quan chi nhánh đang xem (nguồn hoặc đích); ADMIN toàn chuỗi → tất cả. */
    public List<StockTransferResponse> list() {
        Long storeId = StoreContext.currentStoreId();
        var list = storeId == null
                ? transferRepository.findAllByOrderByIdDesc()
                : transferRepository.findForStore(storeId);
        return list.stream().map(StockTransferResponse::from).toList();
    }

    public StockTransferResponse get(Long id) {
        return StockTransferResponse.from(loadVisible(id));
    }

    /** Tạo phiếu (PENDING). Nguồn = chi nhánh đang xem; đích = req.destStoreId. */
    @Transactional
    public StockTransferResponse create(CreateTransferRequest req) {
        Long sourceId = StoreContext.requireStoreId();
        if (req.destStoreId().equals(sourceId))
            throw new BadRequestException("Chi nhánh đích phải khác chi nhánh nguồn");
        Store source = storeRepository.getReferenceById(sourceId);
        Store dest = storeRepository.findById(req.destStoreId())
                .orElseThrow(() -> NotFoundException.of("chi nhánh đích", req.destStoreId()));

        StockTransfer t = new StockTransfer();
        t.setCode(generateCode());
        t.setSourceStore(source);
        t.setDestStore(dest);
        t.setStatus(TransferStatus.PENDING);
        t.setNote(req.note());
        t.setCreatedBy(userRepository.getReferenceById(SecurityUtils.currentUserId()));

        for (CreateTransferRequest.Line line : req.items()) {
            GoodsReceiptItem batch = batchRepository.findById(line.batchId())
                    .orElseThrow(() -> NotFoundException.of("lô", line.batchId()));
            // Lô phải thuộc chi nhánh NGUỒN (không cho điều chuyển lô của chi nhánh khác).
            if (!batch.getReceipt().getStore().getId().equals(sourceId))
                throw new BadRequestException("Lô #" + line.batchId() + " không thuộc chi nhánh nguồn");
            StockTransferItem it = new StockTransferItem();
            it.setBatch(batch);
            it.setProduct(batch.getProduct());
            it.setQuantity(line.quantity());
            it.setExpiryDate(batch.getExpiryDate());
            it.setCostPrice(batch.getImportPrice());   // giá vốn theo lô — chuyển kho không đổi giá vốn
            t.addItem(it);
        }
        StockTransfer saved = transferRepository.save(t);
        auditService.log("CREATE_TRANSFER", "STOCK_TRANSFER", saved.getId(),
                "Lập phiếu điều chuyển " + saved.getCode() + " → CN#" + req.destStoreId());
        return StockTransferResponse.from(saved);
    }

    /** PENDING → SHIPPING: trừ tồn KHO chi nhánh nguồn qua adjustment TRANSFER_OUT (có khóa + kiểm tồn). */
    @Transactional
    public StockTransferResponse ship(Long id) {
        StockTransfer t = transferRepository.findByIdForUpdate(id)
                .orElseThrow(() -> NotFoundException.of("phiếu điều chuyển", id));
        StoreContext.assertSameStore(t.getSourceStore().getId());   // chỉ chi nhánh NGUỒN được ship
        assertTransition(t, TransferStatus.SHIPPING);

        // Khóa tồn TẤT CẢ sản phẩm theo THỨ TỰ TOÀN CỤC (id tăng dần) trước khi đọc view —
        // cùng thứ tự khóa với SaleService để không deadlock chéo giữa ship và bán hàng.
        t.getItems().stream().map(it -> it.getProduct().getId()).distinct().sorted()
                .forEach(productRepository::findByIdForUpdate);
        for (StockTransferItem it : t.getItems()) {
            BatchStockView v = batchStockRepository.findById(it.getBatch().getId())
                    .orElseThrow(() -> NotFoundException.of("lô", it.getBatch().getId()));
            long inWarehouse = v.getInWarehouse() != null ? v.getInWarehouse() : 0L;
            if (it.getQuantity() > inWarehouse)
                throw new ConflictException("Lô #" + it.getBatch().getId() + " không đủ tồn kho để chuyển (còn "
                        + inWarehouse + ", cần " + it.getQuantity() + "). Hàng trên kệ phải lấy về kho trước.");

            StockAdjustment adj = new StockAdjustment();
            adj.setStore(t.getSourceStore());
            adj.setBatch(it.getBatch());
            adj.setQuantity(it.getQuantity());
            adj.setReason(AdjustmentReason.TRANSFER_OUT);
            adj.setNote("Điều chuyển " + t.getCode() + " → CN#" + t.getDestStore().getId());
            adj.setCreatedBy(userRepository.getReferenceById(SecurityUtils.currentUserId()));
            adjustmentRepository.save(adj);
        }
        t.setStatus(TransferStatus.SHIPPING);
        t.setShippedBy(userRepository.getReferenceById(SecurityUtils.currentUserId()));
        t.setShippedAt(java.time.LocalDateTime.now());
        auditService.log("SHIP_TRANSFER", "STOCK_TRANSFER", id, t.getCode());
        return StockTransferResponse.from(t);
    }

    /** SHIPPING → RECEIVED: tạo phiếu nhập nội bộ ở chi nhánh đích (tồn xuất hiện như lô mới, HSD gốc). */
    @Transactional
    public StockTransferResponse receive(Long id) {
        StockTransfer t = transferRepository.findByIdForUpdate(id)
                .orElseThrow(() -> NotFoundException.of("phiếu điều chuyển", id));
        StoreContext.assertSameStore(t.getDestStore().getId());     // chỉ chi nhánh ĐÍCH được nhận
        assertTransition(t, TransferStatus.RECEIVED);

        GoodsReceipt savedReceipt = createInternalReceipt(t.getDestStore(), t,
                "Nhận điều chuyển " + t.getCode() + " từ CN#" + t.getSourceStore().getId());

        t.setStatus(TransferStatus.RECEIVED);
        t.setReceivedBy(userRepository.getReferenceById(SecurityUtils.currentUserId()));
        t.setReceivedAt(java.time.LocalDateTime.now());
        t.setDestReceipt(savedReceipt);
        auditService.log("RECEIVE_TRANSFER", "STOCK_TRANSFER", id,
                t.getCode() + " → phiếu nhập " + savedReceipt.getCode());
        return StockTransferResponse.from(t);
    }

    /**
     * Hủy phiếu. PENDING: chưa xuất hàng → chỉ đổi trạng thái. SHIPPING (thu hồi): hàng đã rời kho nguồn →
     * HOÀN lại bằng phiếu nhập bù ở chính kho NGUỒN (giữ HSD/giá vốn gốc) để không kẹt hàng. Chỉ chi nhánh nguồn hủy.
     */
    @Transactional
    public StockTransferResponse cancel(Long id, String reason) {
        StockTransfer t = transferRepository.findByIdForUpdate(id)
                .orElseThrow(() -> NotFoundException.of("phiếu điều chuyển", id));
        StoreContext.assertSameStore(t.getSourceStore().getId());
        assertTransition(t, TransferStatus.CANCELLED);
        if (t.getStatus() == TransferStatus.SHIPPING) {
            // Thu hồi: tạo phiếu nhập bù ở kho NGUỒN, hoàn đúng số lượng đã xuất (chống kẹt hàng — M2).
            createInternalReceipt(t.getSourceStore(), t,
                    "Hoàn hàng do hủy điều chuyển " + t.getCode());
        }
        t.setStatus(TransferStatus.CANCELLED);
        t.setCancelledBy(userRepository.getReferenceById(SecurityUtils.currentUserId()));
        t.setCancelledAt(java.time.LocalDateTime.now());
        t.setCancelReason(reason);
        auditService.log("CANCEL_TRANSFER", "STOCK_TRANSFER", id, t.getCode() + " — " + reason);
        return StockTransferResponse.from(t);
    }

    // ---- helpers ----

    /** Tạo phiếu nhập NỘI BỘ (source=TRANSFER, không NCC) cho một kho từ các dòng của phiếu điều chuyển,
     *  giữ nguyên HSD & giá vốn theo lô gốc → tồn xuất hiện như lô mới, FEFO theo HSD vẫn đúng. */
    private GoodsReceipt createInternalReceipt(Store store, StockTransfer t, String note) {
        GoodsReceipt rcpt = new GoodsReceipt();
        rcpt.setCode(genReceiptCode());
        rcpt.setStore(store);
        rcpt.setSupplier(null);
        rcpt.setSource(ReceiptSource.TRANSFER);
        rcpt.setCreatedBy(userRepository.getReferenceById(SecurityUtils.currentUserId()));
        rcpt.setNote(note);
        java.math.BigDecimal total = java.math.BigDecimal.ZERO;
        for (StockTransferItem it : t.getItems()) {
            GoodsReceiptItem gi = new GoodsReceiptItem();
            gi.setProduct(it.getProduct());
            gi.setQuantity(it.getQuantity());
            gi.setImportPrice(it.getCostPrice());
            gi.setExpiryDate(it.getExpiryDate());
            rcpt.addItem(gi);
            total = total.add(it.getCostPrice().multiply(java.math.BigDecimal.valueOf(it.getQuantity())));
        }
        rcpt.setTotalAmount(total);
        return receiptRepository.save(rcpt);
    }

    private void assertTransition(StockTransfer t, TransferStatus to) {
        if (!ALLOWED.getOrDefault(t.getStatus(), Set.of()).contains(to))
            throw new BadRequestException("Không thể chuyển trạng thái " + t.getStatus() + " → " + to);
    }

    private StockTransfer loadVisible(Long id) {
        StockTransfer t = transferRepository.findById(id)
                .orElseThrow(() -> NotFoundException.of("phiếu điều chuyển", id));
        Long storeId = StoreContext.currentStoreId();
        if (storeId != null
                && !t.getSourceStore().getId().equals(storeId)
                && !t.getDestStore().getId().equals(storeId))
            throw new BadRequestException("Phiếu điều chuyển không thuộc chi nhánh của bạn");
        return t;
    }

    private String generateCode() {
        long seq = transferRepository.countByCodeStartingWith(CodeGenerator.datePrefix(CODE_PREFIX)) + 1;
        return CodeGenerator.build(CODE_PREFIX, seq, 3);
    }

    private String genReceiptCode() {
        long seq = receiptRepository.countByCodeStartingWith(CodeGenerator.datePrefix("PN")) + 1;
        return CodeGenerator.build("PN", seq, 3);
    }
}
