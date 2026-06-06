package com.pos.service;

import com.pos.dto.unit.UnitRequest;
import com.pos.dto.unit.UnitResponse;
import com.pos.entity.Unit;
import com.pos.exception.NotFoundException;
import com.pos.repository.UnitRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Quản lý đơn vị tính (FR2.2 - UC04). */
@Service
@Transactional(readOnly = true)
public class UnitService {

    private final UnitRepository repository;
    private final AuditService auditService;

    public UnitService(UnitRepository repository, AuditService auditService) {
        this.repository = repository;
        this.auditService = auditService;
    }

    public List<UnitResponse> findAll() {
        return repository.findAll().stream().map(UnitResponse::from).toList();
    }

    @Transactional
    public UnitResponse create(UnitRequest req) {
        Unit u = new Unit();
        u.setName(req.name());
        Unit saved = repository.save(u);
        auditService.log("CREATE_UNIT", "UNIT", saved.getId(),
                "Thêm đơn vị tính \"" + saved.getName() + "\"");
        return UnitResponse.from(saved);
    }

    @Transactional
    public UnitResponse update(Long id, UnitRequest req) {
        Unit u = getOrThrow(id);
        u.setName(req.name());
        Unit saved = repository.save(u);
        auditService.log("UPDATE_UNIT", "UNIT", saved.getId(),
                "Sửa đơn vị tính \"" + saved.getName() + "\"");
        return UnitResponse.from(saved);
    }

    @Transactional
    public void delete(Long id) {
        Unit u = getOrThrow(id);
        String name = u.getName();
        repository.delete(u);
        auditService.log("DELETE_UNIT", "UNIT", id,
                "Xóa đơn vị tính \"" + name + "\"");
    }

    private Unit getOrThrow(Long id) {
        return repository.findById(id).orElseThrow(() -> NotFoundException.of("đơn vị tính", id));
    }
}
