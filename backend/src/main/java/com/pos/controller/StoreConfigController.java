package com.pos.controller;

import com.pos.common.ApiResponse;
import com.pos.dto.config.StoreConfigRequest;
import com.pos.dto.config.StoreConfigResponse;
import com.pos.service.StoreConfigService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Cấu hình cửa hàng (UC20/UC24) — chỉ Admin. */
@RestController
@RequestMapping("/api/store-config")
@PreAuthorize("hasRole('ADMIN')")
public class StoreConfigController {

    private final StoreConfigService service;

    public StoreConfigController(StoreConfigService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<StoreConfigResponse> get() {
        return ApiResponse.ok(service.get());
    }

    @PutMapping
    public ApiResponse<StoreConfigResponse> update(@Valid @RequestBody StoreConfigRequest req) {
        return ApiResponse.ok("Cập nhật cấu hình thành công", service.update(req));
    }
}
