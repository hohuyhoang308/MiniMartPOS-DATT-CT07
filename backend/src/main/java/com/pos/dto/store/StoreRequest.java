package com.pos.dto.store;

import com.pos.entity.enums.CommonStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Tạo/sửa chi nhánh (đa chuỗi). */
public record StoreRequest(
        @NotBlank(message = "Mã chi nhánh không được để trống") @Size(max = 30) String code,
        @NotBlank(message = "Tên chi nhánh không được để trống") @Size(max = 150) String name,
        @Size(max = 255) String address,
        @Size(max = 20) String phone,
        CommonStatus status
) {}
