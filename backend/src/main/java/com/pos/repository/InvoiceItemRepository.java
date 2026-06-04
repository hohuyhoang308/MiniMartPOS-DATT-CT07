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

    /**
     * Lợi nhuận gộp CHÍNH XÁC theo FIFO‑lô: COGS lấy đúng {@code import_price} của lô đã phân bổ khi bán
     * (không dùng cost_price hiện tại của sản phẩm → báo cáo quá khứ không bị đổi khi đổi giá nhập).
     * Lợi nhuận = Σ (giá bán − giá nhập của lô) × số lượng phân bổ, trên HĐ COMPLETED.
     */
    @Query("""
            SELECT COALESCE(SUM((iib.invoiceItem.unitPrice - iib.batch.importPrice) * iib.quantity), 0)
            FROM InvoiceItemBatch iib
            WHERE iib.invoiceItem.invoice.status = com.pos.entity.enums.InvoiceStatus.COMPLETED
              AND iib.invoiceItem.invoice.createdAt >= :from AND iib.invoiceItem.invoice.createdAt < :to
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
     * Sản lượng bán theo (sản phẩm, NGÀY) kể từ {@code from} — mỗi dòng là số bán 1 ngày của 1 SP.
     * Dùng để ước lượng ĐỘ BIẾN ĐỘNG nhu cầu (σ) → tính tồn an toàn theo mức phục vụ.
     */
    @Query(value = """
            SELECT ii.product_id AS productId, COALESCE(SUM(ii.quantity), 0) AS soldQty
            FROM invoice_items ii
            JOIN invoices i ON i.id = ii.invoice_id
            WHERE i.status = 'COMPLETED' AND i.created_at >= :from
            GROUP BY ii.product_id, DATE(i.created_at)
            """, nativeQuery = true)
    List<ProductSalesRow> dailySalesSince(@Param("from") LocalDateTime from);

    /**
     * Sản lượng bán theo (sản phẩm, TUẦN) kể từ {@code from} — mỗi dòng là số bán trong 1 tuần của 1 SP.
     * Gộp theo tuần để LÀM MƯỢT chuỗi nhu cầu bán lẻ (rất nhiều ngày bán = 0). Nếu tính độ biến động
     * theo từng NGÀY thì các ngày trống sẽ thổi phồng σ khiến mọi mặt hàng đều "thất thường" (Z) —
     * gộp tuần cho ra CV (XYZ) phản ánh đúng độ ổn định thực tế của nhu cầu.
     */
    @Query(value = """
            SELECT ii.product_id AS productId, COALESCE(SUM(ii.quantity), 0) AS soldQty
            FROM invoice_items ii
            JOIN invoices i ON i.id = ii.invoice_id
            WHERE i.status = 'COMPLETED' AND i.created_at >= :from
            GROUP BY ii.product_id, FLOOR(DATEDIFF(i.created_at, :from) / 7)
            """, nativeQuery = true)
    List<ProductSalesRow> weeklySalesSince(@Param("from") LocalDateTime from);

    /** Doanh thu + sản lượng theo sản phẩm kể từ {@code from} (HĐ COMPLETED) — phân tích ABC/XYZ. */
    @Query("""
            SELECT ii.product.id AS productId,
                   COALESCE(SUM(ii.subtotal), 0) AS revenue,
                   COALESCE(SUM(ii.quantity), 0) AS qty
            FROM InvoiceItem ii
            WHERE ii.invoice.status = com.pos.entity.enums.InvoiceStatus.COMPLETED
              AND ii.invoice.createdAt >= :from
            GROUP BY ii.product.id
            """)
    List<com.pos.repository.projection.ProductRevenueRow> revenueByProductSince(@Param("from") LocalDateTime from);

    /**
     * Gợi ý "mua kèm" (market-basket): số hóa đơn ĐỒNG XUẤT HIỆN co(A,B) của mỗi sản phẩm B với A.
     * Đếm theo SỐ HÓA ĐƠN phân biệt (không phải số dòng) để tính lift đúng ở tầng service.
     */
    @Query("""
            SELECT ii2.product.id AS productId, COUNT(DISTINCT ii2.invoice.id) AS cnt
            FROM InvoiceItem ii1, InvoiceItem ii2
            WHERE ii1.invoice.id = ii2.invoice.id
              AND ii1.product.id = :productId
              AND ii2.product.id <> :productId
              AND ii1.invoice.status = com.pos.entity.enums.InvoiceStatus.COMPLETED
            GROUP BY ii2.product.id
            ORDER BY COUNT(DISTINCT ii2.invoice.id) DESC
            """)
    List<ProductCountRow> boughtTogether(@Param("productId") Long productId, Pageable pageable);

    /** Số HĐ COMPLETED chứa mỗi sản phẩm n(B) — mẫu số để tính lift cho gợi ý mua kèm. */
    @Query("""
            SELECT ii.product.id AS productId, COUNT(DISTINCT ii.invoice.id) AS cnt
            FROM InvoiceItem ii
            WHERE ii.invoice.status = com.pos.entity.enums.InvoiceStatus.COMPLETED
            GROUP BY ii.product.id
            """)
    List<ProductCountRow> invoiceCountByProduct();
}
