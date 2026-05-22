package com.pos.service;

import com.pos.dto.shift.OpenShiftRequest;
import com.pos.dto.shift.ShiftResponse;
import com.pos.entity.User;
import com.pos.entity.WorkShift;
import com.pos.entity.enums.ShiftStatus;
import com.pos.exception.BadRequestException;
import com.pos.exception.NotFoundException;
import com.pos.repository.UserRepository;
import com.pos.repository.WorkShiftRepository;
import com.pos.repository.view.ShiftSummaryViewRepository;
import com.pos.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Ca làm việc (FR4.1 - UC08). */
@Service
@Transactional(readOnly = true)
public class ShiftService {

    private final WorkShiftRepository shiftRepository;
    private final UserRepository userRepository;
    private final ShiftSummaryViewRepository summaryRepository;

    public ShiftService(WorkShiftRepository shiftRepository,
                        UserRepository userRepository,
                        ShiftSummaryViewRepository summaryRepository) {
        this.shiftRepository = shiftRepository;
        this.userRepository = userRepository;
        this.summaryRepository = summaryRepository;
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

    /** Đóng ca: chỉ chủ ca mới đóng được ca của mình. */
    @Transactional
    public ShiftResponse close(Long shiftId, BigDecimal closingCash) {
        WorkShift shift = shiftRepository.findById(shiftId)
                .orElseThrow(() -> NotFoundException.of("ca làm việc", shiftId));
        if (!shift.getUser().getId().equals(SecurityUtils.currentUserId())) {
            throw new BadRequestException("Bạn chỉ có thể đóng ca của chính mình");
        }
        if (shift.getStatus() == ShiftStatus.CLOSED) {
            throw new BadRequestException("Ca này đã được đóng");
        }
        shift.setClosingCash(closingCash);
        shift.setClosedAt(LocalDateTime.now());
        shift.setStatus(ShiftStatus.CLOSED);
        return toResponse(shiftRepository.save(shift));
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
        return ShiftResponse.from(
                shift, shift.getUser().getFullName(),
                summary.map(s -> s.getTotalSales()).orElse(BigDecimal.ZERO),
                summary.map(s -> s.getInvoiceCount()).orElse(0L));
    }
}
