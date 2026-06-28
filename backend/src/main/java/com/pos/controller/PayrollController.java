package com.pos.controller;

import com.pos.common.ApiResponse;
import com.pos.dto.payroll.*;
import com.pos.service.PayrollExportService;
import com.pos.service.PayrollService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * LƯƠNG & BẢNG CÔNG (module Lương). Quản lý (ADMIN/MANAGER) cấu hình lương, tính & chốt kỳ lương;
 * mọi nhân viên xem được phiếu lương của chính mình. Phạm vi chi nhánh do backend chốt (đa chuỗi).
 */
@RestController
@RequestMapping("/api/payroll")
public class PayrollController {

    private final PayrollService service;
    private final PayrollExportService exportService;

    public PayrollController(PayrollService service, PayrollExportService exportService) {
        this.service = service;
        this.exportService = exportService;
    }

    // --- Cấu hình lương / nhân viên ---
    @GetMapping("/pay-profiles")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<List<PayProfileResponse>> payProfiles() {
        return ApiResponse.ok(service.listPayProfiles());
    }

    @PutMapping("/pay-profiles/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<PayProfileResponse> setPayProfile(@PathVariable Long userId,
                                                         @Valid @RequestBody PayProfileRequest req) {
        return ApiResponse.ok("Đã cập nhật cấu hình lương", service.setPayProfile(userId, req));
    }

    // --- Kỳ lương ---
    @GetMapping("/periods")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<List<PayrollPeriodResponse>> periods() {
        return ApiResponse.ok(service.listPeriods());
    }

    @PostMapping("/periods/compute")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<PayrollPeriodResponse> compute(@Valid @RequestBody ComputePeriodRequest req) {
        return ApiResponse.ok("Đã tính lương kỳ " + req.month(), service.compute(req.month()));
    }

    @GetMapping("/periods/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<PayrollPeriodResponse> period(@PathVariable Long id) {
        return ApiResponse.ok(service.getPeriod(id));
    }

    /** Trình duyệt (bước 1): ADMIN/MANAGER lập gửi duyệt. */
    @PostMapping("/periods/{id}/submit")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<PayrollPeriodResponse> submit(@PathVariable Long id) {
        return ApiResponse.ok("Đã trình duyệt bảng lương", service.submit(id));
    }

    /** Duyệt (bước 2): CHỈ ADMIN — tách trách nhiệm lập/duyệt. */
    @PostMapping("/periods/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<PayrollPeriodResponse> approve(@PathVariable Long id) {
        return ApiResponse.ok("Đã duyệt bảng lương", service.approve(id));
    }

    /** Trả lại bản nháp (CHỈ ADMIN). */
    @PostMapping("/periods/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<PayrollPeriodResponse> reject(@PathVariable Long id,
                                                     @RequestBody(required = false) java.util.Map<String, String> body) {
        return ApiResponse.ok("Đã trả lại bản nháp", service.reject(id, body != null ? body.get("reason") : null));
    }

    @PostMapping("/periods/{id}/pay")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<PayrollPeriodResponse> pay(@PathVariable Long id) {
        return ApiResponse.ok("Đã ghi nhận chi lương", service.markPaid(id));
    }

    /** Xuất bảng lương cả kỳ ra Excel (.xlsx). */
    @GetMapping("/periods/{id}/export")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<byte[]> exportPeriod(@PathVariable Long id) {
        byte[] data = exportService.exportPeriodExcel(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=bang-luong-" + id + ".xlsx")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
    }

    /** In phiếu lương cá nhân ra PDF (STAFF chỉ in phiếu đã chốt của mình — service tự chốt phạm vi). */
    @GetMapping("/payslips/{id}/pdf")
    public ResponseEntity<byte[]> exportPayslip(@PathVariable Long id) {
        byte[] data = exportService.exportPayslipPdf(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=phieu-luong-" + id + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(data);
    }

    // --- Thưởng / phạt ---
    @PostMapping("/payslips/{id}/adjustments")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<PayslipResponse> addAdjustment(@PathVariable Long id,
                                                      @Valid @RequestBody PayslipAdjustmentRequest req) {
        return ApiResponse.ok("Đã thêm điều chỉnh", service.addAdjustment(id, req));
    }

    @DeleteMapping("/adjustments/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<PayslipResponse> removeAdjustment(@PathVariable Long id) {
        return ApiResponse.ok("Đã xóa điều chỉnh", service.removeAdjustment(id));
    }

    // --- Chấm công thủ công & nghỉ phép ---
    @GetMapping("/attendance")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<List<AttendanceEntryResponse>> attendance(@RequestParam String month) {
        return ApiResponse.ok(service.listAttendance(month));
    }

    @PostMapping("/attendance")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<AttendanceEntryResponse> addAttendance(@Valid @RequestBody AttendanceEntryRequest req) {
        return ApiResponse.ok("Đã chấm công", service.addAttendance(req));
    }

    @DeleteMapping("/attendance/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<Void> removeAttendance(@PathVariable Long id) {
        service.removeAttendance(id);
        return ApiResponse.message("Đã xóa bản ghi chấm công");
    }

    // --- Phiếu lương của tôi (mọi vai trò) ---
    @GetMapping("/my-payslips")
    public ApiResponse<List<PayslipResponse>> myPayslips() {
        return ApiResponse.ok(service.myPayslips());
    }
}
