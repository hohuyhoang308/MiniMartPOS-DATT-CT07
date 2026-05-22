package com.pos.job;

import com.pos.service.Web2mSyncService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Job nền định kỳ đối soát thanh toán WEB2M (FR-A4).
 * Bật/tắt qua app.web2m.enabled; chu kỳ qua app.web2m.poll-interval-ms.
 */
@Component
public class Web2mSyncJob {

    private static final Logger log = LoggerFactory.getLogger(Web2mSyncJob.class);

    private final Web2mSyncService syncService;
    private final boolean enabled;

    public Web2mSyncJob(Web2mSyncService syncService,
                        @Value("${app.web2m.enabled:false}") boolean enabled) {
        this.syncService = syncService;
        this.enabled = enabled;
    }

    @Scheduled(fixedDelayString = "${app.web2m.poll-interval-ms:30000}")
    public void poll() {
        if (!enabled) return;
        try {
            int matched = syncService.sync();
            if (matched > 0) log.info("WEB2M: xác nhận {} giao dịch thanh toán", matched);
        } catch (Exception e) {
            log.debug("WEB2M sync bỏ qua: {}", e.getMessage());
        }
    }
}
