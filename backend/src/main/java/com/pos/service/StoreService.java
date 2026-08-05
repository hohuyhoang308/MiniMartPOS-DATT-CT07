package com.pos.service;

import com.pos.dto.store.StoreRequest;
import com.pos.dto.store.StoreResponse;
import com.pos.entity.Store;
import com.pos.entity.enums.CommonStatus;
import com.pos.exception.BadRequestException;
import com.pos.exception.NotFoundException;
import com.pos.repository.StoreRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Quản lý CHI NHÁNH trong chuỗi (đa chuỗi) — chỉ ADMIN (toàn chuỗi). */
@Service
@Transactional(readOnly = true)
public class StoreService {

    private final StoreRepository repository;
    private final AuditService auditService;

    public StoreService(StoreRepository repository, AuditService auditService) {
        this.repository = repository;
        this.auditService = auditService;
    }

    public List<StoreResponse> findAll() {
        return repository.findAll().stream()
                .sorted((a, b) -> a.getCode().compareToIgnoreCase(b.getCode()))
                .map(StoreResponse::from).toList();
    }

    @Transactional
    public StoreResponse create(StoreRequest req) {
        if (repository.existsByCode(req.code().trim())) {
            throw new BadRequestException("Mã chi nhánh đã tồn tại: " + req.code());
        }
        Store s = new Store();
        apply(s, req);
        Store saved = repository.save(s);
        auditService.log("CREATE_STORE", "STORE", saved.getId(),
                "Thêm chi nhánh " + saved.getCode() + " — " + saved.getName());
        return StoreResponse.from(saved);
    }

    @Transactional
    public StoreResponse update(Long id, StoreRequest req) {
        Store s = repository.findById(id).orElseThrow(() -> NotFoundException.of("chi nhánh", id));
        if (!s.getCode().equalsIgnoreCase(req.code().trim()) && repository.existsByCode(req.code().trim())) {
            throw new BadRequestException("Mã chi nhánh đã tồn tại: " + req.code());
        }
        apply(s, req);
        Store saved = repository.save(s);
        auditService.log("UPDATE_STORE", "STORE", saved.getId(),
                "Cập nhật chi nhánh " + saved.getCode() + " — " + saved.getName()
                        + " (trạng thái " + saved.getStatus() + ")");
        return StoreResponse.from(saved);
    }

    private void apply(Store s, StoreRequest req) {
        s.setCode(req.code().trim());
        s.setName(req.name().trim());
        s.setAddress(req.address());
        s.setPhone(req.phone());
        s.setStatus(req.status() != null ? req.status() : CommonStatus.ACTIVE);
    }
}
