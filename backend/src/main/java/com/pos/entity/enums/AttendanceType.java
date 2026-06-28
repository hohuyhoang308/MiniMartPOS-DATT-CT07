package com.pos.entity.enums;

/** Loại bản ghi chấm công thủ công (module Lương/chấm công). */
public enum AttendanceType {
    /** Giờ làm thực tế NGOÀI ca thu ngân (NV kho/bảo vệ không mở ca, hoặc bổ sung/sửa công). Tính lương. */
    WORK,
    /** Nghỉ phép CÓ lương (phép năm) — tính như giờ công hưởng lương. */
    LEAVE_PAID,
    /** Nghỉ KHÔNG lương — chỉ ghi nhận để theo dõi, không tính vào lương. */
    LEAVE_UNPAID
}
