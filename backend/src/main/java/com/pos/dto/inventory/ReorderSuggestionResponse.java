package com.pos.dto.inventory;

import java.math.BigDecimal;

/**
 * Một dòng đề xuất nhập hàng (FR8.3 mở rộng).
 *
 * @param avgDailySold      tốc độ bán trung bình/ngày (làm tròn 1 chữ số thập phân)
 * @param daysUntilStockout số ngày dự kiến còn bán được trước khi hết (null nếu chưa bán/không đoán được)
 * @param suggestedQty      số lượng đề xuất nhập để phủ kỳ dự trữ mục tiêu
 * @param urgency           OUT (hết hàng) | URGENT (sắp hết/dưới ngưỡng) | REORDER (nên nhập)
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
        String urgency
) {}
