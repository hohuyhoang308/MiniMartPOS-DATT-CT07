package com.pos.repository;

import com.pos.entity.AttendanceEntry;
import com.pos.repository.projection.AttendanceHoursRow;
import com.pos.repository.projection.AttendanceOverlapRow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface AttendanceEntryRepository extends JpaRepository<AttendanceEntry, Long> {

    /** Bản ghi chấm công của MỘT chi nhánh trong khoảng ngày (màn Bảng công), mới nhất trước. */
    List<AttendanceEntry> findByStore_IdAndWorkDateBetweenOrderByWorkDateDescIdDesc(
            Long storeId, LocalDate from, LocalDate to);

    /**
     * Giờ chấm công cộng dồn theo nhân viên trong [from, to] tại một chi nhánh (cho payroll):
     * paidHours = Σ WORK + LEAVE_PAID; leavePaidHours = Σ LEAVE_PAID. LEAVE_UNPAID không tính lương.
     */
    @Query(value = """
            SELECT a.user_id AS userId,
                   u.full_name AS fullName,
                   COALESCE(SUM(CASE WHEN a.type IN ('WORK','LEAVE_PAID') THEN a.hours ELSE 0 END), 0) AS paidHours,
                   COALESCE(SUM(CASE WHEN a.type = 'LEAVE_PAID' THEN a.hours ELSE 0 END), 0) AS leavePaidHours
            FROM attendance_entries a
            JOIN users u ON u.id = a.user_id
            WHERE a.store_id = :storeId AND a.work_date BETWEEN :from AND :to
            GROUP BY a.user_id, u.full_name
            """, nativeQuery = true)
    List<AttendanceHoursRow> paidHoursByEmployee(@Param("storeId") Long storeId,
                                                 @Param("from") LocalDate from,
                                                 @Param("to") LocalDate to);

    /**
     * Ngày mà một nhân viên VỪA có ca VỪA có chấm công thủ công hưởng lương (WORK/LEAVE_PAID)
     * trong kỳ — nguy cơ tính TRÙNG giờ. Dùng để CẢNH BÁO khi tính lương.
     */
    @Query(value = """
            SELECT u.full_name AS fullName,
                   a.work_date AS workDate,
                   SUM(a.hours) AS hours
            FROM attendance_entries a
            JOIN users u ON u.id = a.user_id
            WHERE a.store_id = :storeId AND a.work_date BETWEEN :from AND :to
              AND a.type IN ('WORK','LEAVE_PAID')
              AND EXISTS (SELECT 1 FROM work_shifts s
                          WHERE s.user_id = a.user_id AND s.store_id = a.store_id
                            AND s.opened_at >= a.work_date
                            AND s.opened_at < DATE_ADD(a.work_date, INTERVAL 1 DAY))
            GROUP BY a.user_id, u.full_name, a.work_date
            ORDER BY a.work_date
            """, nativeQuery = true)
    List<AttendanceOverlapRow> findOverlapWithShifts(@Param("storeId") Long storeId,
                                                     @Param("from") LocalDate from,
                                                     @Param("to") LocalDate to);
}
