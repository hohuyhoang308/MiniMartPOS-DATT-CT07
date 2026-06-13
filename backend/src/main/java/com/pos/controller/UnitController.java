package com.pos.controller;

import com.pos.common.ApiResponse;
import com.pos.dto.unit.UnitRequest;
import com.pos.dto.unit.UnitResponse;
import com.pos.service.UnitService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** CRUD đơn vị tính (FR2.2). */
@RestController
@RequestMapping("/api/units")
public class UnitController {

    private final UnitService service;

    public UnitController(UnitService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<UnitResponse>> list() {
        return ApiResponse.ok(service.findAll());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @org.springframework.web.bind.annotation.ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    public ApiResponse<UnitResponse> create(@Valid @RequestBody UnitRequest req) {
        return ApiResponse.ok("Tạo đơn vị tính thành công", service.create(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<UnitResponse> update(@PathVariable Long id, @Valid @RequestBody UnitRequest req) {
        return ApiResponse.ok("Cập nhật thành công", service.update(id, req));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ApiResponse.message("Xóa đơn vị tính thành công");
    }
}
