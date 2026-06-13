package com.pos.dto.user;

import com.pos.entity.enums.Role;
import com.pos.entity.enums.UserStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Cập nhật nhân viên: đổi họ tên, vai trò, khóa/mở khóa (không đổi username). */
public record UpdateUserRequest(
        @NotBlank(message = "Họ tên không được để trống") @Size(max = 100) String fullName,
        @NotNull(message = "Phải chọn vai trò") Role role,
        /** Chi nhánh trực thuộc — bắt buộc với ADMIN/MANAGER/CASHIER; bỏ trống nếu là CHAIN_ADMIN. */
        Long storeId,
        @NotNull(message = "Phải chọn trạng thái") UserStatus status
) {}
