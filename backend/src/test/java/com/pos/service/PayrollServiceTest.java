package com.pos.service;

import com.pos.dto.payroll.AttendanceEntryRequest;
import com.pos.dto.payroll.AttendanceEntryResponse;
import com.pos.dto.payroll.PayslipAdjustmentRequest;
import com.pos.entity.*;
import com.pos.entity.enums.*;
import com.pos.exception.BadRequestException;
import com.pos.repository.*;
import com.pos.repository.projection.AttendanceOverlapRow;
import com.pos.repository.projection.LongShiftRow;
import com.pos.security.CustomUserDetails;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Kiểm thử công thức tính lương (applyPay/recomputeTotals) và các rào chắn nghiệp vụ:
 * chặn thực lĩnh âm, cảnh báo ca dài bất thường & chấm công trùng ngày có ca.
 * Thuần nghiệp vụ với repository mock — không cần CSDL.
 */
@ExtendWith(MockitoExtension.class)
class PayrollServiceTest {

    @Mock UserRepository userRepository;
    @Mock StoreRepository storeRepository;
    @Mock EmployeePayProfileRepository profileRepository;
    @Mock PayrollPeriodRepository periodRepository;
    @Mock PayslipRepository payslipRepository;
    @Mock PayslipAdjustmentRepository adjustmentRepository;
    @Mock WorkShiftRepository shiftRepository;
    @Mock AttendanceEntryRepository attendanceRepository;
    @Mock TelegramService telegramService;
    @Mock AuditService auditService;

    PayrollService service;

