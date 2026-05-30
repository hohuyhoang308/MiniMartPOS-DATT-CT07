package com.pos.controller;

import com.pos.common.ApiResponse;
import com.pos.entity.AuditLog;
import com.pos.service.AuditService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Xem nhật ký kiểm toán (chỉ ADMIN). */
@RestController
@RequestMapping("/api/audit")
@PreAuthorize("hasRole('ADMIN')")
public class AuditController {

    private final AuditService service;

    public AuditController(AuditService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<AuditLog>> recent() {
        return ApiResponse.ok(service.recent());
    }
}
