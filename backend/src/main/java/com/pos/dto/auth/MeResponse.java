package com.pos.dto.auth;

/** Thông tin tài khoản hiện tại (GET /api/auth/me). */
public record MeResponse(
        Long userId,
        String username,
        String fullName,
        String role,
        Long storeId,
        String storeName
) {}
