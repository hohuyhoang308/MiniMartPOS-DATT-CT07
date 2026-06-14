package com.pos.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * Tạo các TRIGGER toàn vẹn ở tầng CSDL — thứ mà khóa ngoại đơn cột KHÔNG diễn đạt được.
 *
 * <p>Vì sao bằng Java mà không đặt trong {@code db/schema.sql}: trình chạy {@code spring.sql.init}
 * tách câu lệnh theo dấu {@code ;}, nên không thể chứa thân trigger (vốn có nhiều {@code ;}).
 * Driver JDBC thì gửi nguyên một câu {@code CREATE TRIGGER} nên không vướng — ta dùng {@link JdbcTemplate}.</p>
 *
 * <p>Idempotent: {@code DROP TRIGGER IF EXISTS} rồi {@code CREATE} mỗi lần khởi động. Chạy SAU
 * {@code spring.sql.init} (bảng đã có) và TRƯỚC các *DataInitializer ({@code @Order} nhỏ hơn) để dữ
 * liệu seed cũng được kiểm. Nếu tài khoản DB thiếu quyền TRIGGER (vài môi trường prod siết quyền) thì
 * chỉ log cảnh báo, KHÔNG làm app dừng — tầng service vẫn chặn các vi phạm này.</p>
 *
 * <p>Các bất biến được ép:
 * <ul>
 *   <li>Phân bổ lô khi bán ({@code invoice_item_batches}) phải: cùng CHI NHÁNH với hóa đơn; đúng SẢN PHẨM
 *       của dòng hóa đơn; không vượt số lượng dòng; không bán vượt tồn của lô (loại HĐ đã hủy).</li>
 *   <li>Lên kệ / lấy về kho phải cùng CHI NHÁNH giữa lô và kệ; một LÔ chỉ nằm trên MỘT kệ.</li>
 *   <li>Hóa đơn phải cùng CHI NHÁNH với ca làm việc.</li>
 *   <li>Mỗi nhân viên chỉ có MỘT ca đang mở.</li>
 * </ul>
 */
