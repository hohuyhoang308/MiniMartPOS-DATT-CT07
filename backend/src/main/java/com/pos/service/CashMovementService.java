package com.pos.service;

import com.pos.dto.shift.CashMovementRequest;
import com.pos.dto.shift.CashMovementResponse;
import com.pos.entity.CashMovement;
import com.pos.entity.WorkShift;
import com.pos.entity.enums.CashMovementType;
import com.pos.entity.enums.ShiftStatus;
import com.pos.exception.BadRequestException;
import com.pos.exception.NotFoundException;
import com.pos.repository.CashMovementRepository;
import com.pos.repository.UserRepository;
import com.pos.repository.WorkShiftRepository;
import com.pos.security.SecurityUtils;
import com.pos.security.StoreContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * THU/CHI tiền mặt NGOÀI bán hàng trong ca (petty cash). Hoàn thiện câu chuyện quản lý quỹ: trước đây
 * đối soát cuối ca chỉ tính tiền đầu ca + tiền mặt bán, nên mọi khoản chi NCC / đổi tiền lẻ / rút bớt
 * đều biến thành "lệch quỹ" không giải thích được. Mỗi khoản ghi vào CA ĐANG MỞ của thu ngân.
 */
@Service
@Transactional(readOnly = true)
public class CashMovementService {

    private final CashMovementRepository repository;
    private final WorkShiftRepository shiftRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    public CashMovementService(CashMovementRepository repository,
                               WorkShiftRepository shiftRepository,
                               UserRepository userRepository,
                               AuditService auditService) {
        this.repository = repository;
        this.shiftRepository = shiftRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    /** Các khoản thu/chi của một ca (đối soát/đóng ca). Chặn xem chéo chi nhánh. */
    public List<CashMovementResponse> byShift(Long shiftId) {
        WorkShift shift = shiftRepository.findById(shiftId)
                .orElseThrow(() -> NotFoundException.of("ca làm việc", shiftId));
        StoreContext.assertSameStore(shift.getStore().getId());
        return repository.findByShift_IdOrderByIdDesc(shiftId).stream()
                .map(CashMovementResponse::from).toList();
    }

    /** Ghi một khoản thu/chi vào CA ĐANG MỞ của thu ngân hiện tại. */
    @Transactional
    public CashMovementResponse create(CashMovementRequest req) {
        Long userId = SecurityUtils.currentUserId();
        WorkShift shift = shiftRepository.findFirstByUserIdAndStatusOrderByOpenedAtDesc(userId, ShiftStatus.OPEN)
                .orElseThrow(() -> new BadRequestException(
                        "Bạn chưa mở ca làm việc — hãy mở ca trước khi ghi thu/chi quỹ"));

        CashMovement m = new CashMovement();
        m.setShift(shift);
        m.setStore(shift.getStore());
        m.setType(req.type());
        m.setAmount(req.amount());
        m.setReason(req.reason().trim());
        m.setCreatedBy(userRepository.findById(userId).orElse(null));
        CashMovement saved = repository.save(m);

        String dir = req.type() == CashMovementType.IN ? "THU" : "CHI";
        auditService.log("CASH_MOVEMENT", "SHIFT", shift.getId(),
                dir + " quỹ " + req.amount() + "đ (ca #" + shift.getId() + ") — " + req.reason().trim());
        return CashMovementResponse.from(saved);
    }
}
