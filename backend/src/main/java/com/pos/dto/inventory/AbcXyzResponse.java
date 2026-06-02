package com.pos.dto.inventory;

import java.math.BigDecimal;

/**
 * Phân loại ABC/XYZ một sản phẩm.
 * <ul>
 *   <li>ABC theo doanh thu (Pareto): A ≤ 80% luỹ kế, B ≤ 95%, C còn lại.</li>
 *   <li>XYZ theo độ biến động nhu cầu (CV = σ/μ): X &lt; 0.5 (ổn định), Y &lt; 1.0, Z ≥ 1.0 (thất thường).</li>
 * </ul>
 */
public record AbcXyzResponse(
        Long productId,
        String name,
        BigDecimal revenue,
        double revenueShare,     // % doanh thu của SP này
        double cumulativeShare,  // % luỹ kế (để xếp ABC)
        String abcClass,
        long soldQty,
        double cv,               // hệ số biến thiên nhu cầu
        String xyzClass
) {}
