package com.pos.dto.user;

import com.pos.entity.enums.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @NotBlank(message = "Tên đăng nhập không được để trống") @Size(min = 3, max = 50) String username,
        @NotBlank(message = "Mật khẩu không được để trống") @Size(min = 6, max = 100) String password,
        @NotBlank(message = "Họ tên không được để trống") @Size(max = 100) String fullName,
        @NotNull(message = "Phải chọn vai trò") Role role,
        /** Chi nhánh trực thuộc — bắt buộc với ADMIN/MANAGER/CASHIER; bỏ trống nếu là CHAIN_ADMIN. */
        Long storeId
) {}
