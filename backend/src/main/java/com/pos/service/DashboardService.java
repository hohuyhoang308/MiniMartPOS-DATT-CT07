package com.pos.service;

import com.pos.dto.dashboard.DashboardResponse;
import com.pos.repository.InvoiceItemRepository;
import com.pos.repository.InvoiceRepository;
import com.pos.repository.view.ProductStockViewRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Dashboard (FR9.1). */
@Service
@Transactional(readOnly = true)
public class DashboardService {

    private static final int CHART_DAYS = 7;
    private static final int TOP_PRODUCTS = 5;

    private final InvoiceRepository invoiceRepository;
    private final InvoiceItemRepository invoiceItemRepository;
    private final ProductStockViewRepository stockRepository;

    public DashboardService(InvoiceRepository invoiceRepository,
                            InvoiceItemRepository invoiceItemRepository,
                            ProductStockViewRepository stockRepository) {
        this.invoiceRepository = invoiceRepository;
        this.invoiceItemRepository = invoiceItemRepository;
        this.stockRepository = stockRepository;
    }

    public DashboardResponse getDashboard() {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime startOfTomorrow = today.plusDays(1).atStartOfDay();
        LocalDateTime startOfMonth = today.withDayOfMonth(1).atStartOfDay();

        BigDecimal revenueToday = invoiceRepository.sumRevenue(startOfDay, startOfTomorrow);
        BigDecimal revenueMonth = invoiceRepository.sumRevenue(startOfMonth, startOfTomorrow);
        long invoiceCountToday = invoiceRepository.countCompleted(startOfDay, startOfTomorrow);
        long lowStockCount = stockRepository.findLowStock().size();

        List<DashboardResponse.TopProduct> top = invoiceItemRepository
                .topProducts(startOfMonth, startOfTomorrow, PageRequest.of(0, TOP_PRODUCTS))
                .stream()
                .map(r -> new DashboardResponse.TopProduct(
                        r.getProductId(), r.getProductName(), r.getQuantitySold(), r.getRevenue()))
                .toList();

        LocalDateTime chartFrom = today.minusDays(CHART_DAYS - 1L).atStartOfDay();
        List<DashboardResponse.DailyPoint> chart = invoiceRepository
                .revenueByDay(chartFrom, startOfTomorrow)
                .stream()
                .map(r -> new DashboardResponse.DailyPoint(r.getDay(), r.getRevenue(), r.getInvoiceCount()))
                .toList();

        return new DashboardResponse(
                revenueToday, revenueMonth, invoiceCountToday, lowStockCount, top, chart);
    }
}