@Component
@Order(5)
public class SchemaTriggersInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SchemaTriggersInitializer.class);

    private final JdbcTemplate jdbc;
    private final Environment env;

    public SchemaTriggersInitializer(JdbcTemplate jdbc, Environment env) {
        this.jdbc = jdbc;
        this.env = env;
    }

    /** Mỗi phần tử: [tên trigger, câu CREATE TRIGGER đầy đủ]. */
    private static final List<String[]> TRIGGERS = List.of(
            new String[]{"trg_iib_bi_integrity", """
                CREATE TRIGGER trg_iib_bi_integrity BEFORE INSERT ON invoice_item_batches FOR EACH ROW
                BEGIN
                    DECLARE v_item_product BIGINT; DECLARE v_item_qty INT; DECLARE v_inv_store BIGINT;
                    DECLARE v_batch_product BIGINT; DECLARE v_batch_store BIGINT; DECLARE v_batch_qty INT;
                    DECLARE v_alloc INT; DECLARE v_sold INT;
                    SELECT ii.product_id, ii.quantity, i.store_id
                      INTO v_item_product, v_item_qty, v_inv_store
                      FROM invoice_items ii JOIN invoices i ON i.id = ii.invoice_id
                      WHERE ii.id = NEW.invoice_item_id;
                    SELECT gri.product_id, gr.store_id, gri.quantity
                      INTO v_batch_product, v_batch_store, v_batch_qty
                      FROM goods_receipt_items gri JOIN goods_receipts gr ON gr.id = gri.receipt_id
                      WHERE gri.id = NEW.batch_id;
                    IF v_batch_store <> v_inv_store THEN
                        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Vi pham toan ven: phan bo lo khac chi nhanh voi hoa don';
                    END IF;
                    IF v_batch_product <> v_item_product THEN
                        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Vi pham toan ven: lo phan bo khong dung san pham cua dong hoa don';
                    END IF;
                    SELECT COALESCE(SUM(iib.quantity),0) INTO v_alloc
                      FROM invoice_item_batches iib WHERE iib.invoice_item_id = NEW.invoice_item_id;
                    IF v_alloc + NEW.quantity > v_item_qty THEN
                        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Vi pham toan ven: tong phan bo lo vuot so luong dong hoa don';
                    END IF;
                    SELECT COALESCE(SUM(iib.quantity),0) INTO v_sold
                      FROM invoice_item_batches iib
                      JOIN invoice_items ii2 ON ii2.id = iib.invoice_item_id
                      JOIN invoices i2 ON i2.id = ii2.invoice_id
                      WHERE iib.batch_id = NEW.batch_id AND i2.status <> 'CANCELLED';
                    IF v_sold + NEW.quantity > v_batch_qty THEN
                        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Vi pham toan ven: ban vuot ton kho cua lo';
                    END IF;
                END
                """},
            new String[]{"trg_st_bi_integrity", """
                CREATE TRIGGER trg_st_bi_integrity BEFORE INSERT ON shelf_transfers FOR EACH ROW
                BEGIN
                    DECLARE v_batch_store BIGINT; DECLARE v_shelf_store BIGINT; DECLARE v_other_shelf BIGINT;
                    SELECT gr.store_id INTO v_batch_store
                      FROM goods_receipt_items gri JOIN goods_receipts gr ON gr.id = gri.receipt_id
                      WHERE gri.id = NEW.batch_id;
                    SELECT store_id INTO v_shelf_store FROM shelves WHERE id = NEW.shelf_id;
                    IF v_batch_store <> v_shelf_store THEN
                        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Vi pham toan ven: len ke khac chi nhanh voi lo';
                    END IF;
                    SELECT MIN(shelf_id) INTO v_other_shelf FROM shelf_transfers
                      WHERE batch_id = NEW.batch_id AND shelf_id <> NEW.shelf_id;
                    IF v_other_shelf IS NOT NULL THEN
                        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Vi pham toan ven: mot lo chi duoc nam tren mot ke';
                    END IF;
                END
                """},
            new String[]{"trg_sr_bi_integrity", """
                CREATE TRIGGER trg_sr_bi_integrity BEFORE INSERT ON shelf_returns FOR EACH ROW
                BEGIN
                    DECLARE v_batch_store BIGINT; DECLARE v_shelf_store BIGINT;
                    SELECT gr.store_id INTO v_batch_store
                      FROM goods_receipt_items gri JOIN goods_receipts gr ON gr.id = gri.receipt_id
                      WHERE gri.id = NEW.batch_id;
                    SELECT store_id INTO v_shelf_store FROM shelves WHERE id = NEW.shelf_id;
                    IF v_batch_store <> v_shelf_store THEN
                        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Vi pham toan ven: lay hang tu ke khac chi nhanh voi lo';
                    END IF;
                END
                """},
            new String[]{"trg_inv_bi_store", """
                CREATE TRIGGER trg_inv_bi_store BEFORE INSERT ON invoices FOR EACH ROW
                BEGIN
                    DECLARE v_shift_store BIGINT;
                    SELECT store_id INTO v_shift_store FROM work_shifts WHERE id = NEW.shift_id;
                    IF v_shift_store IS NOT NULL AND NEW.store_id <> v_shift_store THEN
                        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Vi pham toan ven: hoa don khac chi nhanh voi ca lam viec';
                    END IF;
                END
                """},
            new String[]{"trg_ws_bi_oneopen", """
                CREATE TRIGGER trg_ws_bi_oneopen BEFORE INSERT ON work_shifts FOR EACH ROW
                BEGIN
                    IF NEW.status = 'OPEN'
                       AND EXISTS (SELECT 1 FROM work_shifts WHERE user_id = NEW.user_id AND status = 'OPEN') THEN
                        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Vi pham toan ven: nhan vien da co mot ca dang mo';
                    END IF;
                END
                """}
    );

    @Override
    public void run(String... args) {
        boolean prod = Arrays.asList(env.getActiveProfiles()).contains("prod");
        int ok = 0;
        for (String[] t : TRIGGERS) {
            try {
                jdbc.execute("DROP TRIGGER IF EXISTS " + t[0]);
                jdbc.execute(t[1]);
                ok++;
            } catch (Exception e) {
                // PROD: KHÔNG cho chạy thiếu trigger toàn vẹn — nếu không, các bất biến quan trọng (không bán âm,
                // phân bổ chéo cửa hàng, 1 ca/người) chỉ còn được chặn ở tầng service (có thể đua khi tải cao).
                // Buộc cấp quyền TRIGGER cho tài khoản DB rồi khởi động lại. DEV: chỉ cảnh báo cho tiện.
                if (prod) {
                    throw new IllegalStateException("Không tạo được trigger toàn vẹn '" + t[0]
                            + "' ở môi trường prod (" + e.getMessage()
                            + "). Hãy cấp quyền TRIGGER cho tài khoản CSDL rồi khởi động lại.", e);
                }
                log.warn("Không tạo được trigger {} ({}). Tầng service vẫn chặn vi phạm này.",
                        t[0], e.getMessage());
            }
        }
        log.info("Đã cài {}/{} trigger toàn vẹn ở tầng CSDL.", ok, TRIGGERS.size());
    }
}
