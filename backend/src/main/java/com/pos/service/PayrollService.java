package com.pos.service;

import com.pos.dto.payroll.*;
import com.pos.entity.*;
import com.pos.entity.enums.PayType;
import com.pos.entity.enums.PayrollStatus;
import com.pos.entity.enums.PayslipAdjustmentType;
import com.pos.entity.enums.Role;
import com.pos.exception.BadRequestException;
import com.pos.exception.NotFoundException;
import com.pos.repository.*;
import com.pos.repository.projection.AttendanceHoursRow;
import com.pos.repository.projection.WorkedHoursRow;
import com.pos.security.SecurityUtils;
import com.pos.security.StoreContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

/**
 * LƯƠNG & BẢNG CÔNG (module Lương) — UC tính lương từ ca làm việc.
 *
 * <p>Công suy ra từ {@link WorkShift} đã đóng (giờ = closed_at − opened_at), cộng dồn theo
 * (nhân viên × chi nhánh × tháng). Lương = công × đơn giá (theo {@link EmployeePayProfile}),
 * cộng tăng ca/phụ cấp, cộng/trừ thưởng-phạt. Xem docs/PAYROLL_DESIGN.md.</p>
 *
 * <p>Đa chuỗi & phân quyền giống {@link UserService}: ADMIN toàn chuỗi (theo chi nhánh đang chọn),
 * MANAGER chỉ chi nhánh của mình. Mọi thao tác trạng thái/đổi mức lương đều ghi audit.</p>
 */
@Service
@Transactional(readOnly = true)
public class PayrollService {

    private static final BigDecimal DEFAULT_STANDARD_HOURS = new BigDecimal("208");
    private static final BigDecimal DEFAULT_OT_MULTIPLIER = new BigDecimal("1.5");

    private final UserRepository userRepository;
    private final StoreRepository storeRepository;
    private final EmployeePayProfileRepository profileRepository;
    private final PayrollPeriodRepository periodRepository;
    private final PayslipRepository payslipRepository;
    private final PayslipAdjustmentRepository adjustmentRepository;
    private final WorkShiftRepository shiftRepository;
    private final AttendanceEntryRepository attendanceRepository;
    private final TelegramService telegramService;
    private final AuditService auditService;

    public PayrollService(UserRepository userRepository,
                          StoreRepository storeRepository,
                          EmployeePayProfileRepository profileRepository,
                          PayrollPeriodRepository periodRepository,
                          PayslipRepository payslipRepository,
                          PayslipAdjustmentRepository adjustmentRepository,
                          WorkShiftRepository shiftRepository,
                          AttendanceEntryRepository attendanceRepository,
                          TelegramService telegramService,
                          AuditService auditService) {
        this.userRepository = userRepository;
        this.storeRepository = storeRepository;
        this.profileRepository = profileRepository;
        this.periodRepository = periodRepository;
        this.payslipRepository = payslipRepository;
        this.adjustmentRepository = adjustmentRepository;
        this.shiftRepository = shiftRepository;
        this.attendanceRepository = attendanceRepository;
        this.telegramService = telegramService;
        this.auditService = auditService;
    }

    // =====================================================================
    //  CẤU HÌNH LƯƠNG / NHÂN VIÊN
    // =====================================================================

    /** Danh sách nhân viên + cấu hình lương (theo phạm vi chi nhánh). Nhân viên chưa cấu hình → mặc định. */
    public List<PayProfileResponse> listPayProfiles() {
        List<User> employees = employeesInScope();
        Map<Long, EmployeePayProfile> byUser = employees.isEmpty() ? Map.of()
                : profileRepository.findByUserIdIn(employees.stream().map(User::getId).toList())
                        .stream().collect(Collectors.toMap(p -> p.getUser().getId(), p -> p));
        return employees.stream()
                .map(u -> PayProfileResponse.forUser(u, byUser.get(u.getId())))
                .toList();
    }

