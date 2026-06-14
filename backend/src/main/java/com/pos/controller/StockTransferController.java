package com.pos.controller;

import com.pos.common.ApiResponse;
import com.pos.dto.transfer.CreateTransferRequest;
import com.pos.dto.transfer.StockTransferResponse;
import com.pos.service.StockTransferService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * ĐIỀU CHUYỂN HÀNG NỘI BỘ (Obj 1.1). Phạm vi chi nhánh do {@code StoreContext} chốt:
 * ship chỉ chi nhánh nguồn, receive chỉ chi nhánh đích. ADMIN/MANAGER thao tác.
 */
@RestController
@RequestMapping("/api/stock-transfers")
@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
public class StockTransferController {

    private final StockTransferService service;

    public StockTransferController(StockTransferService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<StockTransferResponse>> list() {
        return ApiResponse.ok(service.list());
    }

    @GetMapping("/{id}")
    public ApiResponse<StockTransferResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(service.get(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<StockTransferResponse> create(@Valid @RequestBody CreateTransferRequest req) {
        return ApiResponse.ok("Đã lập phiếu điều chuyển", service.create(req));
    }

    @PostMapping("/{id}/ship")
    public ApiResponse<StockTransferResponse> ship(@PathVariable Long id) {
        return ApiResponse.ok("Đã xuất chuyển hàng", service.ship(id));
    }

    @PostMapping("/{id}/receive")
    public ApiResponse<StockTransferResponse> receive(@PathVariable Long id) {
        return ApiResponse.ok("Đã nhận hàng điều chuyển", service.receive(id));
    }

    @PostMapping("/{id}/cancel")
    public ApiResponse<StockTransferResponse> cancel(@PathVariable Long id,
                                                     @RequestBody(required = false) Map<String, String> body) {
        String reason = body != null ? body.getOrDefault("reason", "") : "";
        return ApiResponse.ok("Đã hủy phiếu điều chuyển", service.cancel(id, reason));
    }
}
