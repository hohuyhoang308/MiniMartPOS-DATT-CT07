package com.pos.repository;

import com.pos.entity.WorkShift;
import com.pos.entity.enums.ShiftStatus;
import com.pos.repository.projection.EmployeeCashRow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface WorkShiftRepository extends JpaRepository<WorkShift, Long> {

    /** Ca đang mở của một thu ngân (mỗi thu ngân chỉ nên có 1 ca OPEN). */
    Optional<WorkShift> findFirstByUserIdAndStatusOrderByOpenedAtDesc(Long userId, ShiftStatus status);

    boolean existsByUserIdAndStatus(Long userId, ShiftStatus status);

    /** 200 ca gần nhất (mới nhất trước) — màn Quản lý ca; giới hạn ở DB để không tải vô hạn. */
    List<WorkShift> findTop200ByOrderByOpenedAtDesc();

    /** 200 ca gần nhất của MỘT chi nhánh (đa chuỗi) — quản lý chỉ thấy ca chi nhánh mình. */
    List<WorkShift> findTop200ByStoreIdOrderByOpenedAtDesc(Long storeId);

    /** Ca đã đóng gần nhất — để gợi ý tiền đầu ca (= tiền cuối ca trước, két chuyển tiếp). */
    Optional<WorkShift> findFirstByStatusOrderByClosedAtDesc(ShiftStatus status);

    /** Ca đã đóng gần nhất CỦA MỘT CỬA HÀNG — gợi ý tiền đầu ca đúng két của cửa hàng (đa cửa hàng). */
    Optional<WorkShift> findFirstByStoreIdAndStatusOrderByClosedAtDesc(Long storeId, ShiftStatus status);

    /**
     * TRÁCH NHIỆM QUỸ theo THU NGÂN trong khoảng (mở ca trong [from,to)): tổng lệch quỹ các ca ĐÃ ĐÓNG
     * = Σ (đếm thực − [đầu ca + tiền mặt bán + ròng thu/chi]) — khớp đúng công thức đối soát ở trang Ca.
     * Lọc chi nhánh (null = toàn chuỗi). Dùng cho báo cáo hiệu suất nhân viên.
     */
    @Query(value = """
            SELECT s.user_id AS userId,
                   SUM(CASE WHEN s.status = 'CLOSED'
                            THEN s.closing_cash - s.opening_cash - COALESCE(s.final_cash_sales, cs.cash, 0) - COALESCE(cm.net,0)
                            ELSE 0 END) AS variance,
                   SUM(CASE WHEN s.status = 'CLOSED' THEN 1 ELSE 0 END) AS closedShifts,
                   COUNT(*) AS totalShifts
            FROM work_shifts s
            LEFT JOIN ( SELECT shift_id, SUM(total_amount) AS cash FROM invoices
                        WHERE status = 'COMPLETED' AND payment_method = 'CASH' GROUP BY shift_id ) cs
                   ON cs.shift_id = s.id
            LEFT JOIN ( SELECT shift_id, SUM(CASE WHEN type = 'IN' THEN amount ELSE -amount END) AS net
                        FROM cash_movements GROUP BY shift_id ) cm
                   ON cm.shift_id = s.id
            WHERE s.opened_at >= :from AND s.opened_at < :to
              AND (:storeId IS NULL OR s.store_id = :storeId)
            GROUP BY s.user_id
            """, nativeQuery = true)
    List<EmployeeCashRow> cashAccountabilityByCashier(@Param("from") LocalDateTime from,
                                                      @Param("to") LocalDateTime to,
                                                      @Param("storeId") Long storeId);
}
