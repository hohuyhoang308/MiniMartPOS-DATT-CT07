package com.pos.service;

import com.pos.dto.report.RevenueReportResponse;
import com.pos.dto.report.ShiftReportResponse;
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
    private final ShiftSummaryViewRepository shiftSummaryRepository;

    public ReportService(InvoiceRepository invoiceRepository,
                         ShiftSummaryViewRepository shiftSummaryRepository) {
        this.invoiceRepository = invoiceRepository;
        this.shiftSummaryRepository = shiftSummaryRepository;
    }

    public RevenueReportResponse revenue(LocalDate from, LocalDate to) {
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.plusDays(1).atStartOfDay();
        BigDecimal total = invoiceRepository.sumRevenue(start, end);
        long count = invoiceRepository.countCompleted(start, end);
        List<RevenueReportResponse.DailyPoint> days = invoiceRepository.revenueByDay(start, end).stream()
                .map(r -> new RevenueReportResponse.DailyPoint(r.getDay(), r.getRevenue(), r.getInvoiceCount()))
                .toList();
        return new RevenueReportResponse(from, to, total, count, days);
    }

    public List<ShiftReportResponse> shiftReport() {
        return shiftSummaryRepository.findAll().stream().map(ShiftReportResponse::from).toList();
    }

    /** Xuất báo cáo doanh thu theo ngày ra file Excel (.xlsx). */
    public byte[] exportRevenueExcel(LocalDate from, LocalDate to) {
        RevenueReportResponse report = revenue(from, to);
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Doanh thu");

            CellStyle headerStyle = wb.createCellStyle();
            Font bold = wb.createFont();
            bold.setBold(true);
            headerStyle.setFont(bold);

            Row title = sheet.createRow(0);
            title.createCell(0).setCellValue("BÁO CÁO DOANH THU TỪ " + from + " ĐẾN " + to);

            Row header = sheet.createRow(2);
            String[] cols = {"Ngày", "Doanh thu (đ)", "Số hóa đơn"};
            for (int i = 0; i < cols.length; i++) {
                Cell c = header.createCell(i);
                c.setCellValue(cols[i]);
                c.setCellStyle(headerStyle);
            }

            int rowIdx = 3;
            for (RevenueReportResponse.DailyPoint d : report.days()) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(d.day());
                row.createCell(1).setCellValue(d.revenue() != null ? d.revenue().doubleValue() : 0);
                row.createCell(2).setCellValue(d.invoiceCount() != null ? d.invoiceCount() : 0);
            }

            Row totalRow = sheet.createRow(rowIdx + 1);
            Cell totalLabel = totalRow.createCell(0);
            totalLabel.setCellValue("TỔNG CỘNG");
            totalLabel.setCellStyle(headerStyle);
            totalRow.createCell(1).setCellValue(report.totalRevenue() != null ? report.totalRevenue().doubleValue() : 0);
            totalRow.createCell(2).setCellValue(report.totalInvoices());

            for (int i = 0; i < cols.length; i++) sheet.autoSizeColumn(i);

            wb.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Lỗi xuất Excel: " + e.getMessage(), e);
        }
    }
}
