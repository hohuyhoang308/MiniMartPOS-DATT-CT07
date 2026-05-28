package com.pos.controller;

import com.pos.common.ApiResponse;
import com.pos.dto.inventory.BatchDetailResponse;
import com.pos.dto.inventory.ExpiringBatchResponse;
import com.pos.dto.inventory.ReorderSuggestionResponse;
import com.pos.dto.inventory.ShelfTransferRequest;
import com.pos.dto.inventory.StockResponse;
import com.pos.service.InventoryService;
import com.pos.service.ShelfService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Kho & cảnh báo (FR8). Chỉ Admin/Manager. */
@RestController
@RequestMapping("/api/inventory")
@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
public class InventoryController {

    private final InventoryService service;
    private final ShelfService shelfService;

    public InventoryController(InventoryService service, ShelfService shelfService) {
        this.service = service;
        this.shelfService = shelfService;
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

    /** Chi tiết các lô của 1 sản phẩm (HSD + tồn kho/kệ theo lô). */
    @GetMapping("/batches/{productId}")
    public ApiResponse<List<BatchDetailResponse>> batches(@PathVariable Long productId) {
        return ApiResponse.ok(service.productBatches(productId));
    }

    /** Đưa hàng từ KHO lên KỆ (chọn lô theo FIFO/HSD). */
    @PostMapping("/shelf-transfer")
    public ApiResponse<Integer> shelfTransfer(@Valid @RequestBody ShelfTransferRequest req) {
        int moved = shelfService.replenishShelf(req.productId(), req.quantity());
        return ApiResponse.ok("Đã đưa " + moved + " sản phẩm lên kệ", moved);
    }
}
