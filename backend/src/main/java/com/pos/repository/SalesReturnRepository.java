package com.pos.repository;

import com.pos.entity.SalesReturn;
import com.pos.repository.projection.BucketAmountRow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface SalesReturnRepository extends JpaRepository<SalesReturn, Long> {

    List<SalesReturn> findByInvoiceIdOrderByCreatedAtDesc(Long invoiceId);

    List<SalesReturn> findTop200ByOrderByCreatedAtDesc();

    /** Tổng tiền hoàn trong khoảng (theo ngày trả) — để trừ khỏi doanh thu (cơ sở tiền). */
    @Query("SELECT COALESCE(SUM(r.refundAmount), 0) FROM SalesReturn r WHERE r.createdAt >= :from AND r.createdAt < :to")
    BigDecimal sumRefundBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    /** Tiền hoàn gộp theo KỲ (bucket) — để trừ doanh thu báo cáo theo ngày/tuần/tháng/năm. */
    @Query(value = """
            SELECT DATE_FORMAT(created_at, :fmt) AS bucket, COALESCE(SUM(refund_amount), 0) AS amount
            FROM sales_returns
            WHERE created_at >= :from AND created_at < :to
            GROUP BY DATE_FORMAT(created_at, :fmt)
            """, nativeQuery = true)
    List<BucketAmountRow> refundByPeriod(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to,
                                         @Param("fmt") String fmt);
}
