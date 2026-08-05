package com.pos.security;

import com.pos.exception.BadRequestException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** Tiện ích lấy người dùng đang đăng nhập từ SecurityContext. */
public final class SecurityUtils {

    private SecurityUtils() {}

    public static CustomUserDetails currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof CustomUserDetails details)) {
            throw new BadRequestException("Không xác định được người dùng đang đăng nhập");
        }
        return details;
    }

    public static Long currentUserId() {
        return currentUser().getId();
    }

    /** Có phải quản trị viên TOÀN CHUỖI không (vai trò ADMIN, không gắn cửa hàng). */
    public static boolean isAdmin() {
        return "ADMIN".equals(currentUser().getRole());
    }

    /** Có phải cấp quản lý trở lên không (ADMIN hoặc MANAGER). */
    public static boolean isManagerOrAbove() {
        String role = currentUser().getRole();
        return "ADMIN".equals(role) || "MANAGER".equals(role);
    }

    /** Có phải nhân viên bán hàng (STAFF) không. */
    public static boolean isStaff() {
        return "STAFF".equals(currentUser().getRole());
    }
}
