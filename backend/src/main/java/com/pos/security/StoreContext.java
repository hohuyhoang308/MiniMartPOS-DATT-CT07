package com.pos.security;

import com.pos.exception.BadRequestException;

/**
 * Bối cảnh CHI NHÁNH (đa chuỗi) cho mỗi request.
 *
 * <p>Quy tắc xác định chi nhánh hiệu lực:</p>
 * <ul>
 *   <li>Người dùng GẮN chi nhánh (MANAGER/STAFF) → luôn là chi nhánh của họ
 *       (lấy từ {@link CustomUserDetails#getStoreId()}). Header {@code X-Store-Id} bị BỎ QUA
 *       để không thể tự vượt rào sang chi nhánh khác.</li>
 *   <li>ADMIN (toàn chuỗi, không gắn chi nhánh) → theo chi nhánh đã chọn ở header
 *       {@code X-Store-Id} (do {@link StoreContextFilter} nạp). Không chọn → {@code null}
 *       = phạm vi TOÀN CHUỖI (hợp lệ cho báo cáo/đọc; bị chặn ở thao tác ghi).</li>
 * </ul>
 */
public final class StoreContext {

    private static final ThreadLocal<Long> HEADER_STORE = new ThreadLocal<>();

    private StoreContext() {}

    static void setHeaderStoreId(Long id) {
        if (id != null) HEADER_STORE.set(id);
    }

    static void clear() {
        HEADER_STORE.remove();
    }

    /** Chi nhánh hiệu lực; {@code null} = toàn chuỗi (chỉ ADMIN (toàn chuỗi) khi chưa chọn chi nhánh). */
    public static Long currentStoreId() {
        CustomUserDetails user = SecurityUtils.currentUser();
        Long bound = user.getStoreId();
        if (bound != null) return bound;   // user gắn chi nhánh → luôn theo chi nhánh đó
        return HEADER_STORE.get();         // ADMIN (toàn chuỗi) → chi nhánh đã chọn (có thể null)
    }

    /** Bắt buộc có chi nhánh (cho thao tác GHI: mở ca, nhập kho, tạo kệ, bán hàng…). */
    public static Long requireStoreId() {
        Long s = currentStoreId();
        if (s == null) {
            throw new BadRequestException(
                    "Vui lòng chọn chi nhánh đang làm việc (X-Store-Id) trước khi thực hiện thao tác này");
        }
        return s;
    }

    /**
     * Chặn truy cập CHÉO CHI NHÁNH: bản ghi tra theo id phải thuộc chi nhánh đang làm việc.
     * ADMIN (toàn chuỗi) chưa chọn chi nhánh ({@code currentStoreId()==null}) được phép xem toàn chuỗi.
     */
    public static void assertSameStore(Long recordStoreId) {
        Long cur = currentStoreId();
        if (cur != null && recordStoreId != null && !cur.equals(recordStoreId)) {
            throw new BadRequestException("Bản ghi không thuộc chi nhánh đang làm việc");
        }
    }

    /**
     * Chi nhánh MỤC TIÊU của một thao tác nhận {@code storeId} tùy chọn: ADMIN (toàn chuỗi) được
     * chỉ định cửa hàng qua tham số; người dùng gắn cửa hàng luôn dùng cửa hàng của mình.
     */
    public static Long resolveTargetStoreId(Long requested) {
        if (requested != null && SecurityUtils.isAdmin()) return requested;
        return requireStoreId();
    }
}
