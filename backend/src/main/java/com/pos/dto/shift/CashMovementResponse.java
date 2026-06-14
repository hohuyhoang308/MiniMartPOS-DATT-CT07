package com.pos.dto.shift;

import com.pos.entity.CashMovement;
import com.pos.entity.enums.CashMovementType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Một dòng thu/chi quỹ tiền mặt của ca. */
public record CashMovementResponse(
        Long id,
        Long shiftId,
        CashMovementType type,
        BigDecimal amount,
        String reason,
        String createdByName,
        LocalDateTime createdAt
) {
    public static CashMovementResponse from(CashMovement c) {
        return new CashMovementResponse(
                c.getId(),
                c.getShift().getId(),
                c.getType(),
                c.getAmount(),
                c.getReason(),
                c.getCreatedBy() != null ? c.getCreatedBy().getFullName() : null,
                c.getCreatedAt());
    }
}
