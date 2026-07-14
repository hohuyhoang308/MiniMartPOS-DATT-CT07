package com.pos.service;

import com.pos.dto.report.DailyRollupResponse;
import com.pos.dto.report.EmployeeReportResponse;
import com.pos.dto.report.ProductReportResponse;
import com.pos.dto.report.ReportPeriod;
import com.pos.dto.report.RevenueReportResponse;
import com.pos.dto.report.ShiftReportResponse;
import com.pos.entity.DailySalesRollup;
import com.pos.entity.WorkShift;
import com.pos.entity.enums.ShiftStatus;
import com.pos.entity.view.ProductStockView;
import com.pos.repository.CashMovementRepository;
import com.pos.repository.DailySalesRollupRepository;
import com.pos.repository.InvoiceItemRepository;
import com.pos.repository.InvoiceRepository;
import com.pos.repository.WorkShiftRepository;
import com.pos.repository.projection.EmployeeCashRow;
import com.pos.repository.projection.ProductCogsRow;
import com.pos.repository.projection.ShiftCashRow;
import com.pos.repository.projection.UserQtyRow;
import com.pos.repository.view.ProductStockViewRepository;
import com.pos.repository.view.ShiftSummaryViewRepository;
import com.pos.security.StoreContext;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** Báo cáo doanh thu/ca + xuất Excel (FR9.2, FR9.3). */
@Service
@Transactional(readOnly = true)
public class ReportService {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceItemRepository invoiceItemRepository;
    private final ShiftSummaryViewRepository shiftSummaryRepository;
    private final CashMovementRepository cashMovementRepository;
    private final WorkShiftRepository workShiftRepository;
    private final ProductStockViewRepository productStockRepository;
    private final DailySalesRollupRepository rollupRepository;

    public ReportService(InvoiceRepository invoiceRepository,
                         InvoiceItemRepository invoiceItemRepository,
                         ShiftSummaryViewRepository shiftSummaryRepository,
                         CashMovementRepository cashMovementRepository,
                         WorkShiftRepository workShiftRepository,
                         ProductStockViewRepository productStockRepository,
                         DailySalesRollupRepository rollupRepository) {
        this.invoiceRepository = invoiceRepository;
        this.invoiceItemRepository = invoiceItemRepository;
        this.shiftSummaryRepository = shiftSummaryRepository;
        this.cashMovementRepository = cashMovementRepository;
        this.workShiftRepository = workShiftRepository;
        this.productStockRepository = productStockRepository;
        this.rollupRepository = rollupRepository;
    }

    /**
     * Báo cáo doanh thu/lợi nhuận ngày đọc từ bảng ROLLUP (Obj 2) — nhanh & ổn định với dữ liệu nhiều năm.
     * Store-scoped: chọn chi nhánh → đúng chi nhánh; ADMIN toàn chuỗi → gộp tất cả chi nhánh theo ngày.
     *
     * <p>Trước khi đọc, tổng hợp lại khoảng ngày yêu cầu (upsert idempotent) để số liệu HÔM NAY (và mọi
     * thay đổi sau lần chạy cron 00:30, vd hủy hóa đơn) luôn cập nhật — tránh báo cáo thiếu dữ liệu trong ngày.
     * Upsert là 1 truy vấn gộp theo (chi nhánh, ngày) nên rẻ; cron đêm vẫn giữ vai trò tổng hợp định kỳ.</p>
     */
    @Transactional
    public List<DailyRollupResponse> dailyRollup(LocalDate from, LocalDate to) {
        rollupRepository.upsertRange(from, to);   // làm tươi khoảng ngày yêu cầu (gồm hôm nay) trước khi đọc
        Long storeId = StoreContext.currentStoreId();
        List<DailySalesRollup> rows = storeId != null
                ? rollupRepository.findByStoreIdAndSalesDateBetweenOrderBySalesDate(storeId, from, to)
                : rollupRepository.findBySalesDateBetween(from, to);
        return rows.stream().map(DailyRollupResponse::from).toList();
    }

