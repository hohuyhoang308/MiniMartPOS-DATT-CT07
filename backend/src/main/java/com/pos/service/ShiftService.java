package com.pos.service;

import com.pos.dto.shift.OpenShiftRequest;
import com.pos.dto.shift.ShiftResponse;
import com.pos.entity.Store;
import com.pos.entity.User;
import com.pos.entity.WorkShift;
import com.pos.entity.enums.ShiftStatus;
import com.pos.exception.BadRequestException;
import com.pos.exception.NotFoundException;
import com.pos.entity.enums.CashMovementType;
import com.pos.repository.CashMovementRepository;
import com.pos.repository.InvoiceRepository;
import com.pos.repository.StoreRepository;
import com.pos.repository.UserRepository;
import com.pos.repository.WorkShiftRepository;
import com.pos.repository.view.ShiftSummaryViewRepository;
import com.pos.security.CustomUserDetails;
import com.pos.security.SecurityUtils;
import com.pos.security.StoreContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Ca làm việc (FR4.1 - UC08). */
@Service
@Transactional(readOnly = true)
public class ShiftService {

    private final WorkShiftRepository shiftRepository;
    private final UserRepository userRepository;
    private final StoreRepository storeRepository;
    private final ShiftSummaryViewRepository summaryRepository;
    private final InvoiceRepository invoiceRepository;
    private final CashMovementRepository cashMovementRepository;
    private final AuditService auditService;

    public ShiftService(WorkShiftRepository shiftRepository,
                        UserRepository userRepository,
                        StoreRepository storeRepository,
                        ShiftSummaryViewRepository summaryRepository,
                        InvoiceRepository invoiceRepository,
                        CashMovementRepository cashMovementRepository,
                        AuditService auditService) {
        this.shiftRepository = shiftRepository;
        this.userRepository = userRepository;
        this.storeRepository = storeRepository;
        this.summaryRepository = summaryRepository;
        this.invoiceRepository = invoiceRepository;
        this.cashMovementRepository = cashMovementRepository;
        this.auditService = auditService;
    }

