package com.pos.dto.user;

import com.pos.entity.enums.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @NotBlank(message = "Tên đăng nhập không được để trống") @Size(min = 3, max = 50) String username,
        @NotBlank(message = "Mật khẩu không được để trống")
        @Size(min = 8, max = 100, message = "Mật khẩu phải từ 8 ký tự trở lên") String password,
        @NotBlank(message = "Họ tên không được để trống") @Size(max = 100) String fullName,
        @NotNull(message = "Phải chọn vai trò") Role role,
        /** Chi nhánh trực thuộc — bắt buộc với MANAGER/STAFF; bỏ trống nếu là ADMIN (toàn chuỗi). */
        Long storeId
) {}
