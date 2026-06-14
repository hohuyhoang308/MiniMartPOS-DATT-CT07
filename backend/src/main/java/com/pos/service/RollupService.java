package com.pos.service;

import com.pos.repository.DailySalesRollupRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Job tổng hợp DOANH THU NGÀY vào {@code daily_sales_rollup} (Obj 2).
 *
 * <p><b>Kiến trúc & trade-off:</b> báo cáo theo thời gian (doanh thu/lợi nhuận nhiều tháng/năm × nhiều
 * chi nhánh) nếu quét bảng {@code invoices} thô sẽ ngày càng chậm khi dữ liệu lớn. Ta tách thành
 * hai pha: (1) GHI nhanh ở đường bán hàng (không đụng tới), (2) TỔNG HỢP nguội bằng cron mỗi đêm vào
 * bảng rollup nhỏ (1 dòng/chi nhánh/ngày). Báo cáo đọc rollup → độ phức tạp theo SỐ NGÀY, không theo
 * SỐ HÓA ĐƠN. Bảng rollup là dữ liệu suy ra, có thể dựng lại bất cứ lúc nào từ hóa đơn (idempotent upsert),
 * nên không có rủi ro "nguồn sự thật kép".</p>
 */
@Service
public class RollupService {

    private static final Logger log = LoggerFactory.getLogger(RollupService.class);

    private final DailySalesRollupRepository repository;

    public RollupService(DailySalesRollupRepository repository) {
        this.repository = repository;
    }

    /** Tổng hợp lại HÔM QUA + HÔM NAY mỗi đêm 00:30 (hôm nay để chốt phần cuối ngày trước nửa đêm). */
    @Scheduled(cron = "0 30 0 * * *", zone = "Asia/Ho_Chi_Minh")
    @Transactional
    public void nightlyRollup() {
        LocalDate today = LocalDate.now();
        repository.upsertRange(today.minusDays(1), today);
        log.info("Rollup doanh thu ngày: đã tổng hợp {} và {}", today.minusDays(1), today);
    }

    /** Tổng hợp một khoảng ngày (dùng cho backfill / chạy tay khi cần dựng lại). */
    @Transactional
    public void rollupRange(LocalDate from, LocalDate to) {
        repository.upsertRange(from, to);
    }
}
