package com.pos.controller;

import com.pos.common.ApiResponse;
import com.pos.dto.returns.CreateReturnRequest;
import com.pos.dto.returns.ReturnResponse;
import com.pos.dto.returns.ReturnableLineResponse;
import com.pos.service.ReturnService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Trả hàng / hoàn tiền (FR5 mở rộng). Tạo phiếu trả: Manager/Admin. Xem: mọi vai trò có ca. */
@RestController
@RequestMapping("/api/returns")
public class ReturnController {

    private final ReturnService service;

    public ReturnController(ReturnService service) {
        this.service = service;
    }

    /** Các dòng còn được trả của một hóa đơn (cho màn chọn trả). */
    @GetMapping("/invoice/{invoiceId}/returnable")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','CASHIER')")
    public ApiResponse<List<ReturnableLineResponse>> returnable(@PathVariable Long invoiceId) {
        return ApiResponse.ok(service.returnableLines(invoiceId));
    }

    /** Các phiếu trả của một hóa đơn. */
    @GetMapping("/invoice/{invoiceId}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','CASHIER')")
    public ApiResponse<List<ReturnResponse>> byInvoice(@PathVariable Long invoiceId) {
        return ApiResponse.ok(service.byInvoice(invoiceId));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ReturnResponse> create(@Valid @RequestBody CreateReturnRequest req) {
        ReturnResponse r = service.createReturn(req);
        return ApiResponse.ok("Đã tạo phiếu trả hàng, hoàn " + r.refundAmount() + "đ — tồn kho cập nhật tự động", r);
    }
}
