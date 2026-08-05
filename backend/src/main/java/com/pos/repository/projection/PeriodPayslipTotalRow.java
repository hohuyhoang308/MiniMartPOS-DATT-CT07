package com.pos.repository.projection;

import java.math.BigDecimal;

/** Tổng hợp phiếu lương theo KỲ: số phiếu + tổng thực lĩnh — cho danh sách kỳ lương (bỏ N+1). */
public interface PeriodPayslipTotalRow {
    Long getPeriodId();
    Long getCnt();
    BigDecimal getTotalNet();
}
