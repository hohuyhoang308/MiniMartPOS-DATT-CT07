package com.pos.dto.auth;

import jakarta.validation.constraints.NotBlank;

/** Yêu cầu đăng nhập (UC01). */
public record LoginRequest(
        @NotBlank(message = "Tên đăng nhập không được để trống") String username,
        @NotBlank(message = "Mật khẩu không được để trống") String password
) {}
