package com.pos.dto.report;

import java.math.BigDecimal;
import java.util.List;

/**
 * Báo cáo SẢN PHẨM (theo dõi sức khỏe hàng hóa của cửa hàng):
 * <ul>
 *   <li>{@code products}  — lợi nhuận/biên theo từng sản phẩm đã bán trong khoảng.</li>
 *   <li>{@code deadStock} — hàng CÒN TỒN nhưng KHÔNG bán được đơn vị nào trong khoảng (hàng ế/đọng vốn).</li>
 * </ul>
 */
public record ProductReportResponse(
        List<ProductProfit> products,
        List<DeadStock> deadStock
) {
    /** Lợi nhuận/biên của một sản phẩm trong khoảng. */
    public record ProductProfit(
            Long productId,
            String productName,
            String categoryName,
            long qtySold,
            BigDecimal revenue,
            BigDecimal cogs,
            BigDecimal profit,
            BigDecimal marginPercent   // profit / revenue × 100 (0 nếu revenue = 0)
    ) {}

    /** Hàng tồn nhưng không bán được trong khoảng (ứ đọng). */
    public record DeadStock(
            Long productId,
            String name,
            String barcode,
            long currentStock
    ) {}
}
