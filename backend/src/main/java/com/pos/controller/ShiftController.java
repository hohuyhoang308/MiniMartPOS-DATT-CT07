package com.pos.controller;

import com.pos.common.ApiResponse;
import com.pos.dto.shift.CloseShiftRequest;
import com.pos.dto.shift.OpenShiftRequest;
import com.pos.dto.shift.ShiftResponse;
import com.pos.service.ShiftService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Ca làm việc (FR4.1). Mọi vai trò bán hàng đều mở/đóng ca của mình. */
@RestController
@RequestMapping("/api/shifts")
public class ShiftController {

    private final ShiftService service;

    public ShiftController(ShiftService service) {
        this.service = service;
    }

    @PostMapping("/open")
    public ApiResponse<ShiftResponse> open(@Valid @RequestBody OpenShiftRequest req) {
        return ApiResponse.ok("Mở ca thành công", service.open(req));
    }

    @PostMapping("/{id}/close")
    public ApiResponse<ShiftResponse> close(@PathVariable Long id, @Valid @RequestBody CloseShiftRequest req) {
        return ApiResponse.ok("Đóng ca thành công", service.close(id, req.closingCash()));
    }

    /** Danh sách toàn bộ ca — màn Quản lý ca (chỉ quản lý/chủ cửa hàng). */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<List<ShiftResponse>> list() {
        return ApiResponse.ok(service.listAll());
    }

    /** Ca đang mở của tôi (data = null nếu chưa mở ca). */
    @GetMapping("/current")
    public ApiResponse<ShiftResponse> current() {
        return ApiResponse.ok(service.current());
    }

    /** Gợi ý tiền đầu ca = tiền cuối ca trước (két chuyển tiếp) để mở ca khỏi đếm lại. */
    @GetMapping("/suggested-opening")
    public ApiResponse<java.math.BigDecimal> suggestedOpening() {
        return ApiResponse.ok(service.suggestedOpeningCash());
    }

    @GetMapping("/{id}")
    public ApiResponse<ShiftResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(service.findById(id));
    }
}
