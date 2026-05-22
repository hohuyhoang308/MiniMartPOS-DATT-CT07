package com.pos.controller;

import com.pos.common.ApiResponse;
import com.pos.dto.shift.CloseShiftRequest;
import com.pos.dto.shift.OpenShiftRequest;
import com.pos.dto.shift.ShiftResponse;
import com.pos.service.ShiftService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

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

    /** Ca đang mở của tôi (data = null nếu chưa mở ca). */
    @GetMapping("/current")
    public ApiResponse<ShiftResponse> current() {
        return ApiResponse.ok(service.current());
    }

    @GetMapping("/{id}")
    public ApiResponse<ShiftResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(service.findById(id));
    }
}
