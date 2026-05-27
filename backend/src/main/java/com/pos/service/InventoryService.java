package com.pos.service;

import com.pos.dto.inventory.ExpiringBatchResponse;
import com.pos.dto.inventory.ReorderSuggestionResponse;
import com.pos.dto.inventory.StockResponse;
import com.pos.entity.Product;
import com.pos.repository.InvoiceItemRepository;
import com.pos.repository.ProductRepository;
import com.pos.repository.view.ExpiringBatchViewRepository;
import com.pos.repository.view.ProductStockViewRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Tồn kho & cảnh báo (FR8 - UC17). Toàn bộ suy ra từ view (1 nguồn sự thật). */
@Service
@Transactional(readOnly = true)
public class InventoryService {

    /** Cửa sổ tính tốc độ bán. */
    private static final int VELOCITY_DAYS = 30;
    /** Số ngày giao hàng dự kiến của NCC (điểm đặt lại = ngưỡng + bán trong leadtime). */
    private static final int LEAD_DAYS = 3;
    /** Kỳ dự trữ mục tiêu khi nhập (nhập đủ bán ~2 tuần). */
    private static final int COVERAGE_DAYS = 14;
    /** Chi phí cố định mỗi lần đặt hàng (S) — giả định cho EOQ. */
    private static final double ORDER_COST = 50_000;
    /** Tỷ lệ chi phí lưu kho/năm trên giá vốn (H) — giả định 20%/năm. */
    private static final double HOLDING_RATE = 0.20;

    private final ProductStockViewRepository stockRepository;
    private final ExpiringBatchViewRepository expiringRepository;
    private final InvoiceItemRepository invoiceItemRepository;
    private final ProductRepository productRepository;

    public InventoryService(ProductStockViewRepository stockRepository,
                            ExpiringBatchViewRepository expiringRepository,
                            InvoiceItemRepository invoiceItemRepository,
                            ProductRepository productRepository) {
        this.stockRepository = stockRepository;
        this.expiringRepository = expiringRepository;
        this.invoiceItemRepository = invoiceItemRepository;
        this.productRepository = productRepository;
    }

    public List<StockResponse> currentStock() {
        return stockRepository.findAll().stream().map(StockResponse::from).toList();
    }

    public List<StockResponse> lowStock() {
        return stockRepository.findLowStock().stream().map(StockResponse::from).toList();
    }

    public List<ExpiringBatchResponse> expiringBatches() {
        return expiringRepository.findAllByOrderByDaysLeftAsc().stream()
                .map(ExpiringBatchResponse::from).toList();
    }

    /**
     * Đề xuất nhập hàng (FR8.3): kết hợp ngưỡng tồn tối thiểu với tốc độ bán {@value VELOCITY_DAYS}
     * ngày để gợi ý mặt hàng cần nhập và số lượng nhập. Sắp theo độ khẩn rồi tới số ngày còn bán.
     */
    public List<ReorderSuggestionResponse> reorderSuggestions() {
        LocalDateTime from = LocalDateTime.now().minusDays(VELOCITY_DAYS);
        Map<Long, Long> soldByProduct = new HashMap<>();
        for (var row : invoiceItemRepository.soldQuantitySince(from)) {
            soldByProduct.put(row.getProductId(), row.getSoldQty() != null ? row.getSoldQty() : 0L);
        }
        Map<Long, BigDecimal> costByProduct = productRepository.findAll().stream()
                .collect(Collectors.toMap(Product::getId, Product::getCostPrice, (a, b) -> a));

        List<ReorderSuggestionResponse> result = new ArrayList<>();
        for (var v : stockRepository.findAll()) {
            long current = v.getCurrentStock() != null ? v.getCurrentStock() : 0L;
            int min = v.getMinStock() != null ? v.getMinStock() : 0;
            long sold = soldByProduct.getOrDefault(v.getProductId(), 0L);
            double avgDaily = sold / (double) VELOCITY_DAYS;

            // Điểm đặt lại = ngưỡng tối thiểu + lượng bán trong thời gian chờ giao.
            double reorderPoint = min + avgDaily * LEAD_DAYS;
            boolean needReorder = current <= reorderPoint || current <= min;
            if (!needReorder) continue;

            // Tồn mục tiêu: đủ bán COVERAGE_DAYS, nhưng không thấp hơn 2× ngưỡng cảnh báo.
            double targetStock = Math.max(min * 2.0, avgDaily * COVERAGE_DAYS);
            int suggestedQty = (int) Math.max(0, Math.ceil(targetStock - current));
            if (suggestedQty == 0) suggestedQty = Math.max(min, 1); // vẫn nên nhập tối thiểu

            Integer daysLeft = avgDaily > 0 ? (int) Math.floor(current / avgDaily) : null;

            // EOQ = √(2·D·S/H): D nhu cầu/năm, S chi phí đặt hàng, H chi phí lưu kho/đơn vị/năm.
            int reorderPointInt = (int) Math.ceil(reorderPoint);
            double annualDemand = avgDaily * 365;
            double holding = HOLDING_RATE * costByProduct.getOrDefault(v.getProductId(), BigDecimal.ZERO).doubleValue();
            int eoq = (annualDemand > 0 && holding > 0)
                    ? (int) Math.round(Math.sqrt(2 * annualDemand * ORDER_COST / holding))
                    : suggestedQty;

            String urgency;
            if (current <= 0) urgency = "OUT";
            else if (current <= min || (daysLeft != null && daysLeft <= LEAD_DAYS)) urgency = "URGENT";
            else urgency = "REORDER";

            result.add(new ReorderSuggestionResponse(
                    v.getProductId(), v.getBarcode(), v.getName(),
                    current, min, sold,
                    BigDecimal.valueOf(avgDaily).setScale(1, RoundingMode.HALF_UP),
                    daysLeft, suggestedQty, reorderPointInt, eoq, urgency));
        }

        // Khẩn nhất lên đầu (OUT > URGENT > REORDER), trong cùng nhóm thì ít ngày còn bán hơn lên trước.
        result.sort((a, b) -> {
            int ua = urgencyRank(a.urgency()), ub = urgencyRank(b.urgency());
            if (ua != ub) return Integer.compare(ua, ub);
            int da = a.daysUntilStockout() != null ? a.daysUntilStockout() : Integer.MAX_VALUE;
            int db = b.daysUntilStockout() != null ? b.daysUntilStockout() : Integer.MAX_VALUE;
            return Integer.compare(da, db);
        });
        return result;
    }

    private static int urgencyRank(String urgency) {
        return switch (urgency) {
            case "OUT" -> 0;
            case "URGENT" -> 1;
            default -> 2;
        };
    }
}