    @BeforeEach
    void setUp() {
        service = new PayrollService(userRepository, storeRepository, profileRepository,
                periodRepository, payslipRepository, adjustmentRepository,
                shiftRepository, attendanceRepository, telegramService, auditService);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // --- dữ liệu mẫu ---

    private Store store(long id) {
        Store s = new Store();
        s.setId(id);
        s.setName("Chi nhánh " + id);
        return s;
    }

    private User user(long id, Role role, Store store) {
        User u = new User();
        u.setId(id);
        u.setUsername("u" + id);
        u.setFullName("Người dùng " + id);
        u.setRole(role);
        u.setStore(store);
        u.setStatus(UserStatus.ACTIVE);
        return u;
    }

    /** Đăng nhập giả lập: đặt CustomUserDetails vào SecurityContext (giống JwtAuthFilter). */
    private void loginAs(User u) {
        CustomUserDetails details = new CustomUserDetails(u);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities()));
    }

    private EmployeePayProfile profile(PayType type, String rate, String standard, String otMult, String allowance) {
        EmployeePayProfile p = new EmployeePayProfile();
        p.setPayType(type);
        p.setBaseRate(new BigDecimal(rate));
        p.setStandardMonthlyHours(new BigDecimal(standard));
        p.setOtMultiplier(new BigDecimal(otMult));
        p.setMonthlyAllowance(new BigDecimal(allowance));
        return p;
    }

    // =====================================================================
    //  CÔNG THỨC LƯƠNG — applyPay
    // =====================================================================

    @Test
    void hourly_pay_without_overtime() {
        Payslip slip = new Payslip();
        service.applyPay(slip, profile(PayType.HOURLY, "30000", "208", "1.5", "0"),
                new BigDecimal("100"), 12);

        assertThat(slip.getRegularHours()).isEqualByComparingTo("100");
        assertThat(slip.getOtHours()).isEqualByComparingTo("0");
        assertThat(slip.getRegularPay()).isEqualByComparingTo("3000000");
        assertThat(slip.getOtPay()).isEqualByComparingTo("0");
        assertThat(slip.getGrossPay()).isEqualByComparingTo("3000000");
        assertThat(slip.getShiftCount()).isEqualTo(12);
    }

    @Test
    void hourly_overtime_beyond_standard_uses_multiplier() {
        Payslip slip = new Payslip();
        service.applyPay(slip, profile(PayType.HOURLY, "30000", "208", "1.5", "0"),
                new BigDecimal("250"), 30);

        assertThat(slip.getRegularHours()).isEqualByComparingTo("208");
        assertThat(slip.getOtHours()).isEqualByComparingTo("42");
        assertThat(slip.getRegularPay()).isEqualByComparingTo("6240000");   // 208 × 30.000
        assertThat(slip.getOtPay()).isEqualByComparingTo("1890000");        // 42 × 30.000 × 1,5
        assertThat(slip.getGrossPay()).isEqualByComparingTo("8130000");
    }

    @Test
    void monthly_full_hours_earns_full_salary() {
        Payslip slip = new Payslip();
        service.applyPay(slip, profile(PayType.MONTHLY, "8320000", "208", "1.5", "0"),
                new BigDecimal("208"), 26);

        assertThat(slip.getRegularPay()).isEqualByComparingTo("8320000");
        assertThat(slip.getOtPay()).isEqualByComparingTo("0");
        assertThat(slip.getGrossPay()).isEqualByComparingTo("8320000");
    }

    @Test
    void monthly_partial_hours_prorated() {
        Payslip slip = new Payslip();
        service.applyPay(slip, profile(PayType.MONTHLY, "8320000", "208", "1.5", "0"),
                new BigDecimal("104"), 13);

        assertThat(slip.getRegularPay()).isEqualByComparingTo("4160000");   // 8.320.000 × 104/208
        assertThat(slip.getGrossPay()).isEqualByComparingTo("4160000");
    }

    @Test
    void monthly_overtime_paid_at_converted_hourly_rate() {
        Payslip slip = new Payslip();
        service.applyPay(slip, profile(PayType.MONTHLY, "8320000", "208", "1.5", "0"),
                new BigDecimal("250"), 30);

        // đơn giá giờ quy đổi = 8.320.000/208 = 40.000 → OT = 42 × 40.000 × 1,5
        assertThat(slip.getRegularPay()).isEqualByComparingTo("8320000");
        assertThat(slip.getOtPay()).isEqualByComparingTo("2520000");
        assertThat(slip.getGrossPay()).isEqualByComparingTo("10840000");
    }

    @Test
    void allowance_is_added_to_gross_pay() {
        Payslip slip = new Payslip();
        service.applyPay(slip, profile(PayType.HOURLY, "30000", "208", "1.5", "500000"),
                new BigDecimal("100"), 12);

        assertThat(slip.getAllowance()).isEqualByComparingTo("500000");
        assertThat(slip.getGrossPay()).isEqualByComparingTo("3500000");
    }

    @Test
    void missing_profile_defaults_to_monthly_zero_salary() {
        Payslip slip = new Payslip();
        service.applyPay(slip, null, new BigDecimal("100"), 10);

        assertThat(slip.getPayType()).isEqualTo(PayType.MONTHLY);
        assertThat(slip.getBaseRate()).isEqualByComparingTo("0");
        assertThat(slip.getStandardHours()).isEqualByComparingTo("208");
        assertThat(slip.getGrossPay()).isEqualByComparingTo("0");
    }

    // =====================================================================
    //  THƯỞNG / PHẠT — recomputeTotals & rào chắn thực lĩnh âm
    // =====================================================================

    private Payslip slipInDraft(long slipId, User staff, Store store, String gross) {
        PayrollPeriod period = new PayrollPeriod();
        period.setId(9L);
        period.setStore(store);
        period.setPeriodMonth("2026-06");
        period.setStatus(PayrollStatus.DRAFT);
        Payslip slip = new Payslip();
        slip.setId(slipId);
        slip.setPeriod(period);
        slip.setUser(staff);
        slip.setPayType(PayType.HOURLY);
        slip.setGrossPay(new BigDecimal(gross));
        slip.setNetPay(new BigDecimal(gross));
        return slip;
    }

    private PayslipAdjustment adjustment(PayslipAdjustmentType type, String amount) {
        PayslipAdjustment a = new PayslipAdjustment();
        a.setType(type);
        a.setAmount(new BigDecimal(amount));
        a.setReason("test");
        return a;
    }

    @Test
    void net_pay_combines_bonus_and_deduction() {
        Payslip slip = new Payslip();
        slip.setId(7L);
        slip.setGrossPay(new BigDecimal("5000000"));
        when(adjustmentRepository.findByPayslipIdOrderByCreatedAt(7L)).thenReturn(List.of(
                adjustment(PayslipAdjustmentType.BONUS, "500000"),
                adjustment(PayslipAdjustmentType.DEDUCTION, "200000")));

        service.recomputeTotals(slip);

        assertThat(slip.getTotalBonus()).isEqualByComparingTo("500000");
        assertThat(slip.getTotalDeduction()).isEqualByComparingTo("200000");
        assertThat(slip.getNetPay()).isEqualByComparingTo("5300000");
    }

    @Test
    void deduction_exceeding_net_pay_is_rejected() {
        Store st = store(1);
        loginAs(user(2, Role.MANAGER, st));
        Payslip slip = slipInDraft(7L, user(3, Role.STAFF, st), st, "5000000");
        when(payslipRepository.findById(7L)).thenReturn(java.util.Optional.of(slip));

        assertThatThrownBy(() -> service.addAdjustment(7L,
                new PayslipAdjustmentRequest(PayslipAdjustmentType.DEDUCTION, new BigDecimal("6000000"), "Tạm ứng")))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("vượt quá");
        verify(adjustmentRepository, never()).save(any());
    }

    @Test
    void deduction_within_net_pay_is_accepted() {
        Store st = store(1);
        loginAs(user(2, Role.MANAGER, st));
        Payslip slip = slipInDraft(7L, user(3, Role.STAFF, st), st, "5000000");
        when(payslipRepository.findById(7L)).thenReturn(java.util.Optional.of(slip));
        List<PayslipAdjustment> saved = new ArrayList<>();
        when(adjustmentRepository.save(any())).thenAnswer(inv -> {
            PayslipAdjustment a = inv.getArgument(0);
            saved.add(a);
            return a;
        });
        when(adjustmentRepository.findByPayslipIdOrderByCreatedAt(7L))
                .thenAnswer(inv -> new ArrayList<>(saved));

        var resp = service.addAdjustment(7L,
                new PayslipAdjustmentRequest(PayslipAdjustmentType.DEDUCTION, new BigDecimal("2000000"), "Tạm ứng"));

        assertThat(resp.netPay()).isEqualByComparingTo("3000000");
    }

    @Test
    void submit_is_blocked_when_a_payslip_has_negative_net_pay() {
        Store st = store(1);
        loginAs(user(2, Role.MANAGER, st));
        PayrollPeriod period = new PayrollPeriod();
        period.setId(9L);
        period.setStore(st);
        period.setPeriodMonth("2026-06");
        period.setStatus(PayrollStatus.DRAFT);
        when(periodRepository.findById(9L)).thenReturn(java.util.Optional.of(period));
        when(payslipRepository.countByPeriodId(9L)).thenReturn(3L);
        when(payslipRepository.existsByPeriod_IdAndNetPayLessThan(9L, BigDecimal.ZERO)).thenReturn(true);

        assertThatThrownBy(() -> service.submit(9L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("thực lĩnh âm");
        assertThat(period.getStatus()).isEqualTo(PayrollStatus.DRAFT);
    }

    // =====================================================================
    //  CẢNH BÁO BẢNG CÔNG — ca dài bất thường & chấm công trùng ngày có ca
    // =====================================================================

    @Test
    void long_closed_shift_produces_warning() {
        LongShiftRow row = mock(LongShiftRow.class);
        when(row.getShiftId()).thenReturn(12L);
        when(row.getFullName()).thenReturn("Nguyễn Văn A");
        when(row.getOpenedAt()).thenReturn(LocalDateTime.of(2026, 6, 30, 8, 0));
        when(row.getHours()).thenReturn(new BigDecimal("26.5"));
        when(shiftRepository.findLongShifts(anyLong(), any(), any(), anyInt())).thenReturn(List.of(row));
        when(attendanceRepository.findOverlapWithShifts(anyLong(), any(), any())).thenReturn(List.of());

        List<String> warnings = service.buildWarnings(1L, YearMonth.of(2026, 6));

        assertThat(warnings).hasSize(1);
        assertThat(warnings.get(0)).contains("Nguyễn Văn A").contains("26,5").contains("quên đóng");
    }

    @Test
    void same_day_shift_and_attendance_produces_warning() {
        AttendanceOverlapRow row = mock(AttendanceOverlapRow.class);
        when(row.getFullName()).thenReturn("Trần Thị B");
        when(row.getWorkDate()).thenReturn(LocalDate.of(2026, 6, 5));
        when(row.getHours()).thenReturn(new BigDecimal("8"));
        when(shiftRepository.findLongShifts(anyLong(), any(), any(), anyInt())).thenReturn(List.of());
        when(attendanceRepository.findOverlapWithShifts(anyLong(), any(), any())).thenReturn(List.of(row));

        List<String> warnings = service.buildWarnings(1L, YearMonth.of(2026, 6));

        assertThat(warnings).hasSize(1);
        assertThat(warnings.get(0)).contains("Trần Thị B").contains("05/06").contains("trùng");
    }

    @Test
    void no_anomalies_produce_no_warnings() {
        when(shiftRepository.findLongShifts(anyLong(), any(), any(), anyInt())).thenReturn(List.of());
        when(attendanceRepository.findOverlapWithShifts(anyLong(), any(), any())).thenReturn(List.of());

        assertThat(service.buildWarnings(1L, YearMonth.of(2026, 6))).isEmpty();
    }

    // =====================================================================
    //  CHẤM CÔNG — cảnh báo khi ngày đó nhân viên đã có ca
    // =====================================================================

    private AttendanceEntryRequest attReq(long userId, LocalDate date) {
        return new AttendanceEntryRequest(userId, date, AttendanceType.WORK, new BigDecimal("8"), null);
    }

    @Test
    void attendance_on_day_with_shift_returns_warning() {
        Store st = store(1);
        loginAs(user(2, Role.MANAGER, st));
        User staff = user(3, Role.STAFF, st);
        when(userRepository.findById(3L)).thenReturn(java.util.Optional.of(staff));
        when(attendanceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(shiftRepository.existsByUser_IdAndOpenedAtGreaterThanEqualAndOpenedAtLessThan(
                eq(3L), any(), any())).thenReturn(true);

        AttendanceEntryResponse resp = service.addAttendance(attReq(3L, LocalDate.of(2026, 6, 5)));

        assertThat(resp.warning()).isNotNull().contains("đã có ca làm việc");
    }

    @Test
    void attendance_on_free_day_has_no_warning() {
        Store st = store(1);
        loginAs(user(2, Role.MANAGER, st));
        User staff = user(3, Role.STAFF, st);
        when(userRepository.findById(3L)).thenReturn(java.util.Optional.of(staff));
        when(attendanceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(shiftRepository.existsByUser_IdAndOpenedAtGreaterThanEqualAndOpenedAtLessThan(
                eq(3L), any(), any())).thenReturn(false);

        AttendanceEntryResponse resp = service.addAttendance(attReq(3L, LocalDate.of(2026, 6, 5)));

        assertThat(resp.warning()).isNull();
    }
}
