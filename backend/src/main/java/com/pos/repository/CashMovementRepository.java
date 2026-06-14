package com.pos.repository;

import com.pos.entity.CashMovement;
import com.pos.entity.enums.CashMovementType;
import com.pos.repository.projection.ShiftCashRow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface CashMovementRepository extends JpaRepository<CashMovement, Long> {

    /** Các khoản thu/chi của một ca (mới nhất trước) — hiển thị khi đối soát/đóng ca. */
    List<CashMovement> findByShift_IdOrderByIdDesc(Long shiftId);

    /** Tổng tiền THU hoặc CHI của một ca — để tính tiền mặt dự kiến trong két. */
    @Query("SELECT COALESCE(SUM(c.amount), 0) FROM CashMovement c WHERE c.shift.id = :shiftId AND c.type = :type")
    BigDecimal sumByShiftAndType(@Param("shiftId") Long shiftId, @Param("type") CashMovementType type);

    /** RÒNG (THU − CHI) của MỘT ca — cộng vào tiền mặt dự kiến cuối ca. */
    @Query("""
            SELECT COALESCE(SUM(CASE WHEN c.type = com.pos.entity.enums.CashMovementType.IN
                                     THEN c.amount ELSE -c.amount END), 0)
            FROM CashMovement c WHERE c.shift.id = :shiftId
            """)
    BigDecimal netByShift(@Param("shiftId") Long shiftId);

    /** RÒNG (THU − CHI) gộp theo NHIỀU ca cùng lúc — bỏ N+1 ở báo cáo ca. */
    @Query("""
            SELECT c.shift.id AS shiftId,
                   COALESCE(SUM(CASE WHEN c.type = com.pos.entity.enums.CashMovementType.IN
                                     THEN c.amount ELSE -c.amount END), 0) AS amount
            FROM CashMovement c WHERE c.shift.id IN :shiftIds GROUP BY c.shift.id
            """)
    List<ShiftCashRow> netByShiftIds(@Param("shiftIds") List<Long> shiftIds);
}
