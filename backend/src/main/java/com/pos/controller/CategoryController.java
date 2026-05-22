package com.pos.controller;

import com.pos.common.ApiResponse;
import com.pos.dto.category.CategoryRequest;
import com.pos.dto.category.CategoryResponse;
import com.pos.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** CRUD danh mục (FR2.1). GET: mọi vai trò; ghi: Admin/Manager. */
@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService service;

    public CategoryController(CategoryService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<CategoryResponse>> list() {
        return ApiResponse.ok(service.findAll());
    }

    @GetMapping("/{id}")
    public ApiResponse<CategoryResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(service.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<CategoryResponse> create(@Valid @RequestBody CategoryRequest req) {
        return ApiResponse.ok("Tạo danh mục thành công", service.create(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<CategoryResponse> update(@PathVariable Long id, @Valid @RequestBody CategoryRequest req) {
        return ApiResponse.ok("Cập nhật thành công", service.update(id, req));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ApiResponse.message("Xóa danh mục thành công");
    }
}
