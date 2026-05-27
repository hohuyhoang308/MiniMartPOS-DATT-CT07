package com.pos.repository;

import com.pos.entity.InvoiceItem;
import com.pos.repository.projection.CategorySalesRow;
import com.pos.repository.projection.ProductCountRow;
import com.pos.repository.projection.ProductSalesRow;
import com.pos.repository.projection.TopProductRow;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
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

    /** Lợi nhuận gộp = SUM((giá bán - giá vốn) * số lượng) của HĐ COMPLETED. */
    @Query("""
            SELECT COALESCE(SUM((ii.unitPrice - ii.product.costPrice) * ii.quantity), 0)
            FROM InvoiceItem ii
            WHERE ii.invoice.status = com.pos.entity.enums.InvoiceStatus.COMPLETED
              AND ii.invoice.createdAt >= :from AND ii.invoice.createdAt < :to
            """)
    BigDecimal sumProfit(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    /** Tổng số sản phẩm đã bán. */
    @Query("""
            SELECT COALESCE(SUM(ii.quantity), 0) FROM InvoiceItem ii
            WHERE ii.invoice.status = com.pos.entity.enums.InvoiceStatus.COMPLETED
              AND ii.invoice.createdAt >= :from AND ii.invoice.createdAt < :to
            """)
    long sumQuantity(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    /** Doanh thu & sản lượng theo danh mục. */
    @Query("""
            SELECT ii.product.category.name AS categoryName,
                   COALESCE(SUM(ii.subtotal), 0) AS revenue,
                   COALESCE(SUM(ii.quantity), 0) AS quantity
            FROM InvoiceItem ii
            WHERE ii.invoice.status = com.pos.entity.enums.InvoiceStatus.COMPLETED
              AND ii.invoice.createdAt >= :from AND ii.invoice.createdAt < :to
            GROUP BY ii.product.category.name
            ORDER BY SUM(ii.subtotal) DESC
            """)
    List<CategorySalesRow> categorySales(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    /** Sản lượng đã bán theo sản phẩm kể từ {@code from} (HĐ COMPLETED) — tính tốc độ bán. */
    @Query("""
            SELECT ii.product.id AS productId, COALESCE(SUM(ii.quantity), 0) AS soldQty
            FROM InvoiceItem ii
            WHERE ii.invoice.status = com.pos.entity.enums.InvoiceStatus.COMPLETED
              AND ii.invoice.createdAt >= :from
            GROUP BY ii.product.id
            """)
    List<ProductSalesRow> soldQuantitySince(@Param("from") LocalDateTime from);

    /**
     * Gợi ý "mua kèm" (market-basket): các sản phẩm hay xuất hiện CHUNG hóa đơn với sản phẩm cho trước,
     * xếp theo số lần đồng xuất hiện giảm dần (chỉ tính HĐ COMPLETED).
     */
    @Query("""
            SELECT ii2.product.id AS productId, COUNT(ii2.id) AS cnt
            FROM InvoiceItem ii1, InvoiceItem ii2
            WHERE ii1.invoice.id = ii2.invoice.id
              AND ii1.product.id = :productId
              AND ii2.product.id <> :productId
              AND ii1.invoice.status = com.pos.entity.enums.InvoiceStatus.COMPLETED
            GROUP BY ii2.product.id
            ORDER BY COUNT(ii2.id) DESC
            """)
    List<ProductCountRow> boughtTogether(@Param("productId") Long productId, Pageable pageable);
}
