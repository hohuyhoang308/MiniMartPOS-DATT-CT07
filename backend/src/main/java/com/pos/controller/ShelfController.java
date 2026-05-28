package com.pos.controller;

import com.pos.common.ApiResponse;
import com.pos.dto.inventory.ShelfReturnRequest;
import com.pos.dto.inventory.ShelfTransferRequest;
import com.pos.dto.shelf.ShelfItemResponse;
import com.pos.dto.shelf.ShelfRequest;
import com.pos.dto.shelf.ShelfResponse;
import com.pos.service.ShelfService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Quản lý KỆ vật lý (FR8 mở rộng). Xem kệ + lên kệ: mọi vai trò (lên kệ là việc thu ngân);
 * tạo/sửa/xoá kệ: chỉ quản lý/chủ.
 */
@RestController
@RequestMapping("/api/shelves")
public class ShelfController {

    private final ShelfService service;

    public ShelfController(ShelfService service) {
        this.service = service;
    }

    /** Danh sách kệ + thống kê. Mọi vai trò (để lên kệ & xem). */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','CASHIER')")
    public ApiResponse<List<ShelfResponse>> list() {
        return ApiResponse.ok(service.listShelves());
    }

    /** Nội dung một kệ: lô nào, sản phẩm gì, HSD, số lượng. */
    @GetMapping("/{id}/inventory")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','CASHIER')")
    public ApiResponse<List<ShelfItemResponse>> inventory(@PathVariable Long id) {
        return ApiResponse.ok(service.shelfInventory(id));
    }

    /** Đưa hàng từ kho lên một KỆ (FIFO/HSD) — LÊN KỆ là việc của thu ngân. */
    @PostMapping("/transfer")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','CASHIER')")
    public ApiResponse<Integer> transfer(@Valid @RequestBody ShelfTransferRequest req) {
        int moved = service.replenishShelf(req.productId(), req.shelfId(), req.quantity());
        return ApiResponse.ok("Đã đưa " + moved + " sản phẩm lên kệ", moved);
    }

    /** Lấy hàng của một LÔ từ kệ về lại kho (đặt xuống) — đối ứng với lên kệ. */
    @PostMapping("/return")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','CASHIER')")
    public ApiResponse<Long> returnToWarehouse(@Valid @RequestBody ShelfReturnRequest req) {
        long qty = service.returnToWarehouse(req.batchId(), req.quantity());
        return ApiResponse.ok("Đã lấy " + qty + " sản phẩm từ kệ về kho", qty);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ShelfResponse> create(@Valid @RequestBody ShelfRequest req) {
        return ApiResponse.ok("Tạo kệ thành công", service.create(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<ShelfResponse> update(@PathVariable Long id, @Valid @RequestBody ShelfRequest req) {
        return ApiResponse.ok("Cập nhật kệ thành công", service.update(id, req));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ApiResponse.message("Đã xoá kệ");
    }
}
