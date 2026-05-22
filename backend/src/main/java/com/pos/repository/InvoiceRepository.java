package com.pos.repository;

import com.pos.entity.Invoice;
import com.pos.entity.enums.InvoiceStatus;
import com.pos.repository.projection.DailyRevenueRow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    long countByCodeStartingWith(String prefix);

    /** Tổng doanh thu HĐ COMPLETED trong khoảng [from, to). */
    @Query("""
            SELECT COALESCE(SUM(i.totalAmount), 0) FROM Invoice i
            WHERE i.status = com.pos.entity.enums.InvoiceStatus.COMPLETED
              AND i.createdAt >= :from AND i.createdAt < :to
            """)
    BigDecimal sumRevenue(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    /** Số hóa đơn COMPLETED trong khoảng [from, to). */
    @Query("""
            SELECT COUNT(i) FROM Invoice i
            WHERE i.status = com.pos.entity.enums.InvoiceStatus.COMPLETED
              AND i.createdAt >= :from AND i.createdAt < :to
            """)
    long countCompleted(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    /** Doanh thu theo từng ngày trong khoảng (cho biểu đồ dashboard / báo cáo). */
    @Query(value = """
            SELECT DATE(i.created_at) AS day, COALESCE(SUM(i.total_amount), 0) AS revenue,
                   COUNT(*) AS invoiceCount
            FROM invoices i
            WHERE i.status = 'COMPLETED' AND i.created_at >= :from AND i.created_at < :to
            GROUP BY DATE(i.created_at)
            ORDER BY day
            """, nativeQuery = true)
    List<DailyRevenueRow> revenueByDay(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    /** Lọc hóa đơn theo khoảng thời gian / khách / trạng thái / ca (FR5.1). */
    @Query("""
            SELECT i FROM Invoice i
            WHERE (:from IS NULL OR i.createdAt >= :from)
              AND (:to IS NULL OR i.createdAt < :to)
              AND (:customerId IS NULL OR i.customer.id = :customerId)
              AND (:status IS NULL OR i.status = :status)
              AND (:shiftId IS NULL OR i.shift.id = :shiftId)
            ORDER BY i.createdAt DESC
            """)
    List<Invoice> search(@Param("from") LocalDateTime from,
                         @Param("to") LocalDateTime to,
                         @Param("customerId") Long customerId,
                         @Param("status") InvoiceStatus status,
                         @Param("shiftId") Long shiftId);

    List<Invoice> findByCustomerIdAndStatusOrderByCreatedAtDesc(Long customerId, InvoiceStatus status);
}
