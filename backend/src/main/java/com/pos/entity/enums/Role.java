package com.pos.entity.enums;

/**
 * Vai trò người dùng (FR1.2) — phân tầng TỪ CAO XUỐNG THẤP: ADMIN ⊃ MANAGER ⊃ STAFF.
 */
public enum Role {
    /**
     * Quản trị viên TOÀN CHUỖI (đa cửa hàng): KHÔNG thuộc chi nhánh nào ({@code store_id = null}).
     * Quản lý chi nhánh, toàn bộ tài khoản, danh mục chung, cấu hình từng chi nhánh; xem/điều hành
     * mọi cửa hàng; chuyển cửa hàng đang làm việc qua header {@code X-Store-Id}.
     */
    ADMIN,
    /** Quản lý MỘT cửa hàng (N quản lý / cửa hàng): điều hành vận hành của chính cửa hàng mình. */
    MANAGER,
    /** Nhân viên bán hàng tại quầy của MỘT cửa hàng (N nhân viên / cửa hàng): POS, ca, khách, kệ. */
    STAFF
}
