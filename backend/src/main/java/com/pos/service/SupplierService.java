package com.pos.service;

import com.pos.dto.supplier.SupplierRequest;
import com.pos.dto.supplier.SupplierResponse;
import com.pos.entity.Supplier;
import com.pos.entity.enums.CommonStatus;
import com.pos.exception.NotFoundException;
import com.pos.repository.SupplierRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Quản lý nhà cung cấp (FR3.1 - UC06). */
@Service
@Transactional(readOnly = true)
public class SupplierService {

    private final SupplierRepository repository;
    private final AuditService auditService;

    public SupplierService(SupplierRepository repository, AuditService auditService) {
        this.repository = repository;
        this.auditService = auditService;
    }

    public List<SupplierResponse> findAll() {
        return repository.findAll().stream().map(SupplierResponse::from).toList();
    }

    public SupplierResponse findById(Long id) {
        return SupplierResponse.from(getOrThrow(id));
    }

    @Transactional
    public SupplierResponse create(SupplierRequest req) {
        Supplier s = new Supplier();
        apply(s, req);
        s.setStatus(req.status() != null ? req.status() : CommonStatus.ACTIVE);
        Supplier saved = repository.save(s);
        auditService.log("CREATE_SUPPLIER", "SUPPLIER", saved.getId(),
                "Thêm nhà cung cấp \"" + saved.getName() + "\" (SĐT " + saved.getPhone() + ")");
        return SupplierResponse.from(saved);
    }

    @Transactional
    public SupplierResponse update(Long id, SupplierRequest req) {
        Supplier s = getOrThrow(id);
        apply(s, req);
        if (req.status() != null) s.setStatus(req.status());
        Supplier saved = repository.save(s);
        auditService.log("UPDATE_SUPPLIER", "SUPPLIER", saved.getId(),
                "Sửa nhà cung cấp \"" + saved.getName() + "\" (SĐT " + saved.getPhone() + ")");
        return SupplierResponse.from(saved);
    }

    @Transactional
    public void delete(Long id) {
        Supplier s = getOrThrow(id);
        repository.delete(s);
        auditService.log("DELETE_SUPPLIER", "SUPPLIER", s.getId(),
                "Xóa nhà cung cấp \"" + s.getName() + "\" (SĐT " + s.getPhone() + ")");
    }

    private void apply(Supplier s, SupplierRequest req) {
        s.setName(req.name());
        s.setPhone(req.phone());
        s.setEmail(req.email());
        s.setAddress(req.address());
    }

    private Supplier getOrThrow(Long id) {
        return repository.findById(id).orElseThrow(() -> NotFoundException.of("nhà cung cấp", id));
    }
}
