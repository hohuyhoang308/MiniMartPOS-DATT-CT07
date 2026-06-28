package com.pos.dto.payroll;

import com.pos.entity.EmployeePayProfile;
import com.pos.entity.User;
import com.pos.entity.enums.PayType;

import java.math.BigDecimal;

/** Cấu hình lương của một nhân viên (kèm thông tin nhân viên để hiển thị bảng). */
public record PayProfileResponse(
        Long userId, String fullName, String username,
        String role, Long storeId, String storeName,
        PayType payType, BigDecimal baseRate, BigDecimal standardMonthlyHours,
        BigDecimal otMultiplier, BigDecimal monthlyAllowance, boolean configured) {

    /** Nhân viên CHƯA có cấu hình → trả mặc định (lương 0) để bảng vẫn hiển thị, configured=false. */
    public static PayProfileResponse forUser(User u, EmployeePayProfile p) {
        boolean has = p != null;
        return new PayProfileResponse(
                u.getId(), u.getFullName(), u.getUsername(),
                u.getRole().name(),
                u.getStore() != null ? u.getStore().getId() : null,
                u.getStore() != null ? u.getStore().getName() : null,
                has ? p.getPayType() : PayType.MONTHLY,
                has ? p.getBaseRate() : BigDecimal.ZERO,
                has ? p.getStandardMonthlyHours() : new BigDecimal("208"),
                has ? p.getOtMultiplier() : new BigDecimal("1.5"),
                has ? p.getMonthlyAllowance() : BigDecimal.ZERO,
                has);
    }
}
