package com.pos.controller;

import com.pos.common.ApiResponse;
import com.pos.dto.promotion.PromotionRequest;
import com.pos.dto.promotion.PromotionResponse;
import com.pos.dto.promotion.ValidatePromotionRequest;
import com.pos.dto.promotion.ValidatePromotionResponse;
import com.pos.service.PromotionService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Khuyến mãi (FR7). CRUD: Manager/Admin; validate: Cashier áp mã tại POS. */
@RestController
@RequestMapping("/api/promotions")
public class PromotionController {

    private final PromotionService service;

    public PromotionController(PromotionService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<PromotionResponse>> list() {
        return ApiResponse.ok(service.findAll());
    }

    @PostMapping("/validate")
    public ApiResponse<ValidatePromotionResponse> validate(@Valid @RequestBody ValidatePromotionRequest req) {
        return ApiResponse.ok(service.validate(req.code(), req.subtotal()));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @org.springframework.web.bind.annotation.ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    public ApiResponse<PromotionResponse> create(@Valid @RequestBody PromotionRequest req) {
        return ApiResponse.ok("Tạo khuyến mãi thành công", service.create(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<PromotionResponse> update(@PathVariable Long id, @Valid @RequestBody PromotionRequest req) {
        return ApiResponse.ok("Cập nhật thành công", service.update(id, req));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ApiResponse.message("Xóa khuyến mãi thành công");
    }
}
