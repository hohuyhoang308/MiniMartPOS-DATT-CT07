package com.pos.entity.enums;

/** Loại điều chỉnh trên phiếu lương: thưởng (cộng) hay phạt/tạm ứng (trừ) vào thực lĩnh. */
public enum PayslipAdjustmentType {
    /** Cộng vào thực lĩnh (thưởng doanh số, thưởng chuyên cần…). */
    BONUS,
    /** Trừ vào thực lĩnh (tạm ứng, phạt, bù lệch quỹ…). */
    DEDUCTION
}
