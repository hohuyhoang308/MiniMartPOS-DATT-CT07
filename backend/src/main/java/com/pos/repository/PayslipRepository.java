package com.pos.repository;

import com.pos.entity.Payslip;
import com.pos.entity.enums.PayrollStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface PayslipRepository extends JpaRepository<Payslip, Long> {

    List<Payslip> findByPeriodIdOrderByUserId(Long periodId);

    Optional<Payslip> findByPeriodIdAndUserId(Long periodId, Long userId);

    void deleteByPeriodId(Long periodId);

    long countByPeriodId(Long periodId);

    @Query("SELECT COALESCE(SUM(p.netPay), 0) FROM Payslip p WHERE p.period.id = :periodId")
    BigDecimal sumNetByPeriodId(@Param("periodId") Long periodId);

    /** Tổng quỹ lương (Σ thực lĩnh) của một THÁNG, lọc chi nhánh (null = toàn chuỗi) — cho dashboard. */
    @Query("""
            SELECT COALESCE(SUM(p.netPay), 0) FROM Payslip p
            WHERE p.period.periodMonth = :month AND (:storeId IS NULL OR p.period.store.id = :storeId)
            """)
    BigDecimal sumNetByMonth(@Param("month") String month, @Param("storeId") Long storeId);

    /** Phiếu lương của một nhân viên ở các kỳ đã KHÓA/ĐÃ CHI (cho "Phiếu lương của tôi"). */
    @Query("""
            SELECT p FROM Payslip p
            WHERE p.user.id = :userId AND p.period.status IN :statuses
            ORDER BY p.period.periodMonth DESC
            """)
    List<Payslip> findVisibleForEmployee(@Param("userId") Long userId,
                                         @Param("statuses") List<PayrollStatus> statuses);
}
