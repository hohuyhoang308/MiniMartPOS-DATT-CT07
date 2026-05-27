package com.pos.service;

import com.pos.entity.ShelfTransfer;
import com.pos.entity.User;
import com.pos.entity.view.BatchStockView;
import com.pos.exception.BadRequestException;
import com.pos.exception.NotFoundException;
import com.pos.repository.GoodsReceiptItemRepository;
import com.pos.repository.ProductRepository;
import com.pos.repository.ShelfTransferRepository;
import com.pos.repository.UserRepository;
import com.pos.repository.view.BatchStockViewRepository;
import com.pos.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Quản lý KỆ: đưa hàng từ KHO lên KỆ (chọn lô theo FIFO/HSD). */
@Service
@Transactional(readOnly = true)
public class ShelfService {

    private final BatchStockViewRepository batchStockRepository;
    private final ShelfTransferRepository shelfTransferRepository;
    private final GoodsReceiptItemRepository batchRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    public ShelfService(BatchStockViewRepository batchStockRepository,
                        ShelfTransferRepository shelfTransferRepository,
                        GoodsReceiptItemRepository batchRepository,
                        UserRepository userRepository,
                        ProductRepository productRepository) {
        this.batchStockRepository = batchStockRepository;
        this.shelfTransferRepository = shelfTransferRepository;
        this.batchRepository = batchRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
    }

    /**
     * Đưa {@code quantity} sản phẩm từ KHO lên KỆ: rút từ các lô trong kho theo FIFO/HSD
     * (lô cận hạn trước), tạo phiếu chuyển. Trả về số lượng thực tế đã lên kệ (≤ tồn kho hiện có).
     */
    @Transactional
    public int replenishShelf(Long productId, int quantity) {
        if (quantity <= 0) throw new BadRequestException("Số lượng lên kệ phải lớn hơn 0");
        productRepository.findById(productId).orElseThrow(() -> NotFoundException.of("sản phẩm", productId));
        User user = userRepository.findById(SecurityUtils.currentUserId()).orElse(null);

        int need = quantity, moved = 0;
        for (BatchStockView b : batchStockRepository.findWarehouseBatchesFifo(productId)) {
            if (need <= 0) break;
            int avail = b.getInWarehouse() != null ? b.getInWarehouse().intValue() : 0;
            if (avail <= 0) continue;
            int take = Math.min(avail, need);
            ShelfTransfer st = new ShelfTransfer();
            st.setBatch(batchRepository.getReferenceById(b.getBatchId()));
            st.setQuantity(take);
            st.setCreatedBy(user);
            shelfTransferRepository.save(st);
            moved += take;
            need -= take;
        }
        if (moved == 0) throw new BadRequestException("Kho không còn hàng để lên kệ cho sản phẩm này");
        return moved;
    }
}
