package com.pos.job;

import com.pos.repository.projection.LoyaltyMismatchRow;
import com.pos.service.LoyaltyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Đối soát điểm tích lũy định kỳ: kiểm tra số dư customers.loyalty_points có khớp tổng sổ cái (Σ delta) không.
 * CHỈ GHI LOG cảnh báo (không tự sửa) để phát hiện sớm chỗ cập nhật điểm mà quên ghi sổ — việc sửa do người
 * vận hành quyết định, tránh che giấu lỗi bằng auto-correct.
 */
@Component
public class LoyaltyReconcileJob {

    private static final Logger log = LoggerFactory.getLogger(LoyaltyReconcileJob.class);

    private final LoyaltyService loyaltyService;

    public LoyaltyReconcileJob(LoyaltyService loyaltyService) {
        this.loyaltyService = loyaltyService;
    }

    @Scheduled(cron = "0 30 2 * * *") // 02:30 mỗi ngày (giờ thấp điểm)
    public void reconcile() {
        try {
            List<LoyaltyMismatchRow> bad = loyaltyService.findMismatches();
            if (bad.isEmpty()) {
                log.info("Đối soát điểm tích lũy: số dư khách KHỚP sổ cái toàn bộ.");
                return;
            }
            log.warn("Đối soát điểm tích lũy: {} khách có số dư LỆCH sổ cái:", bad.size());
            for (LoyaltyMismatchRow r : bad) {
                log.warn("  - KH #{} {}: số dư = {} nhưng tổng sổ cái = {}",
                        r.getCustomerId(), r.getCustomerName(), r.getStored(), r.getLedger());
            }
        } catch (Exception e) {
            log.error("Lỗi đối soát điểm tích lũy: {}", e.getMessage());
        }
    }
}
