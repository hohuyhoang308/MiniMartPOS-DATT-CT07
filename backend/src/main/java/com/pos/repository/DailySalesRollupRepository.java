package com.pos.repository;

import com.pos.entity.DailySalesRollup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface DailySalesRollupRepository extends JpaRepository<DailySalesRollup, Long> {

    /**
     * TỔNG HỢP & UPSERT doanh thu ngày cho khoảng [from, to] từ hóa đơn COMPLETED. Idempotent:
     * chạy lại cùng khoảng → cập nhật đè (ON DUPLICATE KEY) nên an toàn cho cron chạy hằng đêm hoặc backfill.
     *
     * <p>Tách 3 mức gộp để KHÔNG đếm trùng: hóa đơn (revenue/discount/tax/count), dòng (items_sold),
     * và phân bổ lô (cogs = Σ qty×giá_nhập_lô). gross_profit = revenue − cogs.</p>
     */
    @Modifying
    @Query(value = """
            INSERT INTO daily_sales_rollup
                (store_id, sales_date, revenue, discount, tax, cogs, gross_profit, invoice_count, items_sold, rolled_at)
            SELECT inv.store_id, inv.d,
                   inv.revenue, inv.discount, inv.tax,
                   COALESCE(cog.cogs,0),
                   inv.revenue - COALESCE(cog.cogs,0),
                   inv.invoice_count, COALESCE(itm.items_sold,0), NOW()
            FROM (
                SELECT i.store_id, DATE(i.created_at) d,
                       SUM(i.total_amount) revenue, SUM(i.discount_amount) discount,
                       SUM(i.tax_amount) tax, COUNT(*) invoice_count
                FROM invoices i
                WHERE i.status='COMPLETED' AND DATE(i.created_at) BETWEEN :from AND :to
                GROUP BY i.store_id, DATE(i.created_at)
            ) inv
            LEFT JOIN (
                SELECT i.store_id, DATE(i.created_at) d, SUM(ii.quantity) items_sold
                FROM invoices i JOIN invoice_items ii ON ii.invoice_id=i.id
                WHERE i.status='COMPLETED' AND DATE(i.created_at) BETWEEN :from AND :to
                GROUP BY i.store_id, DATE(i.created_at)
            ) itm ON itm.store_id=inv.store_id AND itm.d=inv.d
            LEFT JOIN (
                SELECT i.store_id, DATE(i.created_at) d, SUM(iib.quantity*gri.import_price) cogs
                FROM invoices i
                JOIN invoice_items ii ON ii.invoice_id=i.id
                JOIN invoice_item_batches iib ON iib.invoice_item_id=ii.id
                JOIN goods_receipt_items gri ON gri.id=iib.batch_id
                WHERE i.status='COMPLETED' AND DATE(i.created_at) BETWEEN :from AND :to
                GROUP BY i.store_id, DATE(i.created_at)
            ) cog ON cog.store_id=inv.store_id AND cog.d=inv.d
            ON DUPLICATE KEY UPDATE
                revenue=VALUES(revenue), discount=VALUES(discount), tax=VALUES(tax),
                cogs=VALUES(cogs), gross_profit=VALUES(gross_profit),
                invoice_count=VALUES(invoice_count), items_sold=VALUES(items_sold), rolled_at=NOW()
            """, nativeQuery = true)
    void upsertRange(@Param("from") LocalDate from, @Param("to") LocalDate to);

    List<DailySalesRollup> findByStoreIdAndSalesDateBetweenOrderBySalesDate(Long storeId, LocalDate from, LocalDate to);

    List<DailySalesRollup> findBySalesDateBetween(LocalDate from, LocalDate to);

    boolean existsByStoreId(Long storeId);
}
