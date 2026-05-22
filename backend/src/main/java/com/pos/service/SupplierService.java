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

    public SupplierService(SupplierRepository repository) {
        this.repository = repository;
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
        return SupplierResponse.from(repository.save(s));
    }

    @Transactional
    public SupplierResponse update(Long id, SupplierRequest req) {
        Supplier s = getOrThrow(id);
        apply(s, req);
        if (req.status() != null) s.setStatus(req.status());
        return SupplierResponse.from(repository.save(s));
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(getOrThrow(id));
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
