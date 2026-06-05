package com.pos.dto.inventory;

import java.math.BigDecimal;

/**
 * Một dòng đề xuất nhập hàng (FR8.3 mở rộng).
 *
 * @param avgDailySold      tốc độ bán trung bình/ngày (làm tròn 1 chữ số thập phân)
 * @param daysUntilStockout số ngày dự kiến còn bán được trước khi hết (null nếu chưa bán/không đoán được)
 * @param suggestedQty      số lượng đề xuất nhập để phủ kỳ dự trữ mục tiêu
 * @param reorderPoint      điểm tái đặt hàng = nhu cầu trong lead time + tồn an toàn (khi tồn ≤ mức này thì nên đặt)
 * @param eoq               lượng đặt hàng kinh tế EOQ = √(2·D·S/H) (số lượng đặt tối ưu mỗi lần)
 * @param urgency           OUT (hết hàng) | URGENT (sắp hết/dưới ngưỡng) | REORDER (nên nhập)
 * @param abcClass          nhóm ABC theo doanh thu (A/B/C) — quyết định mức phục vụ &amp; kỳ dự trữ
 * @param xyzClass          nhóm XYZ theo độ ổn định nhu cầu (X/Y/Z)
 * @param hasExpiringStock  có lô cận/quá HSD → cân nhắc đẩy bán trước khi nhập thêm (tránh ôm hàng hết hạn)
 */
public record ReorderSuggestionResponse(
        Long productId,
        String barcode,
        String name,
        Long currentStock,
        Integer minStock,
        Long soldLast30,
        BigDecimal avgDailySold,
        Integer daysUntilStockout,
        int suggestedQty,
        int reorderPoint,
        int eoq,
        String urgency,
        String abcClass,
        String xyzClass,
        boolean hasExpiringStock
) {}
