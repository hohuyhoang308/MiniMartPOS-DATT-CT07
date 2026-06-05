package com.pos.service;

import com.pos.dto.config.StoreConfigRequest;
import com.pos.dto.config.StoreConfigResponse;
import com.pos.entity.StoreConfig;
import com.pos.repository.StoreConfigRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Cấu hình cửa hàng & tích hợp (FR10, FR-A6). Bảng 1 dòng (singleton id=1). */
@Service
@Transactional(readOnly = true)
public class StoreConfigService {

    private final StoreConfigRepository repository;
    private final AuditService auditService;

    public StoreConfigService(StoreConfigRepository repository, AuditService auditService) {
        this.repository = repository;
        this.auditService = auditService;
    }

    public StoreConfig getEntity() {
        return repository.findById(StoreConfig.SINGLETON_ID).orElseGet(() -> {
            StoreConfig c = new StoreConfig();
            c.setId(StoreConfig.SINGLETON_ID);
            c.setName("Cửa hàng tiện lợi");
            return repository.save(c);
        });
    }

    public StoreConfigResponse get() {
        return StoreConfigResponse.from(getEntity());
    }

    @Transactional
    public StoreConfigResponse update(StoreConfigRequest req) {
        StoreConfig c = getEntity();
        c.setName(req.name());
        c.setAddress(req.address());
        c.setPhone(req.phone());
        c.setTaxCode(req.taxCode());
        c.setLogoUrl(req.logoUrl());
        c.setBankName(req.bankName());
        c.setBankBin(req.bankBin());
        c.setBankAccountNo(req.bankAccountNo());
        c.setBankAccountName(req.bankAccountName());
        c.setTransferPrefix(req.transferPrefix());
        // Token nhạy cảm: chỉ cập nhật khi client gửi giá trị mới (không gửi → giữ nguyên)
        if (req.web2mApiUrl() != null && !req.web2mApiUrl().isBlank()) c.setWeb2mApiUrl(req.web2mApiUrl());
        if (req.telegramBotToken() != null && !req.telegramBotToken().isBlank()) c.setTelegramBotToken(req.telegramBotToken());
        if (req.telegramEnabled() != null) c.setTelegramEnabled(req.telegramEnabled());
        if (req.notifyPayment() != null) c.setNotifyPayment(req.notifyPayment());
        if (req.notifyLowStock() != null) c.setNotifyLowStock(req.notifyLowStock());
        if (req.notifyNewInvoice() != null) c.setNotifyNewInvoice(req.notifyNewInvoice());
        StoreConfig saved = repository.save(c);
        auditService.log("UPDATE_CONFIG", "STORE_CONFIG",
                saved.getId() != null ? saved.getId().longValue() : null,
                "Cập nhật cấu hình cửa hàng / tích hợp");
        return StoreConfigResponse.from(saved);
    }
}
