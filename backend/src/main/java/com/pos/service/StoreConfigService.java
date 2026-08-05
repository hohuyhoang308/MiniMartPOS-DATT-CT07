package com.pos.service;

import com.pos.dto.config.StoreConfigRequest;
import com.pos.dto.config.StoreConfigResponse;
import com.pos.entity.Store;
import com.pos.entity.StoreConfig;
import com.pos.exception.NotFoundException;
import com.pos.repository.StoreConfigRepository;
import com.pos.repository.StoreRepository;
import com.pos.security.StoreContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Cấu hình TỪNG CHI NHÁNH & tích hợp (FR10, FR-A6) — mỗi chi nhánh một dòng, khóa = stores.id. */
@Service
@Transactional(readOnly = true)
public class StoreConfigService {

    private final StoreConfigRepository repository;
    private final StoreRepository storeRepository;
    private final AuditService auditService;

    public StoreConfigService(StoreConfigRepository repository, StoreRepository storeRepository,
                              AuditService auditService) {
        this.repository = repository;
        this.storeRepository = storeRepository;
        this.auditService = auditService;
    }

    /** Cấu hình của một chi nhánh; tự tạo dòng mặc định (lấy tên từ chi nhánh) nếu chưa có. */
    @Transactional
    public StoreConfig getEntity(Long storeId) {
        return repository.findById(storeId).orElseGet(() -> {
            Store store = storeRepository.findById(storeId)
                    .orElseThrow(() -> NotFoundException.of("chi nhánh", storeId));
            StoreConfig c = new StoreConfig();
            c.setId(storeId);
            c.setName(store.getName());
            c.setAddress(store.getAddress());
            c.setPhone(store.getPhone());
            return repository.save(c);
        });
    }

    /**
     * Cửa hàng cần cấu hình: ADMIN truyền {@code storeId} tường minh (chọn trên trang Cấu hình);
     * MANAGER/STAFF bỏ trống → tự lấy cửa hàng của họ. ADMIN bỏ trống → lỗi (yêu cầu chọn cửa hàng).
     */
    private Long resolveConfigStoreId(Long storeId) {
        return StoreContext.resolveTargetStoreId(storeId);
    }

    /** ĐỌC cấu hình — KHÔNG ghi DB. Chưa cấu hình → trả về mặc định tạm (lấy tên/địa chỉ từ chi nhánh). */
    public StoreConfigResponse get(Long storeId) {
        Long id = resolveConfigStoreId(storeId);
        return repository.findById(id)
                .map(StoreConfigResponse::from)
                .orElseGet(() -> {
                    Store store = storeRepository.findById(id)
                            .orElseThrow(() -> NotFoundException.of("chi nhánh", id));
                    StoreConfig c = new StoreConfig();
                    c.setId(id);
                    c.setName(store.getName());
                    c.setAddress(store.getAddress());
                    c.setPhone(store.getPhone());
                    return StoreConfigResponse.from(c);   // chỉ dựng để hiển thị, không lưu
                });
    }

    @Transactional
    public StoreConfigResponse update(Long storeId, StoreConfigRequest req) {
        StoreConfig c = getEntity(resolveConfigStoreId(storeId));
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
        auditService.log("UPDATE_CONFIG", "STORE_CONFIG", saved.getId(),
                "Cập nhật cấu hình chi nhánh / tích hợp");
        return StoreConfigResponse.from(saved);
    }
}
