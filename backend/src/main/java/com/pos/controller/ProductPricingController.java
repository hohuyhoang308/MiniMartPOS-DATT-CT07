package com.pos.controller;

import com.pos.common.ApiResponse;
import com.pos.dto.pricing.StorePriceResponse;
import com.pos.service.ProductPricingService;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * GIÁ BÁN THEO CHI NHÁNH (Obj 1.3). Chi nhánh chốt qua {@code StoreContext}: MANAGER chỉ đụng cửa hàng mình;
 * ADMIN phải chọn chi nhánh (X-Store-Id) trước khi đặt/giật giá.
 */
@RestController
@RequestMapping("/api/store-prices")
@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
public class ProductPricingController {

    private final ProductPricingService service;

    public ProductPricingController(ProductPricingService service) {
        this.service = service;
    }

    /** Danh sách giá riêng đang áp dụng tại chi nhánh đang xem. */
    @GetMapping
    public ApiResponse<List<StorePriceResponse>> list() {
        return ApiResponse.ok(service.listForCurrentStore());
    }

    public record SetStorePriceRequest(
            @NotNull Long productId,
            @NotNull @PositiveOrZero BigDecimal salePrice) {}

    @PutMapping
    public ApiResponse<StorePriceResponse> set(@org.springframework.web.bind.annotation.RequestBody
                                               @jakarta.validation.Valid SetStorePriceRequest req) {
        return ApiResponse.ok("Đã đặt giá riêng cho chi nhánh",
                StorePriceResponse.from(service.setStorePrice(req.productId(), req.salePrice())));
    }

    /** Bỏ giá riêng → quay về giá chuẩn của chuỗi. */
    @DeleteMapping("/{productId}")
    public ApiResponse<Void> clear(@PathVariable Long productId) {
        service.clearStorePrice(productId);
        return ApiResponse.ok("Đã bỏ giá riêng", null);
    }
}
