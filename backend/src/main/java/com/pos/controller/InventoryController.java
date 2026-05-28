package com.pos.controller;

import com.pos.common.ApiResponse;
import com.pos.dto.inventory.BatchDetailResponse;
import com.pos.dto.inventory.ExpiringBatchResponse;
import com.pos.dto.inventory.ReorderSuggestionResponse;
import com.pos.dto.inventory.StockResponse;
import com.pos.service.InventoryService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Kho & cảnh báo (FR8). Chỉ Admin/Manager. */
@RestController
@RequestMapping("/api/inventory")
@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
public class InventoryController {

    private final InventoryService service;

    public InventoryController(InventoryService service) {
        this.service = service;
    }

    @GetMapping("/stock")
    public ApiResponse<List<StockResponse>> stock() {
        return ApiResponse.ok(service.currentStock());
    }

    @GetMapping("/low-stock")
    public ApiResponse<List<StockResponse>> lowStock() {
        return ApiResponse.ok(service.lowStock());
    }

    @GetMapping("/expiring")
    public ApiResponse<List<ExpiringBatchResponse>> expiring() {
        return ApiResponse.ok(service.expiringBatches());
    }

    /** Đề xuất nhập hàng dựa trên tồn kho + tốc độ bán (FR8.3). */
    @GetMapping("/suggestions")
    public ApiResponse<List<ReorderSuggestionResponse>> suggestions() {
        return ApiResponse.ok(service.reorderSuggestions());
    }

    /** Chi tiết các lô của 1 sản phẩm (HSD + tồn kho/kệ theo lô). Cả thu ngân (để lên kệ). */
    @GetMapping("/batches/{productId}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','CASHIER')")
    public ApiResponse<List<BatchDetailResponse>> batches(@PathVariable Long productId) {
        return ApiResponse.ok(service.productBatches(productId));
    }

}
