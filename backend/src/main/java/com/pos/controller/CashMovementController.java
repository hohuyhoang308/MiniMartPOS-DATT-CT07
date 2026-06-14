package com.pos.controller;

import com.pos.common.ApiResponse;
import com.pos.dto.shift.CashMovementRequest;
import com.pos.dto.shift.CashMovementResponse;
import com.pos.service.CashMovementService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * THU/CHI quỹ tiền mặt trong ca (petty cash). Thu ngân (STAFF) cũng được dùng vì họ trực tiếp giữ két —
 * khoản ghi luôn vào CA ĐANG MỞ của chính họ; phạm vi chi nhánh do service + {@code StoreContext} chốt.
 *
 * <p>Defense-in-depth: dù service đã chốt phạm vi qua {@code assertSameStore}/ca-đang-mở, vẫn yêu cầu
 * xác thực ở tầng controller — chỉ vai trò vận hành cửa hàng (STAFF/MANAGER) và ADMIN được gọi.</p>
 */
@RestController
@RequestMapping("/api/cash-movements")
@PreAuthorize("hasAnyRole('STAFF','MANAGER','ADMIN')")
public class CashMovementController {

    private final CashMovementService service;

    public CashMovementController(CashMovementService service) {
        this.service = service;
    }

    /** Các khoản thu/chi của một ca (đối soát khi đóng ca). */
    @GetMapping("/shift/{shiftId}")
    public ApiResponse<List<CashMovementResponse>> byShift(@PathVariable Long shiftId) {
        return ApiResponse.ok(service.byShift(shiftId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CashMovementResponse> create(@Valid @RequestBody CashMovementRequest req) {
        return ApiResponse.ok("Đã ghi nhận thu/chi quỹ", service.create(req));
    }
}
