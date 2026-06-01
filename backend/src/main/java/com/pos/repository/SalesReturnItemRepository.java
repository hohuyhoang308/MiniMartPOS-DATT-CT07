package com.pos.repository;

import com.pos.entity.SalesReturnItem;
import com.pos.repository.projection.BucketAmountRow;
import com.pos.repository.projection.ItemBatchReturnedRow;
import com.pos.repository.projection.ReturnedQtyRow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface SalesReturnItemRepository extends JpaRepository<SalesReturnItem, Long> {

    /** Lợi nhuận của hàng đã TRẢ trong khoảng (theo ngày trả) — để trừ khỏi lợi nhuận. */
    @Query("""
            SELECT COALESCE(SUM((ri.unitPrice - ri.batch.importPrice) * ri.quantity), 0)
            FROM SalesReturnItem ri
            WHERE ri.salesReturn.createdAt >= :from AND ri.salesReturn.createdAt < :to
            """)
    BigDecimal sumReturnedProfitBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    /** Lợi nhuận hàng TRẢ gộp theo KỲ (bucket) — để trừ lợi nhuận báo cáo theo kỳ. */
    @Query(value = """
            SELECT DATE_FORMAT(r.created_at, :fmt) AS bucket,
                   COALESCE(SUM((ri.unit_price - gri.import_price) * ri.quantity), 0) AS amount
            FROM sales_return_items ri
            JOIN sales_returns r        ON r.id  = ri.return_id
            JOIN goods_receipt_items gri ON gri.id = ri.batch_id
            WHERE r.created_at >= :from AND r.created_at < :to
            GROUP BY DATE_FORMAT(r.created_at, :fmt)
            """, nativeQuery = true)
    List<BucketAmountRow> returnedProfitByPeriod(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to,
                                                 @Param("fmt") String fmt);

    /** Số đã trả theo từng dòng bán của 1 hóa đơn — để tính số còn được trả. */
    @Query("""
            SELECT ri.invoiceItem.id AS invoiceItemId, COALESCE(SUM(ri.quantity), 0) AS qty
            FROM SalesReturnItem ri
            WHERE ri.invoiceItem.invoice.id = :invoiceId
            GROUP BY ri.invoiceItem.id
            """)
    List<ReturnedQtyRow> returnedByInvoiceItem(@Param("invoiceId") Long invoiceId);

    /** Số đã trả theo từng (dòng bán, lô) — để không trả vượt phần đã phân bổ của từng lô. */
    @Query("""
            SELECT ri.invoiceItem.id AS invoiceItemId, ri.batch.id AS batchId, COALESCE(SUM(ri.quantity), 0) AS qty
            FROM SalesReturnItem ri
            WHERE ri.invoiceItem.id = :invoiceItemId
            GROUP BY ri.invoiceItem.id, ri.batch.id
            """)
    List<ItemBatchReturnedRow> returnedByItemAndBatch(@Param("invoiceItemId") Long invoiceItemId);
}