    /** Đặt/cập nhật cấu hình lương cho một nhân viên. */
    @Transactional
    public PayProfileResponse setPayProfile(Long userId, PayProfileRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> NotFoundException.of("tài khoản", userId));
        assertCanManageEmployee(user);
        EmployeePayProfile profile = profileRepository.findByUserId(userId)
                .orElseGet(() -> {
                    EmployeePayProfile p = new EmployeePayProfile();
                    p.setUser(user);
                    return p;
                });
        profile.setPayType(req.payType());
        profile.setBaseRate(req.baseRate());
        profile.setStandardMonthlyHours(req.standardMonthlyHours());
        profile.setOtMultiplier(req.otMultiplier());
        profile.setMonthlyAllowance(req.monthlyAllowance());
        profile.setUpdatedBy(SecurityUtils.currentUserId());
        EmployeePayProfile saved = profileRepository.save(profile);
        auditService.log("SET_PAY_PROFILE", "USER", userId,
                "Cấu hình lương " + user.getFullName() + ": " + req.payType()
                        + " " + req.baseRate() + "đ" + (req.payType() == PayType.HOURLY ? "/giờ" : "/tháng")
                        + ", phụ cấp " + req.monthlyAllowance() + "đ");
        return PayProfileResponse.forUser(user, saved);
    }

    // =====================================================================
    //  KỲ LƯƠNG
    // =====================================================================

    /** Danh sách kỳ lương theo phạm vi chi nhánh (mới nhất trước). */
    public List<PayrollPeriodResponse> listPeriods() {
        Long storeId = StoreContext.currentStoreId();
        List<PayrollPeriod> periods = storeId == null
                ? periodRepository.findTop200ByOrderByPeriodMonthDesc()
                : periodRepository.findByStoreIdOrderByPeriodMonthDesc(storeId);
        return periods.stream()
                .map(p -> PayrollPeriodResponse.summary(p,
                        (int) payslipRepository.countByPeriodId(p.getId()),
                        nz(payslipRepository.sumNetByPeriodId(p.getId()))))
                .toList();
    }

    /** Chi tiết một kỳ + danh sách phiếu lương. */
    public PayrollPeriodResponse getPeriod(Long periodId) {
        PayrollPeriod period = getPeriodOrThrow(periodId);
        StoreContext.assertSameStore(period.getStore().getId());
        return PayrollPeriodResponse.detail(period, payslipsOf(periodId));
    }

    /**
     * TẠO hoặc TÍNH LẠI kỳ lương cho chi nhánh đang chọn + tháng {@code 'YYYY-MM'}.
     * Đọc lại giờ công từ ca đã đóng, tính lương gốc/tăng ca; GIỮ NGUYÊN các dòng thưởng/phạt
     * đã nhập (chỉ cộng lại tổng). Chỉ cho phép khi kỳ đang DRAFT.
     */
    @Transactional
    public PayrollPeriodResponse compute(String month) {
        YearMonth ym = parseMonth(month);
        Long storeId = StoreContext.requireStoreId();   // bắt buộc chọn chi nhánh để ghi
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> NotFoundException.of("cửa hàng", storeId));

        PayrollPeriod period = periodRepository.findByStoreIdAndPeriodMonth(storeId, month)
                .orElseGet(() -> {
                    PayrollPeriod p = new PayrollPeriod();
                    p.setStore(store);
                    p.setPeriodMonth(month);
                    p.setStatus(PayrollStatus.DRAFT);
                    p.setCreatedBy(SecurityUtils.currentUserId());
                    return periodRepository.save(p);
                });
        if (period.getStatus() != PayrollStatus.DRAFT) {
            throw new BadRequestException("Kỳ lương đã trình duyệt/đã chốt — không thể tính lại. "
                    + "Nếu cần sửa, hãy trả về bản nháp trước.");
        }

        LocalDateTime from = ym.atDay(1).atStartOfDay();
        LocalDateTime to = ym.plusMonths(1).atDay(1).atStartOfDay();
        // Giờ công đến từ HAI nguồn (đa nguồn chấm công): (1) ca thu ngân đã đóng,
        // (2) chấm công thủ công + nghỉ phép có lương (NV không mở ca/sửa công).
        List<WorkedHoursRow> shiftRows = shiftRepository.workedHoursByEmployee(storeId, from, to);
        List<AttendanceHoursRow> attRows = attendanceRepository.paidHoursByEmployee(
                storeId, ym.atDay(1), ym.atEndOfMonth());

        Map<Long, BigDecimal> shiftHours = new HashMap<>();
        Map<Long, Integer> shiftCount = new HashMap<>();
        Map<Long, String> names = new HashMap<>();
        for (WorkedHoursRow r : shiftRows) {
            shiftHours.put(r.getUserId(), nz(r.getWorkedHours()));
            shiftCount.put(r.getUserId(), r.getShiftCount() != null ? r.getShiftCount().intValue() : 0);
            names.put(r.getUserId(), r.getFullName());
        }
        Map<Long, BigDecimal> attHours = new HashMap<>();
        for (AttendanceHoursRow r : attRows) {
            attHours.put(r.getUserId(), nz(r.getPaidHours()));
            names.putIfAbsent(r.getUserId(), r.getFullName());
        }
        // Tập nhân viên được trả lương = người CÓ ca HOẶC CÓ chấm công hưởng lương trong kỳ.
        Set<Long> allUsers = new HashSet<>(shiftHours.keySet());
        allUsers.addAll(attHours.keySet());

        List<Long> userIds = new ArrayList<>(allUsers);
        Map<Long, EmployeePayProfile> profiles = userIds.isEmpty() ? Map.of()
                : profileRepository.findByUserIdIn(userIds).stream()
                        .collect(Collectors.toMap(p -> p.getUser().getId(), p -> p));

        // Upsert phiếu cho từng người có công; giữ phiếu cũ (để giữ thưởng/phạt) rồi tính lại lương gốc.
        Map<Long, Payslip> existing = payslipRepository.findByPeriodIdOrderByUserId(period.getId())
                .stream().collect(Collectors.toMap(p -> p.getUser().getId(), p -> p));
        for (Long uid : allUsers) {
            Payslip slip = existing.get(uid);
            if (slip == null) {
                slip = new Payslip();
                slip.setPeriod(period);
                slip.setUser(userRepository.getReferenceById(uid));
            }
            // Tổng giờ công hưởng lương = giờ ca + giờ chấm công/nghỉ phép có lương.
            BigDecimal worked = nz(shiftHours.get(uid)).add(nz(attHours.get(uid)));
            applyPay(slip, profiles.get(uid), worked, shiftCount.getOrDefault(uid, 0));
            payslipRepository.save(slip);
            recomputeTotals(slip);   // gộp thưởng/phạt hiện có vào thực lĩnh
        }
        // Xóa phiếu của người KHÔNG còn công trong kỳ (vd ca/chấm công bị xóa) — kèm các dòng điều chỉnh.
        for (Payslip stale : existing.values()) {
            if (!allUsers.contains(stale.getUser().getId())) {
                adjustmentRepository.deleteAll(adjustmentRepository.findByPayslipIdOrderByCreatedAt(stale.getId()));
                payslipRepository.delete(stale);
            }
        }

        auditService.log("COMPUTE_PAYROLL", "PAYROLL_PERIOD", period.getId(),
                "Tính lương kỳ " + month + " — " + store.getName() + " (" + allUsers.size() + " nhân viên)");
        return PayrollPeriodResponse.detail(period, payslipsOf(period.getId()));
    }

    /** TRÌNH DUYỆT (bước 1): DRAFT → PENDING_APPROVAL. Đóng băng số liệu, chờ cấp trên duyệt. */
    @Transactional
    public PayrollPeriodResponse submit(Long periodId) {
        PayrollPeriod period = getPeriodOrThrow(periodId);
        StoreContext.assertSameStore(period.getStore().getId());
        if (period.getStatus() != PayrollStatus.DRAFT) {
            throw new BadRequestException("Chỉ trình duyệt được kỳ đang ở trạng thái nháp (DRAFT)");
        }
        if (payslipRepository.countByPeriodId(periodId) == 0) {
            throw new BadRequestException("Kỳ lương chưa có phiếu nào — hãy tính lương trước khi trình duyệt");
        }
        period.setStatus(PayrollStatus.PENDING_APPROVAL);
        period.setSubmittedBy(SecurityUtils.currentUserId());
        period.setSubmittedAt(LocalDateTime.now());
        periodRepository.save(period);
        auditService.log("SUBMIT_PAYROLL", "PAYROLL_PERIOD", periodId,
                "Trình duyệt kỳ lương " + period.getPeriodMonth() + " — " + period.getStore().getName());
        return PayrollPeriodResponse.detail(period, payslipsOf(periodId));
    }

    /**
     * DUYỆT (bước 2): PENDING_APPROVAL → APPROVED. CHỈ ADMIN (tách trách nhiệm với người lập).
     * Phát thông báo Telegram tới chi nhánh khi chốt.
     */
    @Transactional
    public PayrollPeriodResponse approve(Long periodId) {
        PayrollPeriod period = getPeriodOrThrow(periodId);
        StoreContext.assertSameStore(period.getStore().getId());
        if (!SecurityUtils.isAdmin()) {
            throw new BadRequestException("Chỉ quản trị viên mới được duyệt bảng lương (tách trách nhiệm lập/duyệt)");
        }
        if (period.getStatus() != PayrollStatus.PENDING_APPROVAL) {
            throw new BadRequestException("Chỉ duyệt được kỳ đang CHỜ DUYỆT — hãy trình duyệt trước");
        }
        period.setStatus(PayrollStatus.APPROVED);
        period.setApprovedBy(SecurityUtils.currentUserId());
        period.setApprovedAt(LocalDateTime.now());
        periodRepository.save(period);
        BigDecimal totalNet = nz(payslipRepository.sumNetByPeriodId(periodId));
        long count = payslipRepository.countByPeriodId(periodId);
        auditService.log("APPROVE_PAYROLL", "PAYROLL_PERIOD", periodId,
                "Duyệt bảng lương " + period.getPeriodMonth() + " — " + period.getStore().getName()
                        + " (" + count + " nhân viên, tổng thực lĩnh " + totalNet + "đ)");
        notifyTelegram(period, "✅ <b>Bảng lương đã được DUYỆT</b>", count, totalNet);
        return PayrollPeriodResponse.detail(period, payslipsOf(periodId));
    }

    /** TRẢ LẠI: PENDING_APPROVAL → DRAFT (người duyệt không đồng ý) — chỉ ADMIN. Ghi lý do vào note. */
    @Transactional
    public PayrollPeriodResponse reject(Long periodId, String reason) {
        PayrollPeriod period = getPeriodOrThrow(periodId);
        StoreContext.assertSameStore(period.getStore().getId());
        if (!SecurityUtils.isAdmin()) {
            throw new BadRequestException("Chỉ quản trị viên mới được trả lại bảng lương");
        }
        if (period.getStatus() != PayrollStatus.PENDING_APPROVAL) {
            throw new BadRequestException("Chỉ trả lại được kỳ đang CHỜ DUYỆT");
        }
        period.setStatus(PayrollStatus.DRAFT);
        period.setSubmittedBy(null);
        period.setSubmittedAt(null);
        period.setNote(reason);
        periodRepository.save(period);
        auditService.log("REJECT_PAYROLL", "PAYROLL_PERIOD", periodId,
                "Trả lại bảng lương " + period.getPeriodMonth() + " — " + period.getStore().getName()
                        + (reason != null ? " (lý do: " + reason + ")" : ""));
        return PayrollPeriodResponse.detail(period, payslipsOf(periodId));
    }

    /** CHI LƯƠNG: APPROVED → PAID. Phát thông báo Telegram. */
    @Transactional
    public PayrollPeriodResponse markPaid(Long periodId) {
        PayrollPeriod period = getPeriodOrThrow(periodId);
        StoreContext.assertSameStore(period.getStore().getId());
        if (period.getStatus() != PayrollStatus.APPROVED) {
            throw new BadRequestException("Chỉ chi lương cho kỳ đã DUYỆT — hãy duyệt kỳ trước");
        }
        period.setStatus(PayrollStatus.PAID);
        period.setPaidAt(LocalDateTime.now());
        periodRepository.save(period);
        BigDecimal totalNet = nz(payslipRepository.sumNetByPeriodId(periodId));
        long count = payslipRepository.countByPeriodId(periodId);
        auditService.log("PAY_PAYROLL", "PAYROLL_PERIOD", periodId,
                "Chi lương kỳ " + period.getPeriodMonth() + " — " + period.getStore().getName()
                        + ", tổng thực lĩnh " + totalNet + "đ");
        notifyTelegram(period, "💸 <b>Đã CHI lương</b>", count, totalNet);
        return PayrollPeriodResponse.detail(period, payslipsOf(periodId));
    }

    /** Phát thông báo Telegram tới chi nhánh (không chặn nghiệp vụ nếu Telegram lỗi). */
    private void notifyTelegram(PayrollPeriod period, String headline, long count, BigDecimal totalNet) {
        try {
            String msg = headline + "\n"
                    + "🏬 Chi nhánh: " + period.getStore().getName() + "\n"
                    + "📅 Kỳ lương: tháng " + period.getPeriodMonth() + "\n"
                    + "👥 Số nhân viên: " + count + "\n"
                    + "💰 Tổng thực lĩnh: " + new java.text.DecimalFormat("#,###").format(totalNet) + "đ";
            telegramService.broadcast(period.getStore().getId(), msg);
        } catch (Exception e) {
            // Thông báo là phụ trợ — lỗi gửi không được làm hỏng giao dịch duyệt/chi lương.
        }
    }

    // =====================================================================
    //  THƯỞNG / PHẠT
    // =====================================================================

    /** Thêm dòng thưởng/phạt vào phiếu lương (chỉ khi kỳ còn DRAFT). */
    @Transactional
    public PayslipResponse addAdjustment(Long payslipId, PayslipAdjustmentRequest req) {
        Payslip slip = payslipRepository.findById(payslipId)
                .orElseThrow(() -> NotFoundException.of("phiếu lương", payslipId));
        StoreContext.assertSameStore(slip.getPeriod().getStore().getId());
        if (slip.getPeriod().getStatus() != PayrollStatus.DRAFT) {
            throw new BadRequestException("Kỳ lương đã khóa — không thể thêm thưởng/phạt");
        }
        PayslipAdjustment adj = new PayslipAdjustment();
        adj.setPayslip(slip);
        adj.setType(req.type());
        adj.setAmount(req.amount());
        adj.setReason(req.reason());
        adj.setCreatedBy(SecurityUtils.currentUserId());
        adjustmentRepository.save(adj);
        recomputeTotals(slip);
        payslipRepository.save(slip);
        auditService.log("ADD_PAYSLIP_ADJUSTMENT", "PAYSLIP", payslipId,
                (req.type() == PayslipAdjustmentType.BONUS ? "Thưởng " : "Trừ ") + req.amount()
                        + "đ cho " + slip.getUser().getFullName() + " — " + req.reason());
        return toPayslipResponse(slip);
    }

    /** Xóa một dòng thưởng/phạt (chỉ khi kỳ còn DRAFT). */
    @Transactional
    public PayslipResponse removeAdjustment(Long adjustmentId) {
        PayslipAdjustment adj = adjustmentRepository.findById(adjustmentId)
                .orElseThrow(() -> NotFoundException.of("dòng điều chỉnh", adjustmentId));
        Payslip slip = adj.getPayslip();
        StoreContext.assertSameStore(slip.getPeriod().getStore().getId());
        if (slip.getPeriod().getStatus() != PayrollStatus.DRAFT) {
            throw new BadRequestException("Kỳ lương đã khóa — không thể xóa thưởng/phạt");
        }
        adjustmentRepository.delete(adj);
        recomputeTotals(slip);
        payslipRepository.save(slip);
        auditService.log("REMOVE_PAYSLIP_ADJUSTMENT", "PAYSLIP", slip.getId(),
                "Xóa điều chỉnh #" + adjustmentId + " của " + slip.getUser().getFullName());
        return toPayslipResponse(slip);
    }

    // =====================================================================
    //  PHIẾU LƯƠNG CỦA TÔI
    // =====================================================================

    /** Phiếu lương của chính người đăng nhập (chỉ kỳ đã khóa/đã chi). */
    public List<PayslipResponse> myPayslips() {
        Long userId = SecurityUtils.currentUserId();
        List<Payslip> slips = payslipRepository.findVisibleForEmployee(
                userId, List.of(PayrollStatus.APPROVED, PayrollStatus.PAID));
        return slips.stream().map(this::toPayslipResponse).toList();
    }

    // =====================================================================
    //  CHẤM CÔNG THỦ CÔNG & NGHỈ PHÉP
    // =====================================================================

    /** Bản ghi chấm công của chi nhánh trong tháng {@code 'YYYY-MM'} (mới nhất trước). */
    public List<AttendanceEntryResponse> listAttendance(String month) {
        YearMonth ym = parseMonth(month);
        Long storeId = StoreContext.requireStoreId();
        return attendanceRepository.findByStore_IdAndWorkDateBetweenOrderByWorkDateDescIdDesc(
                        storeId, ym.atDay(1), ym.atEndOfMonth())
                .stream().map(AttendanceEntryResponse::from).toList();
    }

    /** Thêm bản ghi chấm công thủ công / nghỉ phép cho một nhân viên. */
    @Transactional
    public AttendanceEntryResponse addAttendance(AttendanceEntryRequest req) {
        User user = userRepository.findById(req.userId())
                .orElseThrow(() -> NotFoundException.of("tài khoản", req.userId()));
        assertCanManageEmployee(user);
        if (user.getStore() == null) {
            throw new BadRequestException("Nhân viên không thuộc chi nhánh nào — không thể chấm công");
        }
        AttendanceEntry e = new AttendanceEntry();
        e.setUser(user);
        e.setStore(user.getStore());
        e.setWorkDate(req.workDate());
        e.setType(req.type());
        e.setHours(req.hours());
        e.setReason(req.reason());
        e.setCreatedBy(SecurityUtils.currentUserId());
        AttendanceEntry saved = attendanceRepository.save(e);
        auditService.log("ADD_ATTENDANCE", "USER", user.getId(),
                "Chấm công " + user.getFullName() + " ngày " + req.workDate() + ": "
                        + req.type() + " " + req.hours() + "h"
                        + (req.reason() != null ? " (" + req.reason() + ")" : ""));
        return AttendanceEntryResponse.from(saved);
    }

    /** Xóa một bản ghi chấm công. */
    @Transactional
    public void removeAttendance(Long id) {
        AttendanceEntry e = attendanceRepository.findById(id)
                .orElseThrow(() -> NotFoundException.of("bản ghi chấm công", id));
        StoreContext.assertSameStore(e.getStore().getId());
        assertCanManageEmployee(e.getUser());
        attendanceRepository.delete(e);
        auditService.log("REMOVE_ATTENDANCE", "USER", e.getUser().getId(),
                "Xóa chấm công #" + id + " của " + e.getUser().getFullName());
    }

    // =====================================================================
    //  TÍNH TOÁN
    // =====================================================================

    /** Tính lương gốc/tăng ca từ giờ công + cấu hình → ghi snapshot vào phiếu (chưa cộng thưởng/phạt). */
    private void applyPay(Payslip slip, EmployeePayProfile profile, BigDecimal worked, int shiftCount) {
        PayType payType = profile != null ? profile.getPayType() : PayType.MONTHLY;
        BigDecimal baseRate = profile != null ? profile.getBaseRate() : BigDecimal.ZERO;
        BigDecimal standard = profile != null ? profile.getStandardMonthlyHours() : DEFAULT_STANDARD_HOURS;
        BigDecimal otMult = profile != null ? profile.getOtMultiplier() : DEFAULT_OT_MULTIPLIER;
        BigDecimal allowance = profile != null ? profile.getMonthlyAllowance() : BigDecimal.ZERO;

        BigDecimal workedH = worked.setScale(2, RoundingMode.HALF_UP);
        BigDecimal regularH = workedH.min(standard);
        BigDecimal otH = workedH.subtract(standard).max(BigDecimal.ZERO);

        BigDecimal regularPay;
        BigDecimal otPay;
        if (payType == PayType.HOURLY) {
            regularPay = regularH.multiply(baseRate).setScale(0, RoundingMode.HALF_UP);
            otPay = otH.multiply(baseRate).multiply(otMult).setScale(0, RoundingMode.HALF_UP);
        } else {
            // MONTHLY: trả theo tỷ lệ công; tăng ca theo đơn giá giờ quy đổi = baseRate / standard.
            regularPay = baseRate.multiply(regularH).divide(standard, 0, RoundingMode.HALF_UP);
            otPay = baseRate.multiply(otH).multiply(otMult).divide(standard, 0, RoundingMode.HALF_UP);
        }
        BigDecimal gross = regularPay.add(otPay).add(allowance);

        slip.setPayType(payType);
        slip.setBaseRate(baseRate);
        slip.setStandardHours(standard);
        slip.setWorkedHours(workedH);
        slip.setRegularHours(regularH);
        slip.setOtHours(otH);
        slip.setShiftCount(shiftCount);
        slip.setRegularPay(regularPay);
        slip.setOtPay(otPay);
        slip.setAllowance(allowance.setScale(0, RoundingMode.HALF_UP));
        slip.setGrossPay(gross.setScale(0, RoundingMode.HALF_UP));
    }

    /** Cộng lại tổng thưởng/phạt và thực lĩnh từ các dòng điều chỉnh hiện có. */
    private void recomputeTotals(Payslip slip) {
        BigDecimal bonus = BigDecimal.ZERO;
        BigDecimal deduction = BigDecimal.ZERO;
        for (PayslipAdjustment a : adjustmentRepository.findByPayslipIdOrderByCreatedAt(slip.getId())) {
            if (a.getType() == PayslipAdjustmentType.BONUS) bonus = bonus.add(a.getAmount());
            else deduction = deduction.add(a.getAmount());
        }
        slip.setTotalBonus(bonus);
        slip.setTotalDeduction(deduction);
        slip.setNetPay(slip.getGrossPay().add(bonus).subtract(deduction));
    }

    // =====================================================================
    //  HỖ TRỢ
    // =====================================================================

    /** Nhân viên trong phạm vi: MANAGER → STAFF của cửa hàng mình; ADMIN → cửa hàng đang chọn (null = toàn chuỗi). */
    private List<User> employeesInScope() {
        if (!SecurityUtils.isAdmin()) {
            return userRepository.findByStore_IdAndRoleOrderByIdAsc(
                    SecurityUtils.currentUser().getStoreId(), Role.STAFF);
        }
        Long storeId = StoreContext.currentStoreId();
        List<User> users = storeId != null
                ? userRepository.findByStore_IdOrderByIdAsc(storeId)
                : userRepository.findAll();
        // Bỏ tài khoản không gắn cửa hàng (ADMIN toàn chuỗi) — họ không có ca/lương.
        return users.stream().filter(u -> u.getStore() != null).toList();
    }

    /** Chốt phạm vi thao tác lên một nhân viên (giống UserService): MANAGER chỉ STAFF cửa hàng mình. */
    private void assertCanManageEmployee(User target) {
        if (SecurityUtils.isAdmin()) {
            StoreContext.assertSameStore(target.getStore() != null ? target.getStore().getId() : null);
            return;
        }
        Long myStore = SecurityUtils.currentUser().getStoreId();
        boolean sameStore = target.getStore() != null && target.getStore().getId().equals(myStore);
        if (target.getRole() != Role.STAFF || !sameStore) {
            throw new BadRequestException("Không có quyền cấu hình lương cho tài khoản này");
        }
    }

    private List<PayslipResponse> payslipsOf(Long periodId) {
        List<Payslip> slips = payslipRepository.findByPeriodIdOrderByUserId(periodId);
        if (slips.isEmpty()) return List.of();
        Map<Long, List<PayslipAdjustmentResponse>> adjBySlip = adjustmentRepository
                .findByPayslipIdInOrderByCreatedAt(slips.stream().map(Payslip::getId).toList())
                .stream()
                .collect(Collectors.groupingBy(a -> a.getPayslip().getId(),
                        Collectors.mapping(PayslipAdjustmentResponse::from, Collectors.toList())));
        return slips.stream()
                .map(s -> PayslipResponse.from(s, s.getUser().getFullName(),
                        adjBySlip.getOrDefault(s.getId(), List.of())))
                .toList();
    }

    private PayslipResponse toPayslipResponse(Payslip slip) {
        List<PayslipAdjustmentResponse> adjs = adjustmentRepository
                .findByPayslipIdOrderByCreatedAt(slip.getId())
                .stream().map(PayslipAdjustmentResponse::from).toList();
        return PayslipResponse.from(slip, slip.getUser().getFullName(), adjs);
    }

    private PayrollPeriod getPeriodOrThrow(Long id) {
        return periodRepository.findById(id)
                .orElseThrow(() -> NotFoundException.of("kỳ lương", id));
    }

    private static YearMonth parseMonth(String month) {
        try {
            return YearMonth.parse(month);   // 'YYYY-MM'
        } catch (Exception e) {
            throw new BadRequestException("Tháng lương không hợp lệ (cần dạng YYYY-MM): " + month);
        }
    }

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
