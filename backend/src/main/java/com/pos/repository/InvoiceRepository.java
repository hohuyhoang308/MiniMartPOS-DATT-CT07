package com.pos.repository;

import com.pos.entity.Invoice;
import com.pos.entity.enums.InvoiceStatus;
import com.pos.repository.projection.DailyRevenueRow;
import com.pos.repository.projection.EmployeeSalesRow;
import com.pos.repository.projection.HourlySalesRow;
import com.pos.repository.projection.PaymentBreakdownRow;
import com.pos.repository.projection.PeriodReportRow;
import com.pos.repository.projection.ShiftCashRow;
import com.pos.repository.projection.StoreAmountRow;
import com.pos.repository.projection.StoreCountRow;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    long countByCodeStartingWith(String prefix);

    /** Tra HĐ theo khóa chống trùng (idempotency) — trả lại HĐ cũ thay vì tạo mới khi FE gửi lại. */
    Optional<Invoice> findByIdempotencyKey(String idempotencyKey);

    /** Đếm hóa đơn của một ca theo trạng thái — chặn đóng ca khi còn HĐ QR chờ xác nhận tiền. */
    long countByShiftIdAndStatus(Long shiftId, InvoiceStatus status);

    /** Tổng doanh thu HĐ COMPLETED trong khoảng [from, to) — lọc chi nhánh (null = toàn chuỗi). */
    @Query("""
            SELECT COALESCE(SUM(i.totalAmount), 0) FROM Invoice i
            WHERE i.status = com.pos.entity.enums.InvoiceStatus.COMPLETED
              AND i.createdAt >= :from AND i.createdAt < :to
              AND (:storeId IS NULL OR i.store.id = :storeId)
            """)
    BigDecimal sumRevenue(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to,
                          @Param("storeId") Long storeId);

    /** Số hóa đơn COMPLETED trong khoảng [from, to) — lọc chi nhánh (null = toàn chuỗi). */
    @Query("""
            SELECT COUNT(i) FROM Invoice i
            WHERE i.status = com.pos.entity.enums.InvoiceStatus.COMPLETED
              AND i.createdAt >= :from AND i.createdAt < :to
              AND (:storeId IS NULL OR i.store.id = :storeId)
            """)
    long countCompleted(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to,
                        @Param("storeId") Long storeId);

    /** Doanh thu theo từng ngày trong khoảng (biểu đồ dashboard / báo cáo) — lọc chi nhánh. */
    @Query(value = """
            SELECT DATE_FORMAT(i.created_at, '%Y-%m-%d') AS day, COALESCE(SUM(i.total_amount), 0) AS revenue,
                   COUNT(*) AS invoiceCount
            FROM invoices i
            WHERE i.status = 'COMPLETED' AND i.created_at >= :from AND i.created_at < :to
              AND (:storeId IS NULL OR i.store_id = :storeId)
            GROUP BY DATE_FORMAT(i.created_at, '%Y-%m-%d')
            ORDER BY day
            """, nativeQuery = true)
    List<DailyRevenueRow> revenueByDay(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to,
                                       @Param("storeId") Long storeId);

    /**
     * Doanh thu + lợi nhuận + số HĐ gộp theo kỳ (lọc chi nhánh). {@code :fmt} là chuỗi định dạng của
     * MySQL DATE_FORMAT ('%Y-%m-%d' ngày, '%x-W%v' tuần ISO, '%Y-%m' tháng, '%Y' năm).
     * Lợi nhuận gộp trước theo từng hóa đơn (subquery) để KHÔNG nhân bản total_amount.
     */
    @Query(value = """
            SELECT DATE_FORMAT(i.created_at, :fmt) AS bucket,
                   COALESCE(SUM(i.total_amount), 0)                        AS revenue,
                   COALESCE(SUM(i.total_amount), 0) - COALESCE(SUM(c.cogs), 0) AS profit,
                   COUNT(*)                                                AS invoiceCount
            FROM invoices i
            LEFT JOIN (
                SELECT ii.invoice_id AS invoice_id,
                       SUM(gri.import_price * iib.quantity) AS cogs
                FROM invoice_item_batches iib
                JOIN invoice_items ii        ON ii.id  = iib.invoice_item_id
                JOIN goods_receipt_items gri ON gri.id = iib.batch_id
                JOIN invoices i2             ON i2.id  = ii.invoice_id
                WHERE i2.status = 'COMPLETED' AND i2.created_at >= :from AND i2.created_at < :to
                GROUP BY ii.invoice_id
            ) c ON c.invoice_id = i.id
            WHERE i.status = 'COMPLETED' AND i.created_at >= :from AND i.created_at < :to
              AND (:storeId IS NULL OR i.store_id = :storeId)
            GROUP BY DATE_FORMAT(i.created_at, :fmt)
            ORDER BY bucket
            """, nativeQuery = true)
    List<PeriodReportRow> revenueByPeriod(@Param("from") LocalDateTime from,
                                          @Param("to") LocalDateTime to,
                                          @Param("fmt") String fmt,
                                          @Param("storeId") Long storeId);

    /** Lọc hóa đơn theo thời gian / khách / trạng thái / ca / CỬA HÀNG / người bán (FR5.1).
     *  {@code userId} != null: chỉ HĐ thuộc ca của người đó (nhân viên chỉ xem HĐ của mình). */
    @Query("""
            SELECT i FROM Invoice i
            WHERE (:from IS NULL OR i.createdAt >= :from)
              AND (:to IS NULL OR i.createdAt < :to)
              AND (:customerId IS NULL OR i.customer.id = :customerId)
              AND (:status IS NULL OR i.status = :status)
              AND (:shiftId IS NULL OR i.shift.id = :shiftId)
              AND (:storeId IS NULL OR i.store.id = :storeId)
              AND (:userId IS NULL OR i.shift.user.id = :userId)
            ORDER BY i.createdAt DESC
            """)
    List<Invoice> search(@Param("from") LocalDateTime from,
                         @Param("to") LocalDateTime to,
                         @Param("customerId") Long customerId,
                         @Param("status") InvoiceStatus status,
                         @Param("shiftId") Long shiftId,
                         @Param("storeId") Long storeId,
                         @Param("userId") Long userId,
                         Pageable pageable);

    /** Lịch sử mua của 1 khách, LỌC theo chi nhánh đang làm việc (null = toàn chuỗi, chỉ ADMIN (toàn chuỗi)). */
    @Query("""
            SELECT i FROM Invoice i
            WHERE i.customer.id = :customerId AND i.status = :status
              AND (:storeId IS NULL OR i.store.id = :storeId)
            ORDER BY i.createdAt DESC
            """)
    List<Invoice> findCustomerHistory(@Param("customerId") Long customerId,
                                      @Param("status") InvoiceStatus status,
                                      @Param("storeId") Long storeId);

    /** Số khách thân thiết được phục vụ (HĐ COMPLETED có gắn khách) trong khoảng — lọc chi nhánh. */
    @Query("""
            SELECT COUNT(DISTINCT i.customer.id) FROM Invoice i
            WHERE i.status = com.pos.entity.enums.InvoiceStatus.COMPLETED
              AND i.customer IS NOT NULL
              AND i.createdAt >= :from AND i.createdAt < :to
              AND (:storeId IS NULL OR i.store.id = :storeId)
            """)
    long countDistinctCustomers(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to,
                                @Param("storeId") Long storeId);

    /** Cơ cấu doanh thu theo hình thức thanh toán — lọc chi nhánh. */
    @Query(value = """
            SELECT payment_method AS method, COUNT(*) AS cnt, COALESCE(SUM(total_amount), 0) AS amount
            FROM invoices
            WHERE status = 'COMPLETED' AND created_at >= :from AND created_at < :to
              AND (:storeId IS NULL OR store_id = :storeId)
            GROUP BY payment_method
            """, nativeQuery = true)
    List<PaymentBreakdownRow> paymentBreakdown(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to,
                                               @Param("storeId") Long storeId);

    /** Doanh thu theo giờ (tìm giờ cao điểm) — lọc chi nhánh. */
    @Query(value = """
            SELECT HOUR(created_at) AS hour, COALESCE(SUM(total_amount), 0) AS revenue, COUNT(*) AS invoiceCount
            FROM invoices
            WHERE status = 'COMPLETED' AND created_at >= :from AND created_at < :to
              AND (:storeId IS NULL OR store_id = :storeId)
            GROUP BY HOUR(created_at) ORDER BY hour
            """, nativeQuery = true)
    List<HourlySalesRow> hourlySales(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to,
                                     @Param("storeId") Long storeId);

    /** Giao dịch gần đây nhất (toàn chuỗi) cho feed dashboard của ADMIN (toàn chuỗi). */
    List<Invoice> findTop8ByOrderByCreatedAtDesc();

    /** Giao dịch gần đây nhất của MỘT chi nhánh — feed dashboard theo chi nhánh. */
    List<Invoice> findTop8ByStoreIdOrderByCreatedAtDesc(Long storeId);

    /** Tổng tiền mặt thực thu của 1 ca (HĐ COMPLETED, thanh toán CASH) — phục vụ đối soát quỹ. */
    @Query("""
            SELECT COALESCE(SUM(i.totalAmount), 0) FROM Invoice i
            WHERE i.shift.id = :shiftId
              AND i.status = com.pos.entity.enums.InvoiceStatus.COMPLETED
              AND i.paymentMethod = com.pos.entity.enums.PaymentMethod.CASH
            """)
    BigDecimal sumCashSalesByShift(@Param("shiftId") Long shiftId);

    /** Tiền mặt thực thu gộp theo NHIỀU ca cùng lúc — bỏ N+1 ở báo cáo ca. */
    @Query("""
            SELECT i.shift.id AS shiftId, COALESCE(SUM(i.totalAmount), 0) AS amount
            FROM Invoice i
            WHERE i.shift.id IN :shiftIds
              AND i.status = com.pos.entity.enums.InvoiceStatus.COMPLETED
              AND i.paymentMethod = com.pos.entity.enums.PaymentMethod.CASH
            GROUP BY i.shift.id
            """)
    List<ShiftCashRow> cashSalesByShiftIds(@Param("shiftIds") List<Long> shiftIds);

    /** Doanh số bán gộp theo THU NGÂN (qua ca) trong khoảng — báo cáo hiệu suất nhân viên, lọc chi nhánh. */
    @Query("""
            SELECT i.shift.user.id AS userId, i.shift.user.fullName AS cashierName,
                   COUNT(i) AS invoiceCount, COALESCE(SUM(i.totalAmount), 0) AS revenue
            FROM Invoice i
            WHERE i.status = com.pos.entity.enums.InvoiceStatus.COMPLETED
              AND i.createdAt >= :from AND i.createdAt < :to
              AND (:storeId IS NULL OR i.store.id = :storeId)
            GROUP BY i.shift.user.id, i.shift.user.fullName
            ORDER BY COALESCE(SUM(i.totalAmount), 0) DESC
            """)
    List<EmployeeSalesRow> salesByCashier(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to,
                                          @Param("storeId") Long storeId);

    /** Doanh thu gộp theo CHI NHÁNH trong khoảng — màn so sánh chi nhánh (1 truy vấn cho mọi cửa hàng). */
    @Query("""
            SELECT i.store.id AS storeId, COALESCE(SUM(i.totalAmount), 0) AS amount
            FROM Invoice i
            WHERE i.status = com.pos.entity.enums.InvoiceStatus.COMPLETED
              AND i.createdAt >= :from AND i.createdAt < :to
            GROUP BY i.store.id
            """)
    List<StoreAmountRow> revenueByStore(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    /** Số hóa đơn COMPLETED gộp theo CHI NHÁNH trong khoảng — màn so sánh chi nhánh. */
    @Query("""
            SELECT i.store.id AS storeId, COUNT(i) AS count
            FROM Invoice i
            WHERE i.status = com.pos.entity.enums.InvoiceStatus.COMPLETED
              AND i.createdAt >= :from AND i.createdAt < :to
            GROUP BY i.store.id
            """)
    List<StoreCountRow> countByStore(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
