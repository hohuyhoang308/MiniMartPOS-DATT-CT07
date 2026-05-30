package com.pos.service;

import com.pos.entity.AuditLog;
import com.pos.repository.AuditLogRepository;
import com.pos.security.CustomUserDetails;
import com.pos.security.SecurityUtils;
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

    /** 200 vết mới nhất — màn xem nhật ký (admin). */
    @Transactional(readOnly = true)
    public List<AuditLog> recent() {
        return repository.findTop200ByOrderByCreatedAtDesc();
    }

    @Transactional
    public void log(String action, String targetType, Long targetId, String detail) {
        AuditLog a = new AuditLog();
        CustomUserDetails me = SecurityUtils.currentUser();
        if (me != null) {
            a.setActorUserId(me.getId());
            a.setActorUsername(me.getUsername());
        }
        a.setAction(action);
        a.setTargetType(targetType);
        a.setTargetId(targetId);
        a.setDetail(detail != null && detail.length() > 500 ? detail.substring(0, 500) : detail);
        repository.save(a);
    }
}
