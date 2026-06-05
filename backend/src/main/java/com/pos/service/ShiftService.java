package com.pos.service;

import com.pos.dto.shift.OpenShiftRequest;
import com.pos.dto.shift.ShiftResponse;
import com.pos.entity.User;
import com.pos.entity.WorkShift;
import com.pos.entity.enums.ShiftStatus;
import com.pos.exception.BadRequestException;
import com.pos.exception.NotFoundException;
import com.pos.repository.InvoiceRepository;
import com.pos.repository.UserRepository;
import com.pos.repository.WorkShiftRepository;
import com.pos.repository.view.ShiftSummaryViewRepository;
import com.pos.security.CustomUserDetails;
import com.pos.security.SecurityUtils;
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
    private final ShiftSummaryViewRepository summaryRepository;
    private final InvoiceRepository invoiceRepository;
    private final com.pos.repository.SalesReturnRepository returnRepository;
    private final AuditService auditService;

    public ShiftService(WorkShiftRepository shiftRepository,
                        UserRepository userRepository,
                        ShiftSummaryViewRepository summaryRepository,
                        InvoiceRepository invoiceRepository,
                        com.pos.repository.SalesReturnRepository returnRepository,
                        AuditService auditService) {
        this.shiftRepository = shiftRepository;
        this.userRepository = userRepository;
        this.summaryRepository = summaryRepository;
        this.invoiceRepository = invoiceRepository;
        this.returnRepository = returnRepository;
        this.auditService = auditService;
    }

    /** Mở ca cho thu ngân đang đăng nhập (nếu chưa có ca OPEN). */
    @Transactional
    public ShiftResponse open(OpenShiftRequest req) {
        Long userId = SecurityUtils.currentUserId();
        if (shiftRepository.existsByUserIdAndStatus(userId, ShiftStatus.OPEN)) {
            throw new BadRequestException("Bạn đang có một ca chưa đóng — vui lòng đóng ca trước khi mở ca mới");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> NotFoundException.of("tài khoản", userId));
        WorkShift shift = new WorkShift();
        shift.setUser(user);
        shift.setOpeningCash(req.openingCash());
        shift.setStatus(ShiftStatus.OPEN);
        return toResponse(shiftRepository.save(shift));
    }

    /** 200 ca gần nhất (mới nhất trước) — màn Quản lý ca. */
    public List<ShiftResponse> listAll() {
        return shiftRepository.findTop200ByOrderByOpenedAtDesc().stream().map(this::toResponse).toList();
    }

    /**
     * Gợi ý tiền đầu ca cho ca sắp mở = tiền cuối ca của ca đóng gần nhất (két chuyển tiếp).
     * Chưa có ca nào đóng → 0 (thu ngân tự nhập lần đầu).
     */
    public BigDecimal suggestedOpeningCash() {
        return shiftRepository.findFirstByStatusOrderByClosedAtDesc(ShiftStatus.CLOSED)
                .map(WorkShift::getClosingCash)
                .filter(java.util.Objects::nonNull)
                .orElse(BigDecimal.ZERO);
    }

    /** Đóng ca: chủ ca tự đóng, HOẶC quản lý/chủ cửa hàng đóng hộ ca bất kỳ. */
    @Transactional
    public ShiftResponse close(Long shiftId, BigDecimal closingCash) {
        WorkShift shift = shiftRepository.findById(shiftId)
                .orElseThrow(() -> NotFoundException.of("ca làm việc", shiftId));
        CustomUserDetails me = SecurityUtils.currentUser();
        boolean isManager = "ADMIN".equals(me.getRole()) || "MANAGER".equals(me.getRole());
        if (!isManager && !shift.getUser().getId().equals(me.getId())) {
            throw new BadRequestException("Bạn chỉ có thể đóng ca của chính mình");
        }
        if (shift.getStatus() == ShiftStatus.CLOSED) {
            throw new BadRequestException("Ca này đã được đóng");
        }
        shift.setClosingCash(closingCash);
        shift.setClosedAt(LocalDateTime.now());
        shift.setStatus(ShiftStatus.CLOSED);
        ShiftResponse resp = toResponse(shiftRepository.save(shift));
        auditService.log("CLOSE_SHIFT", "SHIFT", shift.getId(),
                "Đóng ca #" + shift.getId() + " (" + shift.getUser().getFullName() + "), đếm "
                        + closingCash + "đ, chênh lệch quỹ " + resp.cashDifference() + "đ"
                        + (isManager && !shift.getUser().getId().equals(me.getId()) ? " [đóng hộ]" : ""));
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
        return toResponse(shiftRepository.findById(id)
                .orElseThrow(() -> NotFoundException.of("ca làm việc", id)));
    }

    private ShiftResponse toResponse(WorkShift shift) {
        var summary = summaryRepository.findByShiftId(shift.getId());
        BigDecimal cashSales = invoiceRepository.sumCashSalesByShift(shift.getId());
        // Tiền hoàn trả hàng phát sinh trong ca (theo khoảng thời gian ca) — chi ra khỏi két.
        LocalDateTime from = shift.getOpenedAt();
        LocalDateTime to = shift.getClosedAt() != null ? shift.getClosedAt() : LocalDateTime.now();
        BigDecimal cashRefunds = (from != null) ? returnRepository.sumRefundBetween(from, to) : BigDecimal.ZERO;
        return ShiftResponse.from(
                shift, shift.getUser().getFullName(),
                summary.map(s -> s.getTotalSales()).orElse(BigDecimal.ZERO),
                summary.map(s -> s.getInvoiceCount()).orElse(0L),
                cashSales, cashRefunds);
    }
}
