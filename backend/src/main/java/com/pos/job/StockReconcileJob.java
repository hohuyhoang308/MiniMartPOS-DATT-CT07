package com.pos.job;

import com.pos.entity.view.BatchStockView;
import com.pos.repository.view.BatchStockViewRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Đối soát TOÀN VẸN TỒN KHO định kỳ (defense-in-depth cho bất biến on_shelf ≥ 0, in_warehouse ≥ 0).
 * Khóa bi quan trên sản phẩm đã ngăn bán/lên kệ/trả vượt tồn ở tầng Java; job này chỉ là lưới an toàn:
 * nếu vẫn xuất hiện lô tồn ÂM (do bug hoặc thao tác SQL tay), GHI LOG cảnh báo để phát hiện sớm.
 * Read-only, KHÔNG tự sửa.
 */
@Component
public class StockReconcileJob {

    private static final Logger log = LoggerFactory.getLogger(StockReconcileJob.class);

    private final BatchStockViewRepository batchStockRepository;

    public StockReconcileJob(BatchStockViewRepository batchStockRepository) {
        this.batchStockRepository = batchStockRepository;
    }

    @Scheduled(cron = "0 45 2 * * *") // 02:45 mỗi ngày
    @Transactional(readOnly = true)
    public void reconcile() {
        try {
            List<BatchStockView> bad = batchStockRepository.findNegativeStock();
            if (bad.isEmpty()) {
                log.info("Đối soát tồn kho: không có lô tồn âm — toàn vẹn OK.");
                return;
            }
            log.warn("Đối soát tồn kho: {} lô có tồn ÂM (kệ/kho) — CẦN KIỂM TRA:", bad.size());
            for (BatchStockView v : bad) {
                log.warn("  - Lô #{} (SP #{}): tồn kệ = {}, tồn kho = {}",
                        v.getBatchId(), v.getProductId(), v.getOnShelf(), v.getInWarehouse());
            }
        } catch (Exception e) {
            log.error("Lỗi đối soát tồn kho: {}", e.getMessage());
        }
    }
}
