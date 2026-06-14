package com.pos.controller;

import com.pos.common.ApiResponse;
import com.pos.dto.inventory.StockAdjustmentRequest;
import com.pos.dto.inventory.StockAdjustmentResponse;
import com.pos.service.StockAdjustmentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Xuất hủy / điều chỉnh giảm tồn (FR8 — kiểm kê & hao hụt). Thao tác GIẢM tồn nhạy cảm → chỉ ADMIN/MANAGER
 * (KHÔNG cho STAFF). Phạm vi chi nhánh do {@link StockAdjustmentService} + {@code StoreContext} chốt.
 */
@RestController
@RequestMapping("/api/stock-adjustments")
@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
public class StockAdjustmentController {

    private final StockAdjustmentService service;

    public StockAdjustmentController(StockAdjustmentService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<StockAdjustmentResponse>> list() {
        return ApiResponse.ok(service.recent());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<StockAdjustmentResponse> create(@Valid @RequestBody StockAdjustmentRequest req) {
        return ApiResponse.ok("Đã xuất hủy khỏi tồn kho", service.create(req));
    }
}
