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

    /** Chi nhánh đang làm việc (đa chuỗi); null = toàn chuỗi. Xem {@link StoreContext}. */
    public static Long currentStoreId() {
        return StoreContext.currentStoreId();
    }

    /** Có phải quản trị viên TOÀN CHUỖI không (vai trò ADMIN, không gắn cửa hàng). */
    public static boolean isAdmin() {
        return "ADMIN".equals(currentUser().getRole());
    }
}
