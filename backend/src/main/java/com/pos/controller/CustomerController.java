package com.pos.controller;

import com.pos.common.ApiResponse;
import com.pos.dto.customer.CustomerHistoryResponse;
import com.pos.dto.customer.CustomerRequest;
import com.pos.dto.customer.CustomerResponse;
import com.pos.service.CustomerService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Khách hàng thân thiết (FR6). Cashier: xem/thêm; Manager/Admin: toàn quyền. */
@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerService service;

    public CustomerController(CustomerService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<CustomerResponse>> list(@RequestParam(required = false) String keyword) {
        return ApiResponse.ok(service.search(keyword));
    }

    @GetMapping("/by-phone/{phone}")
    public ApiResponse<CustomerResponse> byPhone(@PathVariable String phone) {
        return ApiResponse.ok(service.findByPhone(phone));
    }

    /** Lịch sử mua + điểm của khách. Cashier có trang Khách hàng (đã được phép) nên giữ truy cập. */
    @GetMapping("/{id}/history")
    public ApiResponse<CustomerHistoryResponse> history(@PathVariable Long id) {
        return ApiResponse.ok(service.history(id));
    }

    /** Cashier được phép thêm khách mới ngay tại POS. */
    @PostMapping
    @org.springframework.web.bind.annotation.ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    public ApiResponse<CustomerResponse> create(@Valid @RequestBody CustomerRequest req) {
        return ApiResponse.ok("Thêm khách hàng thành công", service.create(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<CustomerResponse> update(@PathVariable Long id, @Valid @RequestBody CustomerRequest req) {
        return ApiResponse.ok("Cập nhật thành công", service.update(id, req));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ApiResponse.message("Xóa khách hàng thành công");
    }
}
