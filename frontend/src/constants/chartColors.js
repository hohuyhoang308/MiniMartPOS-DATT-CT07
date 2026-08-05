/**
 * Màu biểu đồ dùng chung giữa các trang (Dashboard, So sánh chi nhánh, Báo cáo).
 * Đổi ở đây là đồng bộ mọi biểu đồ.
 */

/** Bảng màu cho chuỗi/nhóm (vd doanh thu theo nhóm hàng). */
export const SERIES_COLORS = ['#10b981', '#0ea5e9', '#f59e0b', '#8b5cf6', '#f43f5e', '#14b8a6']

/** Bảng màu cột so sánh chi nhánh (nhiều mục hơn, thứ tự riêng). */
export const STORE_BAR_COLORS = ['#10b981', '#0ea5e9', '#8b5cf6', '#f59e0b', '#f43f5e', '#14b8a6', '#6366f1']

/** Màu vùng tô dưới đường doanh thu (gradient). */
export const REVENUE_FILL = '#10b981'

/** Màu nét đường doanh thu. */
export const REVENUE_STROKE = '#059669'

/** Màu nét đường lợi nhuận. */
export const PROFIT_COLOR = '#8b5cf6'

/** Màu thanh "đang chứa / sức chứa" của kệ theo % đầy: thường → vàng (≥80%) → đỏ (đầy). */
export const capacityBarColor = (pct) => (pct >= 100 ? '#f43f5e' : pct >= 80 ? '#f59e0b' : 'var(--brand-500)')
