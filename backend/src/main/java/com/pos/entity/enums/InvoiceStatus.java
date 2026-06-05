package com.pos.entity.enums;

/**
 * Trạng thái hóa đơn.
 * <ul>
 *   <li>{@code PENDING_PAYMENT} — HĐ QR vừa tạo, đang CHỜ xác nhận tiền về. Giữ chỗ tồn kho
 *       (view v_batch_stock coi là đã bán) nhưng CHƯA tính vào doanh thu/đối soát quỹ.</li>
 *   <li>{@code COMPLETED} — đã hoàn tất (tiền mặt ngay, hoặc QR đã xác nhận PAID).</li>
 *   <li>{@code CANCELLED} — đã hủy / QR quá hạn ⇒ tồn kho tự hoàn (view bỏ qua HĐ CANCELLED).</li>
 * </ul>
 */
public enum InvoiceStatus {
    PENDING_PAYMENT,
    COMPLETED,
    CANCELLED
}
