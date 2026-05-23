package com.pos.controller;

import com.pos.common.ApiResponse;
import com.pos.dto.supplier.SupplierRequest;
import com.pos.dto.supplier.SupplierResponse;
import com.pos.service.SupplierService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** CRUD nhà cung cấp (FR3.1). Chỉ Admin/Manager. */
@RestController
@RequestMapping("/api/suppliers")
@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
public class SupplierController {

    private final SupplierService service;

    public SupplierController(SupplierService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<SupplierResponse>> list() {
        return ApiResponse.ok(service.findAll());
    }

    @GetMapping("/{id}")
    public ApiResponse<SupplierResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(service.findById(id));
    }

    @PostMapping
    @org.springframework.web.bind.annotation.ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    public ApiResponse<SupplierResponse> create(@Valid @RequestBody SupplierRequest req) {
        return ApiResponse.ok("Tạo nhà cung cấp thành công", service.create(req));
    }

    @PutMapping("/{id}")
    public ApiResponse<SupplierResponse> update(@PathVariable Long id, @Valid @RequestBody SupplierRequest req) {
        return ApiResponse.ok("Cập nhật thành công", service.update(id, req));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ApiResponse.message("Xóa nhà cung cấp thành công");
    }
}
