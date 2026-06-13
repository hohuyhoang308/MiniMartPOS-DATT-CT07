package com.pos.service;

import com.pos.dto.inventory.BatchDetailResponse;
import com.pos.dto.inventory.ExpiringBatchResponse;
import com.pos.dto.inventory.ReorderSuggestionResponse;
import com.pos.dto.inventory.StockResponse;
import com.pos.entity.Product;
import com.pos.entity.Shelf;
import com.pos.repository.InvoiceItemRepository;
import com.pos.repository.ProductRepository;
import com.pos.repository.ShelfRepository;
import com.pos.repository.view.BatchStockViewRepository;
import com.pos.repository.view.ExpiringBatchViewRepository;
import com.pos.repository.view.ProductStockViewRepository;
import com.pos.security.StoreContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    /** Trần EOQ: không đặt quá số ngày bán này trong một lần (tránh EOQ phình cho hàng rẻ/bán chậm). */
    private static final int EOQ_MAX_COVER_DAYS = 60;

    private final ProductStockViewRepository stockRepository;
    private final ExpiringBatchViewRepository expiringRepository;
    private final InvoiceItemRepository invoiceItemRepository;
    private final ProductRepository productRepository;
    private final BatchStockViewRepository batchStockRepository;
    private final ShelfRepository shelfRepository;

    public InventoryService(ProductStockViewRepository stockRepository,
                            ExpiringBatchViewRepository expiringRepository,
                            InvoiceItemRepository invoiceItemRepository,
                            ProductRepository productRepository,
                            BatchStockViewRepository batchStockRepository,
                            ShelfRepository shelfRepository) {
        this.stockRepository = stockRepository;
        this.expiringRepository = expiringRepository;
        this.invoiceItemRepository = invoiceItemRepository;
        this.productRepository = productRepository;
        this.batchStockRepository = batchStockRepository;
        this.shelfRepository = shelfRepository;
    }

    /** Bản đồ id kệ → mã kệ (vd 6 → "K06"). */
    private Map<Long, String> shelfCodeById() {
        return shelfRepository.findAll().stream()
                .collect(Collectors.toMap(Shelf::getId, Shelf::getCode, (a, b) -> a));
    }

    /** Bản đồ sản phẩm → mã kệ đang bày của CHI NHÁNH (kệ có tồn &gt; 0). */
    private Map<Long, String> productShelfCode(Long storeId) {
        Map<Long, String> byId = shelfCodeById();
        Map<Long, String> byProduct = new HashMap<>();
        for (var b : batchStockRepository.findOnShelfByStore(storeId)) {
            if (b.getShelfId() != null) {
                byProduct.putIfAbsent(b.getProductId(), byId.get(b.getShelfId()));
            }
        }
        return byProduct;
    }

    /** Chi tiết các LÔ còn hàng của 1 sản phẩm TẠI CHI NHÁNH (HSD + tồn kho/kệ + Ở KỆ NÀO theo lô). */
    public List<BatchDetailResponse> productBatches(Long productId) {
        Long storeId = StoreContext.requireStoreId();
        Map<Long, String> shelfById = shelfCodeById();
        return batchStockRepository.findProductBatches(productId, storeId).stream()
                .map(v -> BatchDetailResponse.from(v, v.getShelfId() != null ? shelfById.get(v.getShelfId()) : null))
                .toList();
    }

    public List<StockResponse> currentStock() {
        Long storeId = StoreContext.requireStoreId();
        Map<Long, String> byProduct = productShelfCode(storeId);
        return stockRepository.findByStoreId(storeId).stream()
                .map(v -> StockResponse.from(v, byProduct.get(v.getProductId()))).toList();
    }

    public List<StockResponse> lowStock() {
        Long storeId = StoreContext.requireStoreId();
        Map<Long, String> byProduct = productShelfCode(storeId);
        return stockRepository.findLowStock(storeId).stream()
                .map(v -> StockResponse.from(v, byProduct.get(v.getProductId()))).toList();
    }

    public List<ExpiringBatchResponse> expiringBatches() {
        return expiringRepository.findByStoreIdOrderByDaysLeftAsc(StoreContext.requireStoreId()).stream()
                .map(ExpiringBatchResponse::from).toList();
    }

    /**
     * Đề xuất nhập hàng (FR8.3): kết hợp ngưỡng tồn tối thiểu với tốc độ bán {@value VELOCITY_DAYS}
     * ngày để gợi ý mặt hàng cần nhập và số lượng nhập. Sắp theo độ khẩn rồi tới số ngày còn bán.
     */
    public List<ReorderSuggestionResponse> reorderSuggestions() {
        Long storeId = StoreContext.requireStoreId();
        LocalDateTime from = LocalDateTime.now().minusDays(VELOCITY_DAYS);
        Map<Long, Long> soldByProduct = new HashMap<>();
        for (var row : invoiceItemRepository.soldQuantitySince(from, storeId)) {
            soldByProduct.put(row.getProductId(), row.getSoldQty() != null ? row.getSoldQty() : 0L);
        }
        // Tổng bình phương lượng bán theo NGÀY (để tính phương sai nhu cầu σ²).
        Map<Long, Double> sumSqByProduct = new HashMap<>();
        for (var row : invoiceItemRepository.dailySalesSince(from, storeId)) {
            double q = row.getSoldQty() != null ? row.getSoldQty() : 0;
            sumSqByProduct.merge(row.getProductId(), q * q, Double::sum);
        }
        Map<Long, BigDecimal> costByProduct = productRepository.findAll().stream()
                .collect(Collectors.toMap(Product::getId, Product::getCostPrice, (a, b) -> a));

        // KẾT NỐI ABC/XYZ → phân hóa chính sách đặt hàng theo độ quan trọng (ABC) & độ ổn định (XYZ).
        Map<Long, String[]> classMap = abcXyzAnalysis().stream()
                .collect(Collectors.toMap(com.pos.dto.inventory.AbcXyzResponse::productId,
                        r -> new String[]{r.abcClass(), r.xyzClass()}, (a, b) -> a));
        // KẾT NỐI HSD → cảnh báo sản phẩm đang có lô cận/quá hạn (đừng vội nhập thêm khi còn hàng sắp hết hạn).
        Set<Long> expiringProducts = expiringRepository.findByStoreIdOrderByDaysLeftAsc(storeId).stream()
                .map(com.pos.entity.view.ExpiringBatchView::getProductId).collect(Collectors.toSet());

        List<ReorderSuggestionResponse> result = new ArrayList<>();
        for (var v : stockRepository.findByStoreId(storeId)) {
            long current = v.getCurrentStock() != null ? v.getCurrentStock() : 0L;
            int min = v.getMinStock() != null ? v.getMinStock() : 0;
            long sold = soldByProduct.getOrDefault(v.getProductId(), 0L);
            double avgDaily = sold / (double) VELOCITY_DAYS;

            // Nhóm phân loại (mặc định C·Z nếu không có doanh thu 90 ngày — coi như đuôi & thất thường).
            String[] cls = classMap.getOrDefault(v.getProductId(), new String[]{"C", "Z"});
            String abc = cls[0], xyz = cls[1];

            // Độ lệch chuẩn nhu cầu/ngày: σ = √(E[q²] − E[q]²), tính trên cả ngày không bán (=0).
            double meanSq = sumSqByProduct.getOrDefault(v.getProductId(), 0.0) / VELOCITY_DAYS;
            double sigma = Math.sqrt(Math.max(0, meanSq - avgDaily * avgDaily));
            // Tồn an toàn theo MỨC PHỤC VỤ phân hóa theo ABC: A giữ kỹ hơn (~98%), C nới (~90%).
            double safetyStock = serviceZ(abc) * sigma * Math.sqrt(LEAD_DAYS);
            // Z (thất thường): chặn tồn an toàn phình to → tránh ôm hàng chết (đúng tinh thần "giảm tồn" nhóm Z).
            if ("Z".equals(xyz)) safetyStock = Math.min(safetyStock, avgDaily * LEAD_DAYS);
            double reorderPoint = avgDaily * LEAD_DAYS + safetyStock;

            boolean needReorder = current <= reorderPoint || current <= min;
            // C·Z (ít & thất thường): chỉ đề xuất khi THỰC chạm ngưỡng/hết → đặt thưa, đỡ đọng vốn.
            if ("C".equals(abc) && "Z".equals(xyz)) needReorder = current <= min;
            if (!needReorder) continue;

            // Tồn mục tiêu: đủ bán theo kỳ dự trữ (C đặt thưa → kỳ dài hơn). Sàn theo nhóm: A/B giữ 2×
            // ngưỡng để chắc còn hàng; C chỉ về TỚI ngưỡng (giảm tồn, tránh ôm hàng chậm luân chuyển).
            double floor = "C".equals(abc) ? min : min * 2.0;
            double targetStock = Math.max(floor, avgDaily * coverageDays(abc));
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
            // Chặn EOQ phi thực tế cho hàng rẻ/bán chậm (H nhỏ → EOQ phình): không quá ~2 tháng bán.
            eoq = Math.min(eoq, Math.max(suggestedQty, (int) Math.ceil(avgDaily * EOQ_MAX_COVER_DAYS)));

            // Độ khẩn theo THỜI GIAN còn bán, không theo "dưới ngưỡng": dưới min mà còn nhiều ngày bán → chỉ REORDER.
            String urgency;
            if (current <= 0) urgency = "OUT";
            else if (daysLeft != null && daysLeft <= LEAD_DAYS) urgency = "URGENT"; // sắp hết trong thời gian giao hàng
            else urgency = "REORDER";

            result.add(new ReorderSuggestionResponse(
                    v.getProductId(), v.getBarcode(), v.getName(),
                    current, min, sold,
                    BigDecimal.valueOf(avgDaily).setScale(1, RoundingMode.HALF_UP),
                    daysLeft, suggestedQty, reorderPointInt, eoq, urgency,
                    abc, xyz, expiringProducts.contains(v.getProductId())));
        }

        // Khẩn nhất lên đầu (OUT > URGENT > REORDER); cùng độ khẩn thì nhóm A lên trước, rồi ít ngày còn bán hơn.
        result.sort((a, b) -> {
            int ua = urgencyRank(a.urgency()), ub = urgencyRank(b.urgency());
            if (ua != ub) return Integer.compare(ua, ub);
            int ra = abcRank(a.abcClass()), rb = abcRank(b.abcClass());
            if (ra != rb) return Integer.compare(ra, rb);
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

    /** Mức phục vụ z theo nhóm ABC: A giữ kỹ (~98%), B (~95%), C nới (~90%) — dồn vốn cho hàng chủ lực. */
    private static double serviceZ(String abc) {
        return switch (abc) { case "A" -> 2.05; case "B" -> 1.65; default -> 1.28; };
    }

    /** Kỳ dự trữ mục tiêu (ngày) theo nhóm ABC: C đặt thưa, gom đơn → kỳ dài hơn để giảm số lần đặt. */
    private static int coverageDays(String abc) {
        return "C".equals(abc) ? 21 : COVERAGE_DAYS;
    }

    private static int abcRank(String abc) {
        return switch (abc) { case "A" -> 0; case "B" -> 1; default -> 2; };
    }

    /** Cửa sổ phân tích ABC/XYZ (1 quý). */
    private static final int ABC_DAYS = 90;
    /** Số "rổ" TUẦN trong cửa sổ (90 ngày ≈ 13 tuần) — đơn vị tính độ biến động nhu cầu cho XYZ. */
    private static final int ABC_WEEKS = (int) Math.ceil(ABC_DAYS / 7.0);

    /**
     * Phân loại ABC/XYZ: ABC theo doanh thu luỹ kế (Pareto 80/95%), XYZ theo độ biến động nhu cầu
     * (CV = σ/μ). Giúp dồn kiểm soát chặt cho nhóm A/X, nới nhóm C/Z. Xếp theo doanh thu giảm dần.
     */
    public List<com.pos.dto.inventory.AbcXyzResponse> abcXyzAnalysis() {
        Long storeId = StoreContext.requireStoreId();
        LocalDateTime from = LocalDateTime.now().minusDays(ABC_DAYS);

        // Độ biến động (XYZ) tính trên lượng bán theo TUẦN — gộp tuần để bỏ nhiễu "ngày bán = 0"
        // của bán lẻ, tránh việc mọi mặt hàng đều bị xếp Z (thất thường) một cách giả tạo.
        Map<Long, Double> weeklySumSq = new HashMap<>();
        for (var row : invoiceItemRepository.weeklySalesSince(from, storeId)) {
            double q = row.getSoldQty() != null ? row.getSoldQty() : 0;
            weeklySumSq.merge(row.getProductId(), q * q, Double::sum);
        }
        Map<Long, String> names = productRepository.findAll().stream()
                .collect(Collectors.toMap(Product::getId, Product::getName, (a, b) -> a));

        var rows = invoiceItemRepository.revenueByProductSince(from, storeId).stream()
                .filter(r -> r.getRevenue() != null && r.getRevenue().signum() > 0)
                .sorted((a, b) -> b.getRevenue().compareTo(a.getRevenue()))
                .toList();
        double totalRevenue = rows.stream().mapToDouble(r -> r.getRevenue().doubleValue()).sum();
        if (totalRevenue <= 0) return List.of();

        List<com.pos.dto.inventory.AbcXyzResponse> result = new ArrayList<>();
        double cum = 0;
        for (var r : rows) {
            double rev = r.getRevenue().doubleValue();
            double share = rev / totalRevenue * 100.0;
            // Xếp theo luỹ kế TRƯỚC item (Pareto): item làm vượt 80% vẫn thuộc A; item đầu luôn là A.
            String abc = cum < 80.0 ? "A" : cum < 95.0 ? "B" : "C";
            cum += share;

            long qty = r.getQty() != null ? r.getQty() : 0;
            // μ, σ theo TUẦN: trung bình = tổng bán / số tuần; tuần không bán tính như 0 (vẫn vào mẫu số).
            double meanWeekly = qty / (double) ABC_WEEKS;
            double sigmaWeekly = Math.sqrt(Math.max(0,
                    weeklySumSq.getOrDefault(r.getProductId(), 0.0) / ABC_WEEKS - meanWeekly * meanWeekly));
            double cv = meanWeekly > 0 ? sigmaWeekly / meanWeekly : 0;
            String xyz = cv < 0.5 ? "X" : cv < 1.0 ? "Y" : "Z";

            result.add(new com.pos.dto.inventory.AbcXyzResponse(
                    r.getProductId(), names.getOrDefault(r.getProductId(), "?"),
                    r.getRevenue(), round1(share), round1(cum), abc, qty, round2(cv), xyz));
        }
        return result;
    }

    private static double round1(double v) { return Math.round(v * 10.0) / 10.0; }
    private static double round2(double v) { return Math.round(v * 100.0) / 100.0; }
}
