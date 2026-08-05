package com.pos.service;

import com.pos.dto.dashboard.DashboardResponse;
import com.pos.dto.dashboard.StoreKpiResponse;
import com.pos.repository.InvoiceItemRepository;
import com.pos.repository.InvoiceRepository;
import com.pos.repository.PayslipRepository;
import com.pos.repository.StoreRepository;
import com.pos.repository.projection.StoreAmountRow;
import com.pos.repository.projection.StoreCountRow;
import com.pos.repository.view.ExpiringBatchViewRepository;
import com.pos.repository.view.ProductStockViewRepository;
import com.pos.security.StoreContext;
import com.pos.util.Money;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Dashboard (FR9.1) — tổng hợp KPI, lợi nhuận, cơ cấu thanh toán, giờ cao điểm, danh mục, giao dịch gần đây.
 *  Lọc theo CHI NHÁNH đang làm việc (đa chuỗi); ADMIN (toàn chuỗi) chưa chọn chi nhánh → toàn chuỗi. */
@Service
@Transactional(readOnly = true)
public class DashboardService {

    private static final int CHART_DAYS = 7;
    private static final int TOP_PRODUCTS = 5;
    private static final int TOP_CATEGORIES = 6;
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private final InvoiceRepository invoiceRepository;
    private final InvoiceItemRepository invoiceItemRepository;
    private final ProductStockViewRepository stockRepository;
    private final ExpiringBatchViewRepository expiringRepository;
    private final StoreRepository storeRepository;
    private final PayslipRepository payslipRepository;

    public DashboardService(InvoiceRepository invoiceRepository,
                            InvoiceItemRepository invoiceItemRepository,
                            ProductStockViewRepository stockRepository,
                            ExpiringBatchViewRepository expiringRepository,
                            StoreRepository storeRepository,
                            PayslipRepository payslipRepository) {
        this.invoiceRepository = invoiceRepository;
        this.invoiceItemRepository = invoiceItemRepository;
        this.stockRepository = stockRepository;
        this.expiringRepository = expiringRepository;
        this.storeRepository = storeRepository;
        this.payslipRepository = payslipRepository;
    }