    public RevenueReportResponse revenue(LocalDate from, LocalDate to, ReportPeriod period) {
        Long storeId = StoreContext.currentStoreId();   // null = toàn chuỗi
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.plusDays(1).atStartOfDay();
        String fmt = period.sqlFormat();

        List<RevenueReportResponse.PeriodPoint> points = new ArrayList<>(invoiceRepository
                .revenueByPeriod(start, end, fmt, storeId).stream()
                .map(r -> new RevenueReportResponse.PeriodPoint(
                        r.getBucket(), nz(r.getRevenue()), nz(r.getProfit()), r.getInvoiceCount()))
                .toList());
        points.sort(Comparator.comparing(RevenueReportResponse.PeriodPoint::label));

        // Tổng tính từ các kỳ để luôn khớp tuyệt đối với bảng/biểu đồ hiển thị.
        BigDecimal totalRevenue = points.stream().map(RevenueReportResponse.PeriodPoint::revenue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalProfit = points.stream().map(RevenueReportResponse.PeriodPoint::profit)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long totalInvoices = points.stream()
                .mapToLong(p -> p.invoiceCount() != null ? p.invoiceCount() : 0).sum();

        return new RevenueReportResponse(from, to, period.name(),
                totalRevenue, totalProfit, totalInvoices, points);
    }

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    public List<ShiftReportResponse> shiftReport() {
        Long storeId = StoreContext.currentStoreId();
        var summaries = storeId == null ? shiftSummaryRepository.findAll()
                                        : shiftSummaryRepository.findByStoreId(storeId);
        // Tiền mặt thực thu của tất cả ca trong 1 truy vấn gộp (bỏ N+1).
        List<Long> shiftIds = summaries.stream().map(v -> v.getShiftId()).toList();
        Map<Long, BigDecimal> cashByShift = shiftIds.isEmpty() ? new HashMap<>()
                : new HashMap<>(invoiceRepository.cashSalesByShiftIds(shiftIds).stream()
                        .collect(Collectors.toMap(ShiftCashRow::getShiftId, r -> nz(r.getAmount()))));
        // Ca ĐÃ ĐÓNG: thay tiền-mặt-bán bằng SNAPSHOT đã chốt lúc đóng (finding #3) để đối soát quỹ cố định,
        // không lệch khi HĐ tiền mặt bị hủy về sau. Ca đang mở: giữ giá trị tính theo thời gian thực ở trên.
        if (!shiftIds.isEmpty()) {
            for (WorkShift ws : workShiftRepository.findAllById(shiftIds)) {
                if (ws.getStatus() == ShiftStatus.CLOSED && ws.getFinalCashSales() != null) {
                    cashByShift.put(ws.getId(), ws.getFinalCashSales());
                }
            }
        }
        // Ròng thu/chi tiền mặt ngoài bán hàng (petty cash) gộp theo ca — để tiền dự kiến khớp trang Ca.
        Map<Long, BigDecimal> netByShift = shiftIds.isEmpty() ? Map.of()
                : cashMovementRepository.netByShiftIds(shiftIds).stream()
                        .collect(Collectors.toMap(ShiftCashRow::getShiftId, r -> nz(r.getAmount())));
        return summaries.stream()
                .map(v -> ShiftReportResponse.from(v,
                        cashByShift.getOrDefault(v.getShiftId(), BigDecimal.ZERO),
                        netByShift.getOrDefault(v.getShiftId(), BigDecimal.ZERO)))
                .toList();
    }

    /**
     * HIỆU SUẤT NHÂN VIÊN trong khoảng [from, to]: gộp doanh số bán (theo ca) + số hàng bán + trách nhiệm quỹ
     * (tổng lệch quỹ các ca đã đóng) theo từng thu ngân — công cụ để quản lý theo dõi nhân viên.
     */
    public List<EmployeeReportResponse> employeeReport(LocalDate from, LocalDate to) {
        Long storeId = StoreContext.currentStoreId();
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.plusDays(1).atStartOfDay();

        Map<Long, Long> itemsByUser = invoiceItemRepository.itemsByCashier(start, end, storeId).stream()
                .collect(Collectors.toMap(UserQtyRow::getUserId, r -> r.getQty() != null ? r.getQty() : 0L));
        Map<Long, EmployeeCashRow> cashByUser = workShiftRepository.cashAccountabilityByCashier(start, end, storeId)
                .stream().collect(Collectors.toMap(EmployeeCashRow::getUserId, r -> r));

        List<EmployeeReportResponse> out = new ArrayList<>();
        Set<Long> seen = new HashSet<>();
        // 1) Thu ngân CÓ bán hàng (đã sắp theo doanh thu giảm dần ở query).
        for (var s : invoiceRepository.salesByCashier(start, end, storeId)) {
            long invoices = s.getInvoiceCount() != null ? s.getInvoiceCount() : 0L;
            BigDecimal revenue = nz(s.getRevenue());
            EmployeeCashRow cash = cashByUser.get(s.getUserId());
            out.add(new EmployeeReportResponse(
                    s.getUserId(), s.getCashierName(), invoices, revenue,
                    itemsByUser.getOrDefault(s.getUserId(), 0L),
                    invoices > 0 ? revenue.divide(BigDecimal.valueOf(invoices), 0, RoundingMode.HALF_UP) : BigDecimal.ZERO,
                    cash != null && cash.getTotalShifts() != null ? cash.getTotalShifts() : 0L,
                    cash != null && cash.getClosedShifts() != null ? cash.getClosedShifts() : 0L,
                    cash != null ? nz(cash.getVariance()) : BigDecimal.ZERO));
            seen.add(s.getUserId());
        }
        // 2) Thu ngân CÓ ca nhưng KHÔNG phát sinh doanh thu (vẫn cần theo dõi trách nhiệm quỹ).
        for (var c : cashByUser.values()) {
            if (seen.contains(c.getUserId())) continue;
            out.add(new EmployeeReportResponse(
                    c.getUserId(), null, 0L, BigDecimal.ZERO, 0L, BigDecimal.ZERO,
                    c.getTotalShifts() != null ? c.getTotalShifts() : 0L,
                    c.getClosedShifts() != null ? c.getClosedShifts() : 0L,
                    nz(c.getVariance())));
        }
        return out;
    }

    /**
     * BÁO CÁO SẢN PHẨM trong khoảng: lợi nhuận/biên theo từng sản phẩm đã bán (doanh thu − COGS FIFO‑lô),
     * và danh sách HÀNG Ế (còn tồn nhưng không bán được đơn vị nào trong khoảng) — chỉ tính khi đã chọn cửa hàng.
     */
    public ProductReportResponse productReport(LocalDate from, LocalDate to) {
        Long storeId = StoreContext.currentStoreId();
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.plusDays(1).atStartOfDay();

        Map<Long, BigDecimal> cogsByProduct = invoiceItemRepository.cogsByProduct(start, end, storeId).stream()
                .collect(Collectors.toMap(ProductCogsRow::getProductId, r -> nz(r.getCogs())));

        Set<Long> soldIds = new HashSet<>();
        List<ProductReportResponse.ProductProfit> products = new ArrayList<>();
        for (var s : invoiceItemRepository.productSales(start, end, storeId)) {
            soldIds.add(s.getProductId());
            BigDecimal revenue = nz(s.getRevenue());
            BigDecimal cogs = cogsByProduct.getOrDefault(s.getProductId(), BigDecimal.ZERO);
            BigDecimal profit = revenue.subtract(cogs);
            BigDecimal margin = revenue.signum() > 0
                    ? profit.multiply(BigDecimal.valueOf(100)).divide(revenue, 1, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            products.add(new ProductReportResponse.ProductProfit(
                    s.getProductId(), s.getProductName(), s.getCategoryName(),
                    s.getQtySold() != null ? s.getQtySold() : 0L, revenue, cogs, profit, margin));
        }

        // Hàng ế: còn tồn trong cửa hàng nhưng không nằm trong tập đã bán của khoảng. Cần cửa hàng cụ thể.
        List<ProductReportResponse.DeadStock> deadStock = new ArrayList<>();
        if (storeId != null) {
            for (ProductStockView v : productStockRepository.findByStoreId(storeId)) {
                long stock = v.getCurrentStock() != null ? v.getCurrentStock() : 0L;
                if (stock > 0 && !soldIds.contains(v.getProductId())) {
                    deadStock.add(new ProductReportResponse.DeadStock(
                            v.getProductId(), v.getName(), v.getBarcode(), stock));
                }
            }
            deadStock.sort(Comparator.comparingLong(ProductReportResponse.DeadStock::currentStock).reversed());
        }
        return new ProductReportResponse(products, deadStock);
    }

    /** Xuất báo cáo doanh thu/lợi nhuận (gộp theo kỳ) ra file Excel (.xlsx). */
    public byte[] exportRevenueExcel(LocalDate from, LocalDate to, ReportPeriod period) {
        RevenueReportResponse report = revenue(from, to, period);
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Doanh thu");

            CellStyle headerStyle = wb.createCellStyle();
            Font bold = wb.createFont();
            bold.setBold(true);
            headerStyle.setFont(bold);

            Row title = sheet.createRow(0);
            title.createCell(0).setCellValue("BÁO CÁO DOANH THU & LỢI NHUẬN ("
                    + periodLabel(period) + ") TỪ " + from + " ĐẾN " + to);

            Row header = sheet.createRow(2);
            String[] cols = {periodLabel(period), "Doanh thu (đ)", "Lợi nhuận (đ)", "Số hóa đơn"};
            for (int i = 0; i < cols.length; i++) {
                Cell c = header.createCell(i);
                c.setCellValue(cols[i]);
                c.setCellStyle(headerStyle);
            }

            int rowIdx = 3;
            for (RevenueReportResponse.PeriodPoint d : report.points()) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(d.label());
                row.createCell(1).setCellValue(d.revenue() != null ? d.revenue().doubleValue() : 0);
                row.createCell(2).setCellValue(d.profit() != null ? d.profit().doubleValue() : 0);
                row.createCell(3).setCellValue(d.invoiceCount() != null ? d.invoiceCount() : 0);
            }

            Row totalRow = sheet.createRow(rowIdx + 1);
            Cell totalLabel = totalRow.createCell(0);
            totalLabel.setCellValue("TỔNG CỘNG");
            totalLabel.setCellStyle(headerStyle);
            totalRow.createCell(1).setCellValue(report.totalRevenue() != null ? report.totalRevenue().doubleValue() : 0);
            totalRow.createCell(2).setCellValue(report.totalProfit() != null ? report.totalProfit().doubleValue() : 0);
            totalRow.createCell(3).setCellValue(report.totalInvoices());

            for (int i = 0; i < cols.length; i++) sheet.autoSizeColumn(i);

            wb.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Lỗi xuất Excel: " + e.getMessage(), e);
        }
    }

    private static String periodLabel(ReportPeriod period) {
        return switch (period) {
            case DAY -> "Ngày";
            case WEEK -> "Tuần";
            case MONTH -> "Tháng";
            case YEAR -> "Năm";
        };
    }
}
