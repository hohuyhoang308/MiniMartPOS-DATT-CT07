package com.pos.dto.auth;

/** Phản hồi đăng nhập: JWT + thông tin hiển thị để FE điều hướng theo vai trò. */
public record LoginResponse(
        String token,
        String tokenType,
        long expiresInMs,
        Long userId,
        String username,
        String fullName,
        String role
) {}
