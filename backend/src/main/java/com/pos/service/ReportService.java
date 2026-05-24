package com.pos.service;

import com.pos.dto.report.ReportPeriod;
import com.pos.dto.report.RevenueReportResponse;
import com.pos.dto.report.ShiftReportResponse;
import com.pos.repository.InvoiceItemRepository;
import com.pos.repository.InvoiceRepository;
import com.pos.repository.view.ShiftSummaryViewRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Báo cáo doanh thu/ca + xuất Excel (FR9.2, FR9.3). */
@Service
@Transactional(readOnly = true)
public class ReportService {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceItemRepository invoiceItemRepository;
    private final ShiftSummaryViewRepository shiftSummaryRepository;

    public ReportService(InvoiceRepository invoiceRepository,
                         InvoiceItemRepository invoiceItemRepository,
                         ShiftSummaryViewRepository shiftSummaryRepository) {
        this.invoiceRepository = invoiceRepository;
        this.invoiceItemRepository = invoiceItemRepository;
        this.shiftSummaryRepository = shiftSummaryRepository;
    }

    public RevenueReportResponse revenue(LocalDate from, LocalDate to, ReportPeriod period) {
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.plusDays(1).atStartOfDay();

        List<RevenueReportResponse.PeriodPoint> points = invoiceRepository
                .revenueByPeriod(start, end, period.sqlFormat()).stream()
                .map(r -> new RevenueReportResponse.PeriodPoint(
                        r.getBucket(), nz(r.getRevenue()), nz(r.getProfit()), r.getInvoiceCount()))
                .toList();

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
        return shiftSummaryRepository.findAll().stream()
                .map(v -> ShiftReportResponse.from(v, invoiceRepository.sumCashSalesByShift(v.getShiftId())))
                .toList();
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
