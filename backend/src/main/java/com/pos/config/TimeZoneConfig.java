package com.pos.config;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

import java.util.TimeZone;

/**
 * Ghim múi giờ MẶC ĐỊNH của JVM = Asia/Ho_Chi_Minh.
 *
 * <p>Datasource (serverTimezone) và Jackson đã được ghim ở {@code application.yml}, nhưng {@code LocalDate.now()} /
 * {@code LocalDateTime.now()} trong service vẫn dùng múi giờ mặc định của JVM. Nếu JVM chạy ở UTC (hay khác) trong
 * khi DB ở giờ VN, biên "hôm nay"/giờ/ngày của dashboard & báo cáo sẽ lệch (hóa đơn cận nửa đêm rơi sai ngày).
 * Cố định JVM về giờ VN để mọi mốc thời gian nhất quán với DB.</p>
 */
@Configuration
public class TimeZoneConfig {

    @PostConstruct
    public void init() {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
    }
}
