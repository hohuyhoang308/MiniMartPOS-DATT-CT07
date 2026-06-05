package com.pos.service;

import com.pos.dto.shelf.ShelfItemResponse;
import com.pos.dto.shelf.ShelfRequest;
import com.pos.dto.shelf.ShelfResponse;
import com.pos.entity.Product;
import com.pos.entity.Shelf;
import com.pos.entity.ShelfReturn;
import com.pos.entity.ShelfTransfer;
import com.pos.entity.User;
import com.pos.entity.enums.CommonStatus;
import com.pos.entity.view.BatchStockView;
import com.pos.exception.BadRequestException;
import com.pos.exception.NotFoundException;
import com.pos.repository.*;
import com.pos.repository.view.BatchStockViewRepository;
import com.pos.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/** Quản lý KỆ vật lý: CRUD kệ + đưa hàng từ KHO lên một KỆ cụ thể (FIFO/HSD) + xem nội dung kệ. */
@Service
@Transactional(readOnly = true)
public class ShelfService {

    private final ShelfRepository shelfRepository;
    private final BatchStockViewRepository batchStockRepository;
    private final ShelfTransferRepository shelfTransferRepository;
    private final ShelfReturnRepository shelfReturnRepository;
    private final GoodsReceiptItemRepository batchRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final AuditService auditService;

    public ShelfService(ShelfRepository shelfRepository,
                        BatchStockViewRepository batchStockRepository,
                        ShelfTransferRepository shelfTransferRepository,
                        ShelfReturnRepository shelfReturnRepository,
                        GoodsReceiptItemRepository batchRepository,
                        UserRepository userRepository,
                        ProductRepository productRepository,
                        AuditService auditService) {
        this.shelfRepository = shelfRepository;
        this.batchStockRepository = batchStockRepository;
        this.shelfTransferRepository = shelfTransferRepository;
        this.shelfReturnRepository = shelfReturnRepository;
        this.batchRepository = batchRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.auditService = auditService;
    }

    // ---------- CRUD kệ ----------

    /** Danh sách kệ kèm thống kê đang chứa (số mặt hàng + tổng số lượng trên kệ). */
    public List<ShelfResponse> listShelves() {
        Map<Long, Set<Long>> productsByShelf = new HashMap<>();
        Map<Long, Long> qtyByShelf = new HashMap<>();
        for (BatchStockView v : batchStockRepository.findAll()) {
            if (v.getShelfId() == null || v.getOnShelf() == null || v.getOnShelf() <= 0) continue;
            productsByShelf.computeIfAbsent(v.getShelfId(), k -> new HashSet<>()).add(v.getProductId());
            qtyByShelf.merge(v.getShelfId(), v.getOnShelf(), Long::sum);
        }
        return shelfRepository.findAllByOrderByCode().stream()
                .map(s -> ShelfResponse.from(s,
                        productsByShelf.getOrDefault(s.getId(), Set.of()).size(),
                        qtyByShelf.getOrDefault(s.getId(), 0L)))
                .toList();
    }

    @Transactional
    public ShelfResponse create(ShelfRequest req) {
        if (shelfRepository.existsByCodeIgnoreCase(req.code().trim())) {
            throw new BadRequestException("Mã kệ đã tồn tại: " + req.code());
        }
        Shelf s = new Shelf();
        apply(s, req);
        return ShelfResponse.from(shelfRepository.save(s), 0, 0);
    }

    @Transactional
    public ShelfResponse update(Long id, ShelfRequest req) {
        Shelf s = getOrThrow(id);
        if (!s.getCode().equalsIgnoreCase(req.code().trim())
                && shelfRepository.existsByCodeIgnoreCase(req.code().trim())) {
            throw new BadRequestException("Mã kệ đã tồn tại: " + req.code());
        }
        apply(s, req);
        return ShelfResponse.from(shelfRepository.save(s), 0, 0);
    }

    @Transactional
    public void delete(Long id) {
        Shelf s = getOrThrow(id);
        boolean hasStock = batchStockRepository.findByShelf(id).stream()
                .anyMatch(v -> v.getOnShelf() != null && v.getOnShelf() > 0);
        if (hasStock) {
            throw new BadRequestException("Kệ còn hàng — không thể xoá. Hãy lấy hết hàng về kho hoặc bán hết trước.");
        }
        // Còn lịch sử lên kệ / lấy về kho tham chiếu kệ này → khoá FK, chặn bằng thông báo rõ ràng.
        if (shelfTransferRepository.countByShelfId(id) > 0 || shelfReturnRepository.countByShelfId(id) > 0) {
            throw new BadRequestException("Kệ đã có lịch sử lên kệ — không thể xoá. Hãy ngừng dùng (đặt trạng thái Ngừng) thay vì xoá.");
        }
        shelfRepository.delete(s);
    }

    // ---------- Nội dung kệ ----------

    /** Các LÔ đang nằm trên kệ: sản phẩm gì, HSD, số lượng. */
    public List<ShelfItemResponse> shelfInventory(Long shelfId) {
        getOrThrow(shelfId);
        List<BatchStockView> batches = batchStockRepository.findByShelf(shelfId);
        Map<Long, String> names = productNames(batches);
        return batches.stream().map(v -> ShelfItemResponse.from(v, names.get(v.getProductId()))).toList();
    }

    // ---------- Lên kệ ----------

