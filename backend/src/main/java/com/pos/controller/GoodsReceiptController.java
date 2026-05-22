package com.pos.controller;

import com.pos.common.ApiResponse;
import com.pos.dto.receipt.GoodsReceiptRequest;
import com.pos.dto.receipt.GoodsReceiptResponse;
import com.pos.service.GoodsReceiptService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Nhập kho (FR3 - UC07). Chỉ Admin/Manager. */
@RestController
@RequestMapping("/api/goods-receipts")
@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
public class GoodsReceiptController {

    private final GoodsReceiptService service;

    public GoodsReceiptController(GoodsReceiptService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<GoodsReceiptResponse>> list() {
        return ApiResponse.ok(service.findAll());
    }

    @GetMapping("/{id}")
    public ApiResponse<GoodsReceiptResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(service.findById(id));
    }

    @PostMapping
    public ApiResponse<GoodsReceiptResponse> create(@Valid @RequestBody GoodsReceiptRequest req) {
        return ApiResponse.ok("Lập phiếu nhập thành công, tồn kho đã tăng", service.create(req));
    }
}
