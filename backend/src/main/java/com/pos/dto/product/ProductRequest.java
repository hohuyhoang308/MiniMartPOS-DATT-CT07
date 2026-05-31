package com.pos.dto.product;

import com.pos.entity.enums.CommonStatus;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record ProductRequest(
        @NotBlank(message = "Mã vạch không được để trống") @Size(max = 50) String barcode,
        @NotBlank(message = "Tên sản phẩm không được để trống") @Size(max = 150) String name,
        @NotNull(message = "Phải chọn danh mục") Long categoryId,
        @NotNull(message = "Phải chọn đơn vị tính") Long unitId,
        @NotNull @DecimalMin(value = "0", message = "Giá vốn phải ≥ 0") BigDecimal costPrice,
        @NotNull @DecimalMin(value = "0", message = "Giá bán phải ≥ 0") BigDecimal salePrice,
        @DecimalMin(value = "0", message = "Thuế suất ≥ 0") @DecimalMax(value = "100", message = "Thuế suất ≤ 100") BigDecimal taxRate,
        @Size(max = 255) String imageUrl,
        @NotNull @Min(value = 0, message = "Mức tồn tối thiểu phải ≥ 0") Integer minStock,
        CommonStatus status
) {}
