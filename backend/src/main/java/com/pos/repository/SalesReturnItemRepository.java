package com.pos.repository;

import com.pos.entity.SalesReturnItem;
import com.pos.repository.projection.ItemBatchReturnedRow;
import com.pos.repository.projection.ReturnedQtyRow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SalesReturnItemRepository extends JpaRepository<SalesReturnItem, Long> {

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
