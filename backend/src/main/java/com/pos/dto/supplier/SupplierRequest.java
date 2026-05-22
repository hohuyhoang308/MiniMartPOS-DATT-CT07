package com.pos.dto.supplier;

import com.pos.entity.enums.CommonStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SupplierRequest(
        @NotBlank(message = "Tên nhà cung cấp không được để trống")
        @Size(max = 150) String name,
        @Size(max = 20) String phone,
        @Email(message = "Email không hợp lệ") @Size(max = 100) String email,
        @Size(max = 255) String address,
        CommonStatus status
) {}
