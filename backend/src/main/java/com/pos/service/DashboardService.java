package com.pos.service;

import com.pos.dto.dashboard.DashboardResponse;
import com.pos.repository.InvoiceItemRepository;
import com.pos.repository.InvoiceRepository;
import com.pos.repository.view.ExpiringBatchViewRepository;
import com.pos.repository.view.ProductStockViewRepository;
import com.pos.security.StoreContext;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Dashboard (FR9.1) — tổng hợp KPI, lợi nhuận, cơ cấu thanh toán, giờ cao điểm, danh mục, giao dịch gần đây.
 *  Lọc theo CHI NHÁNH đang làm việc (đa chuỗi); CHAIN_ADMIN chưa chọn chi nhánh → toàn chuỗi. */
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

    public DashboardService(InvoiceRepository invoiceRepository,
                            InvoiceItemRepository invoiceItemRepository,
                            ProductStockViewRepository stockRepository,
                            ExpiringBatchViewRepository expiringRepository) {
        this.invoiceRepository = invoiceRepository;
        this.invoiceItemRepository = invoiceItemRepository;
        this.stockRepository = stockRepository;
        this.expiringRepository = expiringRepository;
    }

    public DashboardResponse getDashboard() {
        Long storeId = StoreContext.currentStoreId();   // null = toàn chuỗi (CHAIN_ADMIN chưa chọn chi nhánh)
        LocalDate today = LocalDate.now();
        LocalDateTime startToday = today.atStartOfDay();
        LocalDateTime startTomorrow = today.plusDays(1).atStartOfDay();
        LocalDateTime startYesterday = today.minusDays(1).atStartOfDay();
        LocalDateTime startMonth = today.withDayOfMonth(1).atStartOfDay();

        BigDecimal revenueToday = invoiceRepository.sumRevenue(startToday, startTomorrow, storeId);
        BigDecimal revenueYesterday = invoiceRepository.sumRevenue(startYesterday, startToday, storeId);
        BigDecimal revenueMonth = invoiceRepository.sumRevenue(startMonth, startTomorrow, storeId);
        BigDecimal profitToday = invoiceItemRepository.sumProfit(startToday, startTomorrow, storeId);
        BigDecimal profitMonth = invoiceItemRepository.sumProfit(startMonth, startTomorrow, storeId);

        long invoiceCountToday = invoiceRepository.countCompleted(startToday, startTomorrow, storeId);
        long itemsSoldToday = invoiceItemRepository.sumQuantity(startToday, startTomorrow, storeId);
        long customersToday = invoiceRepository.countDistinctCustomers(startToday, startTomorrow, storeId);
        BigDecimal avgOrder = invoiceCountToday > 0
                ? revenueToday.divide(BigDecimal.valueOf(invoiceCountToday), 0, RoundingMode.HALF_UP)
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
                invoiceCountToday, itemsSoldToday, customersToday, avgOrder,
                lowStock, outOfStock, expiring,
                top, chart, payments, hourly, categories, recent);
    }
}
