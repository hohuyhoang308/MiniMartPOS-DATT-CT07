package com.pos.config;

import com.pos.repository.DailySalesRollupRepository;
import com.pos.service.RollupService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Backfill bảng rollup khi khởi động nếu còn rỗng (Obj 2) — chạy SAU seeder hóa đơn (@Order 30 > 20).
 * Dựng tổng hợp doanh thu cho ~2 năm gần nhất từ hóa đơn hiện có để báo cáo có dữ liệu ngay.
 */
@Component
@Order(30)
public class RollupBackfillInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(RollupBackfillInitializer.class);

    private final DailySalesRollupRepository repository;
    private final RollupService rollupService;

    public RollupBackfillInitializer(DailySalesRollupRepository repository, RollupService rollupService) {
        this.repository = repository;
        this.rollupService = rollupService;
    }

    @Override
    public void run(String... args) {
        if (repository.count() > 0) return;   // đã có rollup → không backfill lại
        LocalDate today = LocalDate.now();
        rollupService.rollupRange(today.minusDays(730), today);
        log.info("Backfill rollup doanh thu: đã tổng hợp {} dòng cho ~2 năm gần nhất", repository.count());
    }
}