    /**
     * Đưa {@code quantity} sản phẩm từ KHO lên KỆ {@code shelfId}: rút từ các lô trong kho theo
     * FIFO/HSD, tạo phiếu chuyển. Tôn trọng quy ước "một lô chỉ ở một kệ" (bỏ qua lô đã thuộc kệ khác).
     * Trả về số lượng thực tế đã lên kệ.
     */
    @Transactional
    public int replenishShelf(Long productId, Long shelfId, int quantity) {
        if (quantity <= 0) throw new BadRequestException("Số lượng lên kệ phải lớn hơn 0");
        // Khoá tồn sản phẩm TRƯỚC khi đọc tồn kho từ view → không lên kệ vượt tồn kho khi chạy đồng thời.
        productRepository.findByIdForUpdate(productId).orElseThrow(() -> NotFoundException.of("sản phẩm", productId));
        Shelf shelf = getOrThrow(shelfId);
        User user = userRepository.findById(SecurityUtils.currentUserId()).orElse(null);

        // Giới hạn theo SỨC CHỨA của kệ (0 = không giới hạn)
        int cap = shelf.getCapacity() != null ? shelf.getCapacity() : 0;
        if (cap > 0) {
            long room = cap - currentShelfTotal(shelfId);
            if (room <= 0) {
                throw new BadRequestException("Kệ " + shelf.getCode() + " đã đầy (sức chứa " + cap + ").");
            }
            if (quantity > room) quantity = (int) room; // chỉ lên hàng tới khi đầy kệ
        }

        int need = quantity, moved = 0;
        for (BatchStockView b : batchStockRepository.findWarehouseBatchesFifo(productId)) {
            if (need <= 0) break;
            if (b.getShelfId() != null && !b.getShelfId().equals(shelfId)) continue; // lô đã thuộc kệ khác
            int avail = b.getInWarehouse() != null ? b.getInWarehouse().intValue() : 0;
            if (avail <= 0) continue;
            int take = Math.min(avail, need);
            ShelfTransfer st = new ShelfTransfer();
            st.setBatch(batchRepository.getReferenceById(b.getBatchId()));
            st.setShelf(shelf);
            st.setQuantity(take);
            st.setCreatedBy(user);
            shelfTransferRepository.save(st);
            moved += take;
            need -= take;
        }
        if (moved == 0) {
            throw new BadRequestException("Không có lô phù hợp trong kho để lên kệ này (lô có thể đã thuộc kệ khác).");
        }
        auditService.log("SHELVE", "SHELF", shelfId,
                "Lên kệ " + moved + " sản phẩm #" + productId + " lên kệ " + shelf.getCode());
        return moved;
    }

    // ---------- Lấy hàng từ kệ về kho (đặt xuống) ----------

    /**
     * Lấy {@code quantity} sản phẩm của một LÔ từ KỆ về lại KHO (đối ứng với lên kệ).
     * Số lượng tối đa = tồn của lô đó trên kệ. Trả về số lượng thực tế đã lấy xuống.
     */
    @Transactional
    public long returnToWarehouse(Long batchId, int quantity) {
        if (quantity <= 0) throw new BadRequestException("Số lượng lấy về kho phải lớn hơn 0");
        // Khoá tồn sản phẩm của lô TRƯỚC khi đọc tồn kệ → tránh đua với giao dịch bán (tồn kệ âm).
        com.pos.entity.GoodsReceiptItem batch = batchRepository.findById(batchId)
                .orElseThrow(() -> NotFoundException.of("lô", batchId));
        productRepository.findByIdForUpdate(batch.getProduct().getId());
        BatchStockView v = batchStockRepository.findById(batchId)
                .orElseThrow(() -> NotFoundException.of("lô", batchId));
        long onShelf = v.getOnShelf() != null ? v.getOnShelf() : 0L;
        if (v.getShelfId() == null || onShelf <= 0) {
            throw new BadRequestException("Lô này không còn hàng trên kệ để lấy về kho.");
        }
        if (quantity > onShelf) quantity = (int) onShelf; // chỉ lấy tối đa bằng tồn kệ
        Shelf shelf = getOrThrow(v.getShelfId());
        User user = userRepository.findById(SecurityUtils.currentUserId()).orElse(null);

        ShelfReturn r = new ShelfReturn();
        r.setBatch(batchRepository.getReferenceById(batchId));
        r.setShelf(shelf);
        r.setQuantity(quantity);
        r.setCreatedBy(user);
        shelfReturnRepository.save(r);
        auditService.log("SHELF_RETURN", "SHELF", shelf.getId(),
                "Lấy về kho " + quantity + " từ lô #" + batchId + " (kệ " + shelf.getCode() + ")");
        return quantity;
    }

    // ---------- helpers ----------

    private void apply(Shelf s, ShelfRequest req) {
        s.setCode(req.code().trim());
        s.setName(req.name());
        s.setCapacity(req.capacity() != null ? req.capacity() : 0);
        s.setStatus(req.status() != null ? req.status() : CommonStatus.ACTIVE);
    }

    /** Tổng số sản phẩm đang nằm trên một kệ (để tính chỗ trống theo sức chứa). */
    private long currentShelfTotal(Long shelfId) {
        return batchStockRepository.findByShelf(shelfId).stream()
                .mapToLong(v -> v.getOnShelf() != null ? v.getOnShelf() : 0L).sum();
    }

    private Map<Long, String> productNames(List<BatchStockView> batches) {
        Set<Long> ids = batches.stream().map(BatchStockView::getProductId).collect(Collectors.toSet());
        return productRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Product::getId, Product::getName));
    }

    private Shelf getOrThrow(Long id) {
        return shelfRepository.findById(id).orElseThrow(() -> NotFoundException.of("kệ", id));
    }
}
