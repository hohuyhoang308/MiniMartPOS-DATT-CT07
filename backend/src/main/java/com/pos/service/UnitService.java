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

    public UnitService(UnitRepository repository) {
        this.repository = repository;
    }

    public List<UnitResponse> findAll() {
        return repository.findAll().stream().map(UnitResponse::from).toList();
    }

    @Transactional
    public UnitResponse create(UnitRequest req) {
        Unit u = new Unit();
        u.setName(req.name());
        return UnitResponse.from(repository.save(u));
    }

    @Transactional
    public UnitResponse update(Long id, UnitRequest req) {
        Unit u = getOrThrow(id);
        u.setName(req.name());
        return UnitResponse.from(repository.save(u));
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(getOrThrow(id));
    }

    private Unit getOrThrow(Long id) {
        return repository.findById(id).orElseThrow(() -> NotFoundException.of("đơn vị tính", id));
    }
}
