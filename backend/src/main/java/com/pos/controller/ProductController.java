package com.pos.controller;

import com.pos.common.ApiResponse;
import com.pos.dto.product.ProductRequest;
import com.pos.dto.product.ProductResponse;
import com.pos.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** CRUD sản phẩm + tra cứu mã vạch (FR2). GET: mọi vai trò; ghi: Admin/Manager. */
@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService service;

    public ProductController(ProductService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<ProductResponse>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId) {
        return ApiResponse.ok(service.search(keyword, categoryId));
    }

    @GetMapping("/{id}")
    public ApiResponse<ProductResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(service.findById(id));
    }

    /** Tra cứu nhanh theo mã vạch cho màn hình POS. */
    @GetMapping("/barcode/{code}")
    public ApiResponse<ProductResponse> getByBarcode(@PathVariable String code) {
        return ApiResponse.ok(service.findByBarcode(code));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @org.springframework.web.bind.annotation.ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    public ApiResponse<ProductResponse> create(@Valid @RequestBody ProductRequest req) {
        return ApiResponse.ok("Tạo sản phẩm thành công", service.create(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<ProductResponse> update(@PathVariable Long id, @Valid @RequestBody ProductRequest req) {
        return ApiResponse.ok("Cập nhật thành công", service.update(id, req));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ApiResponse.message("Xóa sản phẩm thành công");
    }
}
