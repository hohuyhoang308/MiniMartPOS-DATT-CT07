package com.pos.repository;

import com.pos.entity.InvoiceItem;
import com.pos.repository.projection.TopProductRow;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface InvoiceItemRepository extends JpaRepository<InvoiceItem, Long> {

    /** Top sản phẩm bán chạy theo số lượng (HĐ COMPLETED) trong khoảng thời gian. */
    @Query("""
            SELECT ii.product.id AS productId, ii.product.name AS productName,
                   SUM(ii.quantity) AS quantitySold, SUM(ii.subtotal) AS revenue
            FROM InvoiceItem ii
            WHERE ii.invoice.status = com.pos.entity.enums.InvoiceStatus.COMPLETED
              AND ii.invoice.createdAt >= :from AND ii.invoice.createdAt < :to
            GROUP BY ii.product.id, ii.product.name
            ORDER BY SUM(ii.quantity) DESC
            """)
    List<TopProductRow> topProducts(@Param("from") LocalDateTime from,
                                    @Param("to") LocalDateTime to,
                                    Pageable pageable);
}
