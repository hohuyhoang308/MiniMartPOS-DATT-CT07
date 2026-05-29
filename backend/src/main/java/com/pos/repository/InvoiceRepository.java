package com.pos.repository;

import com.pos.entity.Invoice;
import com.pos.entity.enums.InvoiceStatus;
import com.pos.repository.projection.DailyRevenueRow;
import com.pos.repository.projection.HourlySalesRow;
import com.pos.repository.projection.PaymentBreakdownRow;
import com.pos.repository.projection.PeriodReportRow;
import org.springframework.data.domain.Pageable;
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

    /**
     * Doanh thu + lợi nhuận + số HĐ gộp theo kỳ. {@code :fmt} là chuỗi định dạng của
     * MySQL DATE_FORMAT ('%Y-%m-%d' ngày, '%x-W%v' tuần ISO, '%Y-%m' tháng, '%Y' năm).
     * Lợi nhuận gộp trước theo từng hóa đơn (subquery) để KHÔNG nhân bản total_amount.
     */
    @Query(value = """
            SELECT DATE_FORMAT(i.created_at, :fmt) AS bucket,
                   COALESCE(SUM(i.total_amount), 0) AS revenue,
                   COALESCE(SUM(p.profit), 0)      AS profit,
                   COUNT(*)                        AS invoiceCount
            FROM invoices i
            LEFT JOIN (
                SELECT ii.invoice_id AS invoice_id,
                       SUM((ii.unit_price - gri.import_price) * iib.quantity) AS profit
                FROM invoice_item_batches iib
                JOIN invoice_items ii      ON ii.id  = iib.invoice_item_id
                JOIN goods_receipt_items gri ON gri.id = iib.batch_id
                GROUP BY ii.invoice_id
            ) p ON p.invoice_id = i.id
            WHERE i.status = 'COMPLETED' AND i.created_at >= :from AND i.created_at < :to
            GROUP BY DATE_FORMAT(i.created_at, :fmt)
            ORDER BY bucket
            """, nativeQuery = true)
    List<PeriodReportRow> revenueByPeriod(@Param("from") LocalDateTime from,
                                          @Param("to") LocalDateTime to,
                                          @Param("fmt") String fmt);

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
                         @Param("shiftId") Long shiftId,
                         Pageable pageable);

    List<Invoice> findByCustomerIdAndStatusOrderByCreatedAtDesc(Long customerId, InvoiceStatus status);

    /** Số khách thân thiết được phục vụ (HĐ COMPLETED có gắn khách) trong khoảng. */
    @Query("""
            SELECT COUNT(DISTINCT i.customer.id) FROM Invoice i
            WHERE i.status = com.pos.entity.enums.InvoiceStatus.COMPLETED
              AND i.customer IS NOT NULL
              AND i.createdAt >= :from AND i.createdAt < :to
            """)
    long countDistinctCustomers(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    /** Cơ cấu doanh thu theo hình thức thanh toán. */
    @Query(value = """
            SELECT payment_method AS method, COUNT(*) AS cnt, COALESCE(SUM(total_amount), 0) AS amount
            FROM invoices
            WHERE status = 'COMPLETED' AND created_at >= :from AND created_at < :to
            GROUP BY payment_method
            """, nativeQuery = true)
    List<PaymentBreakdownRow> paymentBreakdown(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    /** Doanh thu theo giờ (tìm giờ cao điểm). */
    @Query(value = """
            SELECT HOUR(created_at) AS hour, COALESCE(SUM(total_amount), 0) AS revenue, COUNT(*) AS invoiceCount
            FROM invoices
            WHERE status = 'COMPLETED' AND created_at >= :from AND created_at < :to
            GROUP BY HOUR(created_at) ORDER BY hour
            """, nativeQuery = true)
    List<HourlySalesRow> hourlySales(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    /** Giao dịch gần đây nhất (mọi trạng thái) cho feed dashboard. */
    List<Invoice> findTop8ByOrderByCreatedAtDesc();

    /** Tổng tiền mặt thực thu của 1 ca (HĐ COMPLETED, thanh toán CASH) — phục vụ đối soát quỹ. */
    @Query("""
            SELECT COALESCE(SUM(i.totalAmount), 0) FROM Invoice i
            WHERE i.shift.id = :shiftId
              AND i.status = com.pos.entity.enums.InvoiceStatus.COMPLETED
              AND i.paymentMethod = com.pos.entity.enums.PaymentMethod.CASH
            """)
    BigDecimal sumCashSalesByShift(@Param("shiftId") Long shiftId);
}
