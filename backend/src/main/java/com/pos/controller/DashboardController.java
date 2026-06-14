package com.pos.controller;

import com.pos.common.ApiResponse;
import com.pos.dto.dashboard.DashboardResponse;
import com.pos.dto.dashboard.StoreKpiResponse;
import com.pos.service.DashboardService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Dashboard (FR9.1). Chỉ Admin/Manager. */
@RestController
@RequestMapping("/api/dashboard")
@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
public class DashboardController {

    private final DashboardService service;

    public DashboardController(DashboardService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<DashboardResponse> dashboard() {
        return ApiResponse.ok(service.getDashboard());
    }

    /** So sánh KPI giữa các chi nhánh (toàn chuỗi) — chỉ quản trị viên toàn chuỗi (ADMIN). */
    @GetMapping("/stores")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<StoreKpiResponse>> storeComparison() {
        return ApiResponse.ok(service.getStoreComparison());
    }
}
