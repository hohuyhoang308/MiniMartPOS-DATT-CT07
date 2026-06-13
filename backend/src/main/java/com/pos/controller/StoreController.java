package com.pos.controller;

import com.pos.common.ApiResponse;
import com.pos.dto.store.StoreRequest;
import com.pos.dto.store.StoreResponse;
import com.pos.service.StoreService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Quản lý CỬA HÀNG trong chuỗi (đa cửa hàng) — chỉ quản trị viên toàn chuỗi (ADMIN). */
@RestController
@RequestMapping("/api/stores")
@PreAuthorize("hasRole('ADMIN')")
public class StoreController {

    private final StoreService service;

    public StoreController(StoreService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<StoreResponse>> list() {
        return ApiResponse.ok(service.findAll());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<StoreResponse> create(@Valid @RequestBody StoreRequest req) {
        return ApiResponse.ok("Tạo chi nhánh thành công", service.create(req));
    }

    @PutMapping("/{id}")
    public ApiResponse<StoreResponse> update(@PathVariable Long id, @Valid @RequestBody StoreRequest req) {
        return ApiResponse.ok("Cập nhật chi nhánh thành công", service.update(id, req));
    }
}
