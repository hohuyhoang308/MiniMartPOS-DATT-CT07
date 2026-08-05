package com.pos.service;

import com.pos.dto.report.ReportPeriod;
import com.pos.dto.report.RevenueReportResponse;
import com.pos.entity.Store;
import com.pos.entity.User;
import com.pos.entity.enums.CommonStatus;
import com.pos.entity.enums.Role;
import com.pos.entity.enums.UserStatus;
import com.pos.repository.CashMovementRepository;
import com.pos.repository.DailySalesRollupRepository;
import com.pos.repository.InvoiceItemRepository;
import com.pos.repository.InvoiceRepository;
import com.pos.repository.WorkShiftRepository;
import com.pos.repository.projection.PeriodReportRow;
import com.pos.repository.view.ProductStockViewRepository;
import com.pos.repository.view.ShiftSummaryViewRepository;
import com.pos.security.CustomUserDetails;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Kiểm thử BÁO CÁO DOANH THU – LỢI NHUẬN GỘP (FR10, UC20): tổng kỳ phải khớp tuyệt đối
 * với các điểm dữ liệu hiển thị, kỳ được sắp theo nhãn thời gian, giá trị NULL từ CSDL
 * không làm vỡ phép cộng, và file Excel xuất ra đọc lại được đúng số liệu.
 */
@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock InvoiceRepository invoiceRepository;
    @Mock InvoiceItemRepository invoiceItemRepository;
    @Mock ShiftSummaryViewRepository shiftSummaryRepository;
    @Mock CashMovementRepository cashMovementRepository;
    @Mock WorkShiftRepository workShiftRepository;
    @Mock ProductStockViewRepository productStockRepository;
    @Mock DailySalesRollupRepository rollupRepository;

    ReportService service;

    @BeforeEach
    void setUp() {
        service = new ReportService(invoiceRepository, invoiceItemRepository, shiftSummaryRepository,
                cashMovementRepository, workShiftRepository, productStockRepository, rollupRepository);

        // MANAGER gắn cửa hàng 1 → báo cáo tự lọc theo store_id = 1 (BR-09)
        Store store = new Store();
        store.setId(1L);
        store.setStatus(CommonStatus.ACTIVE);
        User u = new User();
        u.setId(10L);
        u.setUsername("manager");
        u.setRole(Role.MANAGER);
        u.setStatus(UserStatus.ACTIVE);
        u.setStore(store);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(new CustomUserDetails(u), null, List.of()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private static PeriodReportRow row(String bucket, String revenue, String profit, Long count) {
        return new PeriodReportRow() {
            @Override public String getBucket() { return bucket; }
            @Override public BigDecimal getRevenue() { return revenue != null ? new BigDecimal(revenue) : null; }
            @Override public BigDecimal getProfit() { return profit != null ? new BigDecimal(profit) : null; }
            @Override public Long getInvoiceCount() { return count; }
        };
    }

    @Test
    void totals_equal_sum_of_period_points() {
        when(invoiceRepository.revenueByPeriod(any(), any(), any(), eq(1L))).thenReturn(List.of(
                row("2026-07-01", "100000", "20000", 2L),
                row("2026-07-02", "200000", "50000", 3L)));

        RevenueReportResponse resp = service.revenue(
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 2), ReportPeriod.DAY);

        assertThat(resp.totalRevenue()).isEqualByComparingTo("300000");
        assertThat(resp.totalProfit()).isEqualByComparingTo("70000"); // lợi nhuận GỘP = doanh thu − giá vốn theo lô
        assertThat(resp.totalInvoices()).isEqualTo(5);
    }

    @Test
    void sorts_periods_by_label_and_tolerates_null_values() {
        // CSDL trả kỳ KHÔNG theo thứ tự + profit NULL (kỳ không bán được gì) — báo cáo không được vỡ
        when(invoiceRepository.revenueByPeriod(any(), any(), any(), eq(1L))).thenReturn(List.of(
                row("2026-07-03", "50000", null, 1L),
                row("2026-07-01", "100000", "20000", 2L)));

        RevenueReportResponse resp = service.revenue(
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 3), ReportPeriod.DAY);

        assertThat(resp.points()).extracting(RevenueReportResponse.PeriodPoint::label)
                .containsExactly("2026-07-01", "2026-07-03"); // đã sắp tăng dần theo nhãn
        assertThat(resp.totalProfit()).isEqualByComparingTo("20000"); // NULL được coi là 0
    }

    @Test
    void empty_range_returns_zero_totals() {
        when(invoiceRepository.revenueByPeriod(any(), any(), any(), eq(1L))).thenReturn(List.of());

        RevenueReportResponse resp = service.revenue(
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 2), ReportPeriod.DAY);

        assertThat(resp.totalRevenue()).isEqualByComparingTo("0");
        assertThat(resp.totalInvoices()).isZero();
        assertThat(resp.points()).isEmpty();
    }

    @Test
    void excel_export_round_trips_the_same_numbers() throws Exception {
        when(invoiceRepository.revenueByPeriod(any(), any(), any(), eq(1L))).thenReturn(List.of(
                row("2026-07-01", "100000", "20000", 2L)));

        byte[] bytes = service.exportRevenueExcel(
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 1), ReportPeriod.DAY);

        assertThat(bytes).isNotEmpty();
        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet sheet = wb.getSheet("Doanh thu");
            assertThat(sheet).isNotNull();
            // Dòng dữ liệu đầu tiên (index 3): nhãn kỳ + doanh thu đọc lại phải đúng số đã báo cáo
            assertThat(sheet.getRow(3).getCell(0).getStringCellValue()).isEqualTo("2026-07-01");
            assertThat(sheet.getRow(3).getCell(1).getNumericCellValue()).isEqualTo(100000d);
        }
    }
}
