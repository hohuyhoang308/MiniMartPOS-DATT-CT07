package com.pos.entity.enums;

/**
 * Loại biến động quỹ tiền mặt NGOÀI bán hàng trong một ca (petty cash).
 * <ul>
 *   <li>{@code IN}  — THU vào két (nộp thêm tiền, đổi tiền lẻ vào quỹ...).</li>
 *   <li>{@code OUT} — CHI ra khỏi két (trả tiền NCC, chi phí vận hành, rút bớt tiền...).</li>
 * </ul>
 * Dùng để đối soát quỹ cuối ca: tiền dự kiến = đầu ca + tiền mặt bán + tổng THU − tổng CHI.
 */
public enum CashMovementType {
    IN,
    OUT
}
