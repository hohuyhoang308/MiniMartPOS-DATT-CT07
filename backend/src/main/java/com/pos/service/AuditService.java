package com.pos.service;

import com.pos.entity.AuditLog;
import com.pos.repository.AuditLogRepository;
import com.pos.security.CustomUserDetails;
import com.pos.security.SecurityUtils;
import com.pos.security.StoreContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Ghi nhật ký kiểm toán cho hành động nhạy cảm. Tham gia transaction của lời gọi (cùng commit/rollback). */
@Service
public class AuditService {

    private final AuditLogRepository repository;

    public AuditService(AuditLogRepository repository) {
        this.repository = repository;
    }

    /**
     * 200 vết mới nhất. ADMIN toàn chuỗi → toàn bộ; MANAGER/STAFF (gắn cửa hàng) → chỉ thao tác
     * phát sinh trong cửa hàng của mình.
     */
    @Transactional(readOnly = true)
    public List<AuditLog> recent() {
        // Dùng StoreContext (không phải store cố định của user) để ADMIN khi "đi sâu" 1 chi nhánh
        // qua X-Store-Id thấy đúng log của chi nhánh đó; MANAGER/STAFF vẫn bị chốt về cửa hàng mình.
        Long scope = StoreContext.currentStoreId();
        return scope != null
                ? repository.findTop200ByStoreIdOrderByCreatedAtDesc(scope)
                : repository.findTop200ByOrderByCreatedAtDesc();
    }

    @Transactional
    public void log(String action, String targetType, Long targetId, String detail) {
        AuditLog a = new AuditLog();
        // Có thể được gọi từ job nền (không có phiên đăng nhập) → để actor/cửa hàng null, không ném lỗi.
        try {
            CustomUserDetails me = SecurityUtils.currentUser();
            a.setActorUserId(me.getId());
            a.setActorUsername(me.getUsername());
            a.setStoreId(StoreContext.currentStoreId());   // cửa hàng phát sinh thao tác (null = toàn chuỗi)
        } catch (RuntimeException noAuthContext) {
            // không có người dùng đăng nhập (vd job dọn HĐ QR quá hạn) — ghi như thao tác hệ thống
        }
        a.setAction(action);
        a.setTargetType(targetType);
        a.setTargetId(targetId);
        a.setDetail(detail != null && detail.length() > 500 ? detail.substring(0, 500) : detail);
        repository.save(a);
    }
}