    public DashboardResponse getDashboard() {
        Long storeId = StoreContext.currentStoreId();   // null = toàn chuỗi (ADMIN (toàn chuỗi) chưa chọn chi nhánh)
        LocalDate today = LocalDate.now();
        LocalDateTime startToday = today.atStartOfDay();
        LocalDateTime startTomorrow = today.plusDays(1).atStartOfDay();
        LocalDateTime startYesterday = today.minusDays(1).atStartOfDay();
        LocalDateTime startMonth = today.withDayOfMonth(1).atStartOfDay();

        BigDecimal revenueToday = invoiceRepository.sumRevenue(startToday, startTomorrow, storeId);
        BigDecimal revenueYesterday = invoiceRepository.sumRevenue(startYesterday, startToday, storeId);
        BigDecimal revenueMonth = invoiceRepository.sumRevenue(startMonth, startTomorrow, storeId);
        // Lợi nhuận gộp = doanh thu thuần (đã trừ khuyến mãi ở total_amount) − giá vốn hàng bán (COGS).
        BigDecimal profitToday = revenueToday.subtract(invoiceItemRepository.sumCogs(startToday, startTomorrow, storeId));
        BigDecimal profitMonth = revenueMonth.subtract(invoiceItemRepository.sumCogs(startMonth, startTomorrow, storeId));

        // Chi phí nhân sự: tổng quỹ lương (Σ thực lĩnh) của kỳ lương tháng này & tỷ lệ trên doanh thu tháng.
        String month = today.format(DateTimeFormatter.ofPattern("yyyy-MM"));
        BigDecimal laborCostMonth = payslipRepository.sumNetByMonth(month, storeId);
        if (laborCostMonth == null) laborCostMonth = BigDecimal.ZERO;
        BigDecimal laborCostRatio = revenueMonth.signum() > 0
                ? laborCostMonth.multiply(BigDecimal.valueOf(100)).divide(revenueMonth, 1, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        long invoiceCountToday = invoiceRepository.countCompleted(startToday, startTomorrow, storeId);
        long itemsSoldToday = invoiceItemRepository.sumQuantity(startToday, startTomorrow, storeId);
        long customersToday = invoiceRepository.countDistinctCustomers(startToday, startTomorrow, storeId);
        BigDecimal avgOrder = invoiceCountToday > 0
                ? Money.prorate(revenueToday, BigDecimal.ONE, BigDecimal.valueOf(invoiceCountToday))
                : BigDecimal.ZERO;

        long lowStock = (storeId == null ? stockRepository.findLowStockAll() : stockRepository.findLowStock(storeId)).size();
        long outOfStock = storeId == null ? stockRepository.countOutOfStockAll() : stockRepository.countOutOfStock(storeId);
        long expiring = storeId == null ? expiringRepository.count() : expiringRepository.countByStoreId(storeId);

        List<DashboardResponse.TopProduct> top = invoiceItemRepository
                .topProducts(startMonth, startTomorrow, storeId, PageRequest.of(0, TOP_PRODUCTS))
                .stream().map(r -> new DashboardResponse.TopProduct(
                        r.getProductId(), r.getProductName(), r.getQuantitySold(), r.getRevenue())).toList();

        LocalDateTime chartFrom = today.minusDays(CHART_DAYS - 1L).atStartOfDay();
        List<DashboardResponse.DailyPoint> chart = invoiceRepository.revenueByDay(chartFrom, startTomorrow, storeId)
                .stream().map(r -> new DashboardResponse.DailyPoint(r.getDay(), r.getRevenue(), r.getInvoiceCount())).toList();

        List<DashboardResponse.PaymentSlice> payments = invoiceRepository.paymentBreakdown(startToday, startTomorrow, storeId)
                .stream().map(r -> new DashboardResponse.PaymentSlice(r.getMethod(), r.getCnt(), r.getAmount())).toList();

        List<DashboardResponse.HourPoint> hourly = invoiceRepository.hourlySales(startToday, startTomorrow, storeId)
                .stream().map(r -> new DashboardResponse.HourPoint(r.getHour(), r.getRevenue(), r.getInvoiceCount())).toList();

        List<DashboardResponse.CategorySlice> categories = invoiceItemRepository.categorySales(startMonth, startTomorrow, storeId)
                .stream().limit(TOP_CATEGORIES)
                .map(r -> new DashboardResponse.CategorySlice(r.getCategoryName(), r.getRevenue(), r.getQuantity())).toList();

        List<DashboardResponse.RecentInvoice> recent =
                (storeId == null ? invoiceRepository.findTop8ByOrderByCreatedAtDesc()
                                 : invoiceRepository.findTop8ByStoreIdOrderByCreatedAtDesc(storeId))
                .stream().map(i -> new DashboardResponse.RecentInvoice(
                        i.getCode(), i.getTotalAmount(), i.getPaymentMethod().name(), i.getStatus().name(),
                        i.getShift().getUser().getFullName(), i.getCreatedAt().format(TIME_FMT))).toList();

        return new DashboardResponse(
                revenueToday, revenueYesterday, revenueMonth, profitToday, profitMonth,
                laborCostMonth, laborCostRatio,
                invoiceCountToday, itemsSoldToday, customersToday, avgOrder,
                lowStock, outOfStock, expiring,
                top, chart, payments, hourly, categories, recent);
    }

    /**
     * So sánh KPI giữa CÁC chi nhánh (toàn chuỗi) — chỉ dành cho quản trị viên toàn chuỗi (ADMIN).
     * Bỏ qua chi nhánh đang chọn ở header: luôn duyệt TẤT CẢ cửa hàng để xếp hạng, so sánh.
     * Sắp xếp theo doanh thu tháng giảm dần (cửa hàng mạnh nhất lên đầu).
     */
    public List<StoreKpiResponse> getStoreComparison() {
        LocalDate today = LocalDate.now();
        LocalDateTime startToday = today.atStartOfDay();
        LocalDateTime startTomorrow = today.plusDays(1).atStartOfDay();
        LocalDateTime startMonth = today.withDayOfMonth(1).atStartOfDay();

        // 6 truy vấn GỘP theo chi nhánh (HẰNG SỐ, không phụ thuộc số cửa hàng) — thay cho vòng lặp N+1.
        Map<Long, BigDecimal> revToday = amountMap(invoiceRepository.revenueByStore(startToday, startTomorrow));
        Map<Long, BigDecimal> revMonth = amountMap(invoiceRepository.revenueByStore(startMonth, startTomorrow));
        Map<Long, BigDecimal> cogsMonth = amountMap(invoiceItemRepository.cogsByStore(startMonth, startTomorrow));
        Map<Long, Long> invToday = countMap(invoiceRepository.countByStore(startToday, startTomorrow));
        Map<Long, Long> lowStock = countMap(stockRepository.lowStockCountByStore());
        Map<Long, Long> outStock = countMap(stockRepository.outOfStockCountByStore());

        return storeRepository.findAll().stream()
                .map(s -> {
                    Long id = s.getId();
                    BigDecimal rMonth = revMonth.getOrDefault(id, BigDecimal.ZERO);
                    BigDecimal profitMonth = rMonth.subtract(cogsMonth.getOrDefault(id, BigDecimal.ZERO));
                    return new StoreKpiResponse(id, s.getCode(), s.getName(), s.getStatus(),
                            revToday.getOrDefault(id, BigDecimal.ZERO), rMonth, profitMonth,
                            invToday.getOrDefault(id, 0L),
                            lowStock.getOrDefault(id, 0L), outStock.getOrDefault(id, 0L));
                })
                .sorted(Comparator.comparing(StoreKpiResponse::revenueMonth).reversed())
                .toList();
    }

    private static Map<Long, BigDecimal> amountMap(List<StoreAmountRow> rows) {
        Map<Long, BigDecimal> m = new HashMap<>();
        for (StoreAmountRow r : rows) if (r.getStoreId() != null) m.put(r.getStoreId(), r.getAmount());
        return m;
    }

    private static Map<Long, Long> countMap(List<StoreCountRow> rows) {
        Map<Long, Long> m = new HashMap<>();
        for (StoreCountRow r : rows) if (r.getStoreId() != null) m.put(r.getStoreId(), r.getCount());
        return m;
    }
}