    /** Mở ca cho thu ngân đang đăng nhập (nếu chưa có ca OPEN). */
    @Transactional
    public ShiftResponse open(OpenShiftRequest req) {
        Long userId = SecurityUtils.currentUserId();
        // Khóa dòng user TRƯỚC khi kiểm tra: 2 request mở ca đồng thời của cùng người
        // phải tuần tự — request sau thấy ca OPEN vừa commit và bị từ chối (hết race).
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> NotFoundException.of("tài khoản", userId));
        if (shiftRepository.existsByUserIdAndStatus(userId, ShiftStatus.OPEN)) {
            throw new BadRequestException("Bạn đang có một ca chưa đóng — vui lòng đóng ca trước khi mở ca mới");
        }
        // Chi nhánh của ca: người gắn chi nhánh → chi nhánh đó; CHAIN_ADMIN → chi nhánh đang chọn (X-Store-Id).
        Store store = user.getStore() != null ? user.getStore()
                : storeRepository.getReferenceById(StoreContext.requireStoreId());
        WorkShift shift = new WorkShift();
        shift.setStore(store);
        shift.setUser(user);
        shift.setOpeningCash(req.openingCash());
        shift.setStatus(ShiftStatus.OPEN);
        WorkShift saved = shiftRepository.save(shift);
        auditService.log("OPEN_SHIFT", "SHIFT", saved.getId(),
                "Mở ca #" + saved.getId() + " (tiền đầu ca " + saved.getOpeningCash() + "đ)");
        return toResponse(saved);
    }

    /** 200 ca gần nhất (mới nhất trước) — màn Quản lý ca. Lọc theo chi nhánh đang làm việc (null = toàn chuỗi). */
    public List<ShiftResponse> listAll() {
        Long storeId = StoreContext.currentStoreId();
        List<WorkShift> shifts = storeId == null
                ? shiftRepository.findTop200ByOrderByOpenedAtDesc()
                : shiftRepository.findTop200ByStoreIdOrderByOpenedAtDesc(storeId);
        return shifts.stream().map(this::toResponse).toList();
    }

    /**
     * Gợi ý tiền đầu ca cho ca sắp mở = tiền cuối ca của ca đóng gần nhất (két chuyển tiếp).
     * Chưa có ca nào đóng → 0 (thu ngân tự nhập lần đầu).
     */
    public BigDecimal suggestedOpeningCash() {
        // Gợi ý theo két của CHÍNH cửa hàng (đa cửa hàng); ADMIN không gắn cửa hàng → toàn chuỗi.
        Long storeId = StoreContext.currentStoreId();
        var lastClosed = storeId == null
                ? shiftRepository.findFirstByStatusOrderByClosedAtDesc(ShiftStatus.CLOSED)
                : shiftRepository.findFirstByStoreIdAndStatusOrderByClosedAtDesc(storeId, ShiftStatus.CLOSED);
        return lastClosed.map(WorkShift::getClosingCash)
                .filter(java.util.Objects::nonNull)
                .orElse(BigDecimal.ZERO);
    }

    /** Đóng ca: chủ ca tự đóng, HOẶC quản lý/chủ cửa hàng đóng hộ ca bất kỳ. */
    @Transactional
    public ShiftResponse close(Long shiftId, BigDecimal closingCash) {
        WorkShift shift = shiftRepository.findById(shiftId)
                .orElseThrow(() -> NotFoundException.of("ca làm việc", shiftId));
        // Cô lập đa cửa hàng: không cho đóng ca thuộc cửa hàng khác (CHAIN_ADMIN chưa chọn chi nhánh được bỏ qua).
        StoreContext.assertSameStore(shift.getStore().getId());
        CustomUserDetails me = SecurityUtils.currentUser();
        boolean isManager = "ADMIN".equals(me.getRole()) || "MANAGER".equals(me.getRole());
        if (!isManager && !shift.getUser().getId().equals(me.getId())) {
            throw new BadRequestException("Bạn chỉ có thể đóng ca của chính mình");
        }
        if (shift.getStatus() == ShiftStatus.CLOSED) {
            throw new BadRequestException("Ca này đã được đóng");
        }
        // Không đóng ca khi còn hóa đơn QR đang chờ xác nhận tiền — tránh hóa đơn "lơ lửng"
        // ngoài ca và lệch doanh thu đối soát (job nền sẽ tự hủy mã QR quá hạn sau 15 phút).
        long pendingQr = invoiceRepository.countByShiftIdAndStatus(shiftId, com.pos.entity.enums.InvoiceStatus.PENDING_PAYMENT);
        if (pendingQr > 0) {
            throw new BadRequestException("Còn " + pendingQr + " hóa đơn QR đang chờ thanh toán trong ca — "
                    + "vui lòng chờ khách chuyển tiền hoặc để hệ thống tự hủy mã quá hạn trước khi đóng ca");
        }
        shift.setClosingCash(closingCash);
        shift.setClosedAt(LocalDateTime.now());
        shift.setStatus(ShiftStatus.CLOSED);
        ShiftResponse resp = toResponse(shiftRepository.save(shift));
        boolean onBehalf = !shift.getUser().getId().equals(me.getId());
        auditService.log("CLOSE_SHIFT", "SHIFT", shift.getId(),
                "Đóng ca #" + shift.getId() + " của " + shift.getUser().getFullName()
                        + ", đếm tiền cuối ca " + closingCash + "đ, chênh lệch quỹ " + resp.cashDifference() + "đ"
                        + (onBehalf ? " (đóng hộ bởi " + me.getFullName() + ")" : ""));
        return resp;
    }

    /** Ca đang mở của thu ngân hiện tại (null nếu chưa mở ca). */
    public ShiftResponse current() {
        Long userId = SecurityUtils.currentUserId();
        return shiftRepository.findFirstByUserIdAndStatusOrderByOpenedAtDesc(userId, ShiftStatus.OPEN)
                .map(this::toResponse)
                .orElse(null);
    }

    public ShiftResponse findById(Long id) {
        WorkShift shift = shiftRepository.findById(id)
                .orElseThrow(() -> NotFoundException.of("ca làm việc", id));
        StoreContext.assertSameStore(shift.getStore().getId());   // chặn xem ca chéo cửa hàng
        return toResponse(shift);
    }

    private ShiftResponse toResponse(WorkShift shift) {
        var summary = summaryRepository.findByShiftId(shift.getId());
        BigDecimal cashSales = invoiceRepository.sumCashSalesByShift(shift.getId());
        BigDecimal cashIn = cashMovementRepository.sumByShiftAndType(shift.getId(), CashMovementType.IN);
        BigDecimal cashOut = cashMovementRepository.sumByShiftAndType(shift.getId(), CashMovementType.OUT);
        return ShiftResponse.from(
                shift, shift.getUser().getFullName(),
                summary.map(s -> s.getTotalSales()).orElse(BigDecimal.ZERO),
                summary.map(s -> s.getInvoiceCount()).orElse(0L),
                cashSales, cashIn, cashOut);
    }
}
