-- =====================================================================
--  SCHEMA TỰ KHỞI TẠO (idempotent) cho POS — chạy bởi Spring Boot
--  spring.sql.init mỗi lần khởi động. An toàn chạy lại nhiều lần:
--    - CREATE TABLE IF NOT EXISTS  → đã có thì bỏ qua (không mất dữ liệu)
--    - CREATE OR REPLACE VIEW      → cập nhật định nghĩa view
--  Dữ liệu nền (users/sản phẩm/khách/khuyến mãi...) do các *DataInitializer
--  trong code seed (idempotent), KHÔNG đặt ở đây.
--  => Drop database rồi khởi động lại: cấu trúc + view tự dựng, seeders tự nạp.
-- =====================================================================

-- Ghim charset/collation MẶC ĐỊNH cho database hiện tại → mọi bảng tạo sau tự thừa hưởng,
-- tránh lệch collation (nguyên nhân gốc của migration "đồng nhất collation" bên dưới). Idempotent.
ALTER DATABASE CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

-- 0. CHI NHÁNH / CỬA HÀNG trong chuỗi (đa chuỗi) ----------------------
--     Phải tạo TRƯỚC mọi bảng tham chiếu store_id. Chi nhánh mặc định id=1 (CH01)
--     do migration bên dưới chèn (dữ liệu cũ trước đa chuỗi thuộc về chi nhánh này).
CREATE TABLE IF NOT EXISTS stores (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    code       VARCHAR(30)  NOT NULL UNIQUE,           -- CH01, CH02...
    name       VARCHAR(150) NOT NULL,
    address    VARCHAR(255),
    phone      VARCHAR(20),
    status     ENUM('ACTIVE','INACTIVE') NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 1. Người dùng & phân quyền ------------------------------------------
--     store_id NULL = quản trị toàn chuỗi (CHAIN_ADMIN); còn lại gắn 1 chi nhánh.
CREATE TABLE IF NOT EXISTS users (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    full_name     VARCHAR(100) NOT NULL,
    role          ENUM('ADMIN','MANAGER','STAFF') NOT NULL,   -- ADMIN=toàn chuỗi; MANAGER/STAFF=một cửa hàng
    store_id      BIGINT,
    status        ENUM('ACTIVE','LOCKED') NOT NULL DEFAULT 'ACTIVE',
    created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_user_store (store_id),
    CONSTRAINT fk_user_store FOREIGN KEY (store_id) REFERENCES stores(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2. Danh mục, đơn vị, nhà cung cấp -----------------------------------
CREATE TABLE IF NOT EXISTS categories (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(255),
    status      ENUM('ACTIVE','INACTIVE') NOT NULL DEFAULT 'ACTIVE'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS units (
    id   BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS suppliers (
    id      BIGINT AUTO_INCREMENT PRIMARY KEY,
    name    VARCHAR(150) NOT NULL,
    phone   VARCHAR(20),
    email   VARCHAR(100),
    address VARCHAR(255),
    status  ENUM('ACTIVE','INACTIVE') NOT NULL DEFAULT 'ACTIVE'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3. Sản phẩm ---------------------------------------------------------
CREATE TABLE IF NOT EXISTS products (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    barcode     VARCHAR(50)  NOT NULL UNIQUE,
    name        VARCHAR(150) NOT NULL,
    category_id BIGINT NOT NULL,
    unit_id     BIGINT NOT NULL,
    cost_price  DECIMAL(12,2) NOT NULL DEFAULT 0,
    sale_price  DECIMAL(12,2) NOT NULL,                  -- giá bán ĐÃ GỒM VAT (chuẩn bán lẻ VN)
    tax_rate    DECIMAL(5,2)  NOT NULL DEFAULT 8.00,      -- thuế suất GTGT % (vd 0/8/10)
    pack_size   INT NOT NULL DEFAULT 1,                   -- 1 ĐV mua (thùng) = ? ĐV bán cơ bản (lon)
    pack_unit_id BIGINT,                                  -- đơn vị MUA (thùng/lốc) — NULL nếu chỉ bán lẻ
    image_url   VARCHAR(255),
    min_stock   INT NOT NULL DEFAULT 0,
    status      ENUM('ACTIVE','INACTIVE') NOT NULL DEFAULT 'ACTIVE',
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_product_name (name),
    KEY idx_product_category (category_id),
    KEY idx_product_unit (unit_id),
    CONSTRAINT fk_product_category  FOREIGN KEY (category_id) REFERENCES categories(id),
    CONSTRAINT fk_product_unit      FOREIGN KEY (unit_id)     REFERENCES units(id),
    CONSTRAINT fk_product_pack_unit FOREIGN KEY (pack_unit_id) REFERENCES units(id),
    CONSTRAINT chk_product_price    CHECK (sale_price >= 0 AND cost_price >= 0),
    CONSTRAINT chk_product_packsize CHECK (pack_size >= 1),
    CONSTRAINT chk_product_minstock CHECK (min_stock >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 4. Nhập kho (goods_receipt_items = LÔ HÀNG) -------------------------
CREATE TABLE IF NOT EXISTS goods_receipts (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    code         VARCHAR(30) NOT NULL UNIQUE,
    store_id     BIGINT NOT NULL,                       -- chi nhánh nhập (LÔ thừa hưởng chi nhánh từ đây)
    supplier_id  BIGINT NOT NULL,
    created_by   BIGINT NOT NULL,
    total_amount DECIMAL(14,2) NOT NULL DEFAULT 0,
    note         VARCHAR(255),
    created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_receipt_store (store_id),
    CONSTRAINT fk_receipt_store    FOREIGN KEY (store_id)    REFERENCES stores(id),
    CONSTRAINT fk_receipt_supplier FOREIGN KEY (supplier_id) REFERENCES suppliers(id),
    CONSTRAINT fk_receipt_user     FOREIGN KEY (created_by)  REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS goods_receipt_items (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    receipt_id   BIGINT NOT NULL,
    product_id   BIGINT NOT NULL,
    quantity     INT NOT NULL,
    import_price DECIMAL(12,2) NOT NULL,
    expiry_date  DATE,
    KEY idx_gri_product (product_id),
    KEY idx_gri_expiry (expiry_date),
    CONSTRAINT fk_gri_receipt FOREIGN KEY (receipt_id) REFERENCES goods_receipts(id) ON DELETE CASCADE,
    CONSTRAINT fk_gri_product FOREIGN KEY (product_id) REFERENCES products(id),
    CONSTRAINT chk_gri_qty CHECK (quantity > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 5. Khách hàng & khuyến mãi ------------------------------------------
CREATE TABLE IF NOT EXISTS customers (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    full_name      VARCHAR(100) NOT NULL,
    phone          VARCHAR(20)  NOT NULL UNIQUE,
    email          VARCHAR(100),
    loyalty_points INT NOT NULL DEFAULT 0,
    created_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_customer_points CHECK (loyalty_points >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS promotions (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    code             VARCHAR(30)  NOT NULL UNIQUE,
    name             VARCHAR(150) NOT NULL,
    discount_type    ENUM('PERCENT','AMOUNT') NOT NULL,
    discount_value   DECIMAL(12,2) NOT NULL,
    min_order_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
    start_date       DATETIME NOT NULL,
    end_date         DATETIME NOT NULL,
    usage_limit      INT,
    used_count       INT NOT NULL DEFAULT 0,
    status           ENUM('ACTIVE','INACTIVE') NOT NULL DEFAULT 'ACTIVE',
    CONSTRAINT chk_promo_value   CHECK (discount_value >= 0),
    CONSTRAINT chk_promo_date    CHECK (end_date >= start_date),
    CONSTRAINT chk_promo_used    CHECK (used_count >= 0),
    CONSTRAINT chk_promo_percent CHECK (discount_type <> 'PERCENT' OR discount_value <= 100),  -- % giảm không quá 100
    CONSTRAINT chk_promo_limit   CHECK (usage_limit IS NULL OR used_count <= usage_limit)       -- không dùng quá hạn mức
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 6. Ca làm việc ------------------------------------------------------
CREATE TABLE IF NOT EXISTS work_shifts (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id     BIGINT NOT NULL,                       -- chi nhánh mở ca (HĐ/phiếu trả thừa hưởng)
    user_id      BIGINT NOT NULL,
    opening_cash DECIMAL(12,2) NOT NULL DEFAULT 0,
    closing_cash DECIMAL(12,2),
    opened_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    closed_at    DATETIME,
    status       ENUM('OPEN','CLOSED') NOT NULL DEFAULT 'OPEN',
    KEY idx_shift_user (user_id),
    KEY idx_shift_status (status),
    KEY idx_shift_store (store_id),
    CONSTRAINT fk_shift_store FOREIGN KEY (store_id) REFERENCES stores(id),
    CONSTRAINT fk_shift_user  FOREIGN KEY (user_id)  REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- (Bảng cash_movements định nghĩa ở mục 12c bên dưới — petty cash thu/chi quỹ.)

-- 7. Hóa đơn & chi tiết ----------------------------------------------
CREATE TABLE IF NOT EXISTS invoices (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    code            VARCHAR(30) NOT NULL UNIQUE,
    store_id        BIGINT NOT NULL,                    -- chi nhánh bán (chốt từ ca: truy vấn doanh thu theo chi nhánh khỏi join)
    shift_id        BIGINT NOT NULL,
    customer_id     BIGINT,
    promotion_id    BIGINT,
    subtotal        DECIMAL(14,2) NOT NULL,
    discount_amount DECIMAL(14,2) NOT NULL DEFAULT 0,
    total_amount    DECIMAL(14,2) AS (subtotal - discount_amount) STORED,
    payment_method  ENUM('CASH','QR') NOT NULL,
    customer_paid   DECIMAL(14,2),
    change_amount   DECIMAL(14,2),
    points_earned   INT NOT NULL DEFAULT 0,
    points_used     INT NOT NULL DEFAULT 0,
    -- PENDING_PAYMENT: HĐ QR đã tạo, đang CHỜ xác nhận tiền về (giữ chỗ tồn nhưng CHƯA tính doanh thu).
    --   QR trả tiền (WEB2M/xác nhận tay) ⇒ COMPLETED; quá hạn ⇒ CANCELLED (tự hoàn tồn + điểm).
    status          ENUM('COMPLETED','CANCELLED','PENDING_PAYMENT') NOT NULL DEFAULT 'COMPLETED',
    tax_amount      DECIMAL(14,2) NOT NULL DEFAULT 0,      -- phần VAT trong tổng (giá đã gồm VAT)
    idempotency_key VARCHAR(64) UNIQUE,                    -- chống tạo HĐ trùng khi FE gửi lại do mất phản hồi
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_invoice_shift (shift_id),
    KEY idx_invoice_customer (customer_id),
    KEY idx_invoice_created (created_at),
    KEY idx_invoice_status (status, created_at),
    KEY idx_invoice_store (store_id, status, created_at),
    CONSTRAINT fk_invoice_store     FOREIGN KEY (store_id)     REFERENCES stores(id),
    CONSTRAINT fk_invoice_shift     FOREIGN KEY (shift_id)     REFERENCES work_shifts(id),
    CONSTRAINT fk_invoice_customer  FOREIGN KEY (customer_id)  REFERENCES customers(id),
    CONSTRAINT fk_invoice_promotion FOREIGN KEY (promotion_id) REFERENCES promotions(id),
    CONSTRAINT chk_invoice_amount   CHECK (subtotal >= 0 AND discount_amount >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS invoice_items (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    invoice_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity   INT NOT NULL,
    unit_price DECIMAL(12,2) NOT NULL,
    subtotal   DECIMAL(14,2) AS (quantity * unit_price) STORED,
    KEY idx_ii_invoice (invoice_id),
    KEY idx_ii_product (product_id),
    CONSTRAINT fk_ii_invoice FOREIGN KEY (invoice_id) REFERENCES invoices(id) ON DELETE CASCADE,
    CONSTRAINT fk_ii_product FOREIGN KEY (product_id) REFERENCES products(id),
    CONSTRAINT chk_ii_qty CHECK (quantity > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 8. Phân bổ tồn theo lô khi bán -------------------------------------
CREATE TABLE IF NOT EXISTS invoice_item_batches (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    invoice_item_id BIGINT NOT NULL,
    batch_id        BIGINT NOT NULL,
    quantity        INT NOT NULL,
    KEY idx_iib_item (invoice_item_id),
    KEY idx_iib_batch (batch_id),
    CONSTRAINT fk_iib_item  FOREIGN KEY (invoice_item_id) REFERENCES invoice_items(id) ON DELETE CASCADE,
    CONSTRAINT fk_iib_batch FOREIGN KEY (batch_id)        REFERENCES goods_receipt_items(id),
    CONSTRAINT chk_iib_qty  CHECK (quantity > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 8a. KỆ VẬT LÝ (Display Shelves): các kệ trưng bày trong cửa hàng (Kệ A1, Kệ 1...).
CREATE TABLE IF NOT EXISTS shelves (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id   BIGINT NOT NULL,                        -- chi nhánh đặt kệ
    code       VARCHAR(30)  NOT NULL,                  -- mã kệ: A1, B2, K01... (duy nhất TRONG chi nhánh)
    name       VARCHAR(100),                           -- tên/khu vực: "Nước giải khát"
    capacity   INT NOT NULL DEFAULT 0,                 -- sức chứa tối đa (số SP); 0 = không giới hạn
    status     ENUM('ACTIVE','INACTIVE') NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_shelf_store (store_id),
    CONSTRAINT uq_shelf_store_code UNIQUE (store_id, code),
    CONSTRAINT fk_shelf_store FOREIGN KEY (store_id) REFERENCES stores(id),
    CONSTRAINT chk_shelf_cap CHECK (capacity >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 8b. CHUYỂN HÀNG TỪ KHO LÊN KỆ (mỗi dòng = số lượng của 1 LÔ đưa lên 1 KỆ cụ thể).
--     Quy ước: một LÔ chỉ nằm trên MỘT kệ (mọi lần lên kệ của lô đó vào cùng kệ).
--     Tồn kệ của lô = đã lên kệ − đã bán; Tồn kho của lô = đã nhập − đã lên kệ.
CREATE TABLE IF NOT EXISTS shelf_transfers (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    batch_id    BIGINT NOT NULL,                       -- = goods_receipt_items.id (lô)
    shelf_id    BIGINT NOT NULL,                       -- kệ đích
    quantity    INT NOT NULL,
    created_by  BIGINT,
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_st_batch (batch_id),
    KEY idx_st_shelf (shelf_id),
    CONSTRAINT fk_st_batch FOREIGN KEY (batch_id) REFERENCES goods_receipt_items(id) ON DELETE CASCADE,
    CONSTRAINT fk_st_shelf FOREIGN KEY (shelf_id) REFERENCES shelves(id),
    CONSTRAINT fk_st_user  FOREIGN KEY (created_by) REFERENCES users(id),
    CONSTRAINT chk_st_qty  CHECK (quantity > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 8c. LẤY HÀNG TỪ KỆ VỀ KHO (đối ứng với lên kệ — "đặt lên thì có đặt xuống").
--     Mỗi dòng = số lượng của 1 LÔ trả từ kệ về kho. Tồn kệ của lô = lên kệ − trả về − bán.
CREATE TABLE IF NOT EXISTS shelf_returns (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    batch_id    BIGINT NOT NULL,                       -- = goods_receipt_items.id (lô)
    shelf_id    BIGINT NOT NULL,                       -- kệ nguồn (lấy hàng xuống từ kệ này)
    quantity    INT NOT NULL,
    created_by  BIGINT,
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_sr_batch (batch_id),
    KEY idx_sr_shelf (shelf_id),
    CONSTRAINT fk_sr_batch FOREIGN KEY (batch_id) REFERENCES goods_receipt_items(id) ON DELETE CASCADE,
    CONSTRAINT fk_sr_shelf FOREIGN KEY (shelf_id) REFERENCES shelves(id),
    CONSTRAINT fk_sr_user  FOREIGN KEY (created_by) REFERENCES users(id),
    CONSTRAINT chk_sr_qty  CHECK (quantity > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- (Đã bỏ chức năng TRẢ HÀNG / HOÀN TIỀN: cửa hàng tiện lợi không nhận trả hàng.
--  Hủy nhầm hóa đơn dùng HỦY HĐ — tồn tự hoàn qua view; không còn bảng sales_returns.)

-- 9. Giao dịch thanh toán QR -----------------------------------------
CREATE TABLE IF NOT EXISTS payment_transactions (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    invoice_id       BIGINT NOT NULL,
    amount           DECIMAL(14,2) NOT NULL,
    transfer_content VARCHAR(50) NOT NULL UNIQUE,
    status           ENUM('PENDING','PAID','EXPIRED','FAILED') NOT NULL DEFAULT 'PENDING',
    bank_reference   VARCHAR(100),
    paid_at          DATETIME,
    expired_at       DATETIME,
    created_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_payment_invoice (invoice_id),
    KEY idx_payment_status (status, expired_at),
    CONSTRAINT fk_payment_invoice FOREIGN KEY (invoice_id) REFERENCES invoices(id) ON DELETE CASCADE,
    CONSTRAINT chk_payment_amount CHECK (amount >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 10. Cấu hình TỪNG CHI NHÁNH (đa chuỗi) — id = stores.id (1–1 chia sẻ khóa) ----
CREATE TABLE IF NOT EXISTS store_config (
    id                 BIGINT PRIMARY KEY,             -- = stores.id (KHÔNG auto-increment)
    name               VARCHAR(150) NOT NULL,
    address            VARCHAR(255),
    phone              VARCHAR(20),
    tax_code           VARCHAR(30),
    logo_url           VARCHAR(255),
    bank_name          VARCHAR(50),
    bank_bin           VARCHAR(20),
    bank_account_no    VARCHAR(30),
    bank_account_name  VARCHAR(100),
    transfer_prefix    VARCHAR(20),
    web2m_api_url      VARCHAR(255),
    telegram_bot_token VARCHAR(255),
    telegram_enabled   TINYINT(1) NOT NULL DEFAULT 0,
    notify_payment     TINYINT(1) NOT NULL DEFAULT 1,
    notify_low_stock   TINYINT(1) NOT NULL DEFAULT 1,
    notify_new_invoice TINYINT(1) NOT NULL DEFAULT 0,
    updated_at         DATETIME,
    CONSTRAINT fk_config_store FOREIGN KEY (id) REFERENCES stores(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 11. Người nhận thông báo Telegram (theo chi nhánh) -----------------
CREATE TABLE IF NOT EXISTS telegram_recipients (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    config_id BIGINT NOT NULL,                          -- = store_config.id = stores.id
    chat_id   VARCHAR(50) NOT NULL,
    label     VARCHAR(100),
    is_active TINYINT(1) NOT NULL DEFAULT 1,
    CONSTRAINT uq_tele_store_chat UNIQUE (config_id, chat_id),
    CONSTRAINT fk_tele_config FOREIGN KEY (config_id) REFERENCES store_config(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 12. NHẬT KÝ KIỂM TOÁN (audit log) — vết "ai làm gì, khi nào" cho hành động nhạy cảm
--     (hủy hóa đơn, đổi giá, chốt quỹ ca, đổi quyền/mật khẩu, đổi cấu hình). Chỉ ghi thêm (append-only).
CREATE TABLE IF NOT EXISTS audit_logs (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    actor_user_id  BIGINT,                                -- ai thực hiện
    actor_username VARCHAR(50),                           -- chốt tên đăng nhập tại thời điểm (khỏi join)
    store_id       BIGINT,                                -- chi nhánh phát sinh thao tác (NULL = toàn chuỗi)
    action         VARCHAR(60) NOT NULL,                  -- vd CANCEL_INVOICE, CHANGE_PRICE, CLOSE_SHIFT
    target_type    VARCHAR(40),                           -- vd INVOICE, PRODUCT, SHIFT
    target_id      BIGINT,
    detail         VARCHAR(500),                          -- mô tả/chênh lệch (vd lý do hủy)
    created_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_audit_created (created_at),
    KEY idx_audit_target (target_type, target_id),
    KEY idx_audit_actor (actor_user_id),
    KEY idx_audit_store_created (store_id, created_at),
    CONSTRAINT fk_audit_user  FOREIGN KEY (actor_user_id) REFERENCES users(id),
    CONSTRAINT fk_audit_store FOREIGN KEY (store_id)      REFERENCES stores(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 12b. XUẤT HỦY / ĐIỀU CHỈNH GIẢM TỒN (FR8 — kiểm kê & hao hụt): rút hàng hết hạn/hư hỏng/thất thoát
--      khỏi tồn KHO của một LÔ. Mỗi dòng GIẢM `quantity` đơn vị (append-only). v_batch_stock trừ tổng này.
CREATE TABLE IF NOT EXISTS stock_adjustments (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id   BIGINT NOT NULL,                            -- chi nhánh phát sinh (= chi nhánh của lô)
    batch_id   BIGINT NOT NULL,                            -- = goods_receipt_items.id (lô bị giảm tồn)
    quantity   INT NOT NULL,                               -- số lượng GIẢM (dương)
    reason     ENUM('EXPIRED','DAMAGED','LOST','OTHER') NOT NULL,  -- hết hạn / hư hỏng / thất thoát / khác
    note       VARCHAR(255),
    created_by BIGINT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_adj_store (store_id),
    KEY idx_adj_batch (batch_id),
    CONSTRAINT fk_adj_store FOREIGN KEY (store_id)   REFERENCES stores(id),
    CONSTRAINT fk_adj_batch FOREIGN KEY (batch_id)   REFERENCES goods_receipt_items(id),
    CONSTRAINT fk_adj_user  FOREIGN KEY (created_by) REFERENCES users(id),
    CONSTRAINT chk_adj_qty  CHECK (quantity > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 12c. THU/CHI TIỀN MẶT NGOÀI BÁN HÀNG trong ca (petty cash). Append-only. Đối soát quỹ cuối ca:
--      tiền dự kiến = đầu ca + tiền mặt bán + SUM(IN) − SUM(OUT).
CREATE TABLE IF NOT EXISTS cash_movements (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    shift_id   BIGINT NOT NULL,                            -- ca phát sinh
    store_id   BIGINT NOT NULL,                            -- chi nhánh (= chi nhánh của ca)
    type       ENUM('IN','OUT') NOT NULL,                  -- THU vào / CHI ra khỏi két
    amount     DECIMAL(14,2) NOT NULL,                     -- số tiền (dương)
    reason     VARCHAR(255) NOT NULL,                      -- lý do/diễn giải (bắt buộc)
    created_by BIGINT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_cash_shift (shift_id),
    KEY idx_cash_store (store_id),
    CONSTRAINT fk_cash_shift FOREIGN KEY (shift_id)   REFERENCES work_shifts(id),
    CONSTRAINT fk_cash_store FOREIGN KEY (store_id)   REFERENCES stores(id),
    CONSTRAINT fk_cash_user  FOREIGN KEY (created_by) REFERENCES users(id),
    CONSTRAINT chk_cash_amount CHECK (amount > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 13. SỔ CÁI ĐIỂM TÍCH LŨY (loyalty ledger) — mỗi thay đổi điểm là 1 dòng (append-only) để truy vết,
--     đối soát số dư = tổng delta. delta>0 tích, delta<0 dùng/điều chỉnh.
CREATE TABLE IF NOT EXISTS loyalty_point_ledger (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id   BIGINT NOT NULL,
    invoice_id    BIGINT,                                -- HĐ phát sinh (NULL nếu điều chỉnh tay)
    delta         INT NOT NULL,                          -- +tích / −dùng / ±điều chỉnh
    reason        VARCHAR(40) NOT NULL,                  -- EARN, REDEEM, CANCEL_REVERSAL...
    balance_after INT NOT NULL,                          -- số dư sau thay đổi (đối soát)
    created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_lpl_customer (customer_id),
    KEY idx_lpl_invoice (invoice_id),
    CONSTRAINT fk_lpl_customer FOREIGN KEY (customer_id) REFERENCES customers(id),
    CONSTRAINT fk_lpl_invoice  FOREIGN KEY (invoice_id)  REFERENCES invoices(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 14. GIÁ BÁN RIÊNG THEO CHI NHÁNH (Obj 1.3) — giữ mô hình giá TẬP TRUNG (products.sale_price là giá chuẩn),
--     mỗi (sản phẩm, chi nhánh) có TỐI ĐA 1 override ACTIVE. Giá bán = COALESCE(override, giá chuẩn).
CREATE TABLE IF NOT EXISTS product_store_prices (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id  BIGINT NOT NULL,
    store_id    BIGINT NOT NULL,
    sale_price  DECIMAL(12,2) NOT NULL,                     -- giá bán riêng (đã gồm VAT)
    status      ENUM('ACTIVE','INACTIVE') NOT NULL DEFAULT 'ACTIVE',
    updated_by  BIGINT,
    updated_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uq_psp UNIQUE (product_id, store_id),        -- 1 override / (sản phẩm, chi nhánh)
    CONSTRAINT fk_psp_product FOREIGN KEY (product_id) REFERENCES products(id),
    CONSTRAINT fk_psp_store   FOREIGN KEY (store_id)   REFERENCES stores(id),
    CONSTRAINT fk_psp_user    FOREIGN KEY (updated_by) REFERENCES users(id),
    CONSTRAINT chk_psp_price  CHECK (sale_price >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 15. ĐIỀU CHUYỂN HÀNG NỘI BỘ giữa chi nhánh (Obj 1.1) — state machine PENDING→SHIPPING→RECEIVED/CANCELLED.
--     KHÔNG mutate lô: SHIP ghi stock_adjustments(reason=TRANSFER_OUT) trừ tồn nguồn; RECEIVE tạo
--     goods_receipts(source=TRANSFER) ở đích → tồn xuất hiện ở đích như lô mới (giữ FEFO theo HSD gốc).
CREATE TABLE IF NOT EXISTS stock_transfers (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    code            VARCHAR(30) NOT NULL UNIQUE,
    source_store_id BIGINT NOT NULL,
    dest_store_id   BIGINT NOT NULL,
    status          ENUM('PENDING','SHIPPING','RECEIVED','CANCELLED') NOT NULL DEFAULT 'PENDING',
    created_by      BIGINT,
    shipped_by      BIGINT, shipped_at  DATETIME,
    received_by     BIGINT, received_at DATETIME,
    cancelled_by    BIGINT, cancelled_at DATETIME, cancel_reason VARCHAR(255),
    dest_receipt_id BIGINT,                                 -- phiếu nhập sinh ở đích khi RECEIVED (truy vết)
    note            VARCHAR(255),
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_tr_src (source_store_id),
    KEY idx_tr_dst (dest_store_id),
    KEY idx_tr_status (status),
    CONSTRAINT fk_tr_src  FOREIGN KEY (source_store_id) REFERENCES stores(id),
    CONSTRAINT fk_tr_dst  FOREIGN KEY (dest_store_id)   REFERENCES stores(id),
    CONSTRAINT fk_tr_rcpt FOREIGN KEY (dest_receipt_id) REFERENCES goods_receipts(id),
    CONSTRAINT fk_tr_cby  FOREIGN KEY (created_by)  REFERENCES users(id),
    CONSTRAINT fk_tr_sby  FOREIGN KEY (shipped_by)  REFERENCES users(id),
    CONSTRAINT fk_tr_rby  FOREIGN KEY (received_by) REFERENCES users(id),
    CONSTRAINT chk_tr_diff CHECK (source_store_id <> dest_store_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS stock_transfer_items (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    transfer_id  BIGINT NOT NULL,
    batch_id     BIGINT NOT NULL,                           -- lô NGUỒN (goods_receipt_items.id)
    product_id   BIGINT NOT NULL,
    quantity     INT NOT NULL,
    expiry_date  DATE,                                      -- HSD chốt từ lô nguồn → tái tạo ở đích
    cost_price   DECIMAL(12,2) NOT NULL,                    -- giá vốn theo lô (chuyển kho không đổi giá vốn)
    CONSTRAINT fk_tri_tr    FOREIGN KEY (transfer_id) REFERENCES stock_transfers(id) ON DELETE CASCADE,
    CONSTRAINT fk_tri_batch FOREIGN KEY (batch_id)    REFERENCES goods_receipt_items(id),
    CONSTRAINT fk_tri_prod  FOREIGN KEY (product_id)  REFERENCES products(id),
    CONSTRAINT chk_tri_qty  CHECK (quantity > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 16. BẢNG TỔNG HỢP DOANH THU NGÀY (Obj 2 — rollup) — 1 dòng / (chi nhánh, ngày). Job nền tổng hợp
--     từ invoices COMPLETED để báo cáo nhiều năm × nhiều chi nhánh không phải quét bảng hóa đơn thô.
CREATE TABLE IF NOT EXISTS daily_sales_rollup (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id      BIGINT NOT NULL,
    sales_date    DATE   NOT NULL,
    revenue       DECIMAL(16,2) NOT NULL DEFAULT 0,         -- Σ total_amount (COMPLETED)
    discount      DECIMAL(16,2) NOT NULL DEFAULT 0,
    tax           DECIMAL(16,2) NOT NULL DEFAULT 0,
    cogs          DECIMAL(16,2) NOT NULL DEFAULT 0,         -- giá vốn đích danh theo lô của hàng đã bán
    gross_profit  DECIMAL(16,2) NOT NULL DEFAULT 0,         -- revenue − cogs
    invoice_count INT NOT NULL DEFAULT 0,
    items_sold    INT NOT NULL DEFAULT 0,
    rolled_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uq_dsr UNIQUE (store_id, sales_date),
    KEY idx_dsr_date (sales_date),
    CONSTRAINT fk_dsr_store FOREIGN KEY (store_id) REFERENCES stores(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 16. LƯƠNG & BẢNG CÔNG (Payroll) — công suy ra từ work_shifts (ca ĐÃ ĐÓNG). Xem docs/PAYROLL_DESIGN.md
-- 16a. Cấu hình lương / nhân viên (1 dòng/nhân viên — cấu hình hiện hành; đổi mức ghi audit_logs).
CREATE TABLE IF NOT EXISTS employee_pay_profiles (
    id                     BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id                BIGINT NOT NULL,
    pay_type               ENUM('HOURLY','MONTHLY') NOT NULL DEFAULT 'MONTHLY',
    base_rate              DECIMAL(12,2) NOT NULL DEFAULT 0,    -- HOURLY: đ/giờ · MONTHLY: đ/tháng
    standard_monthly_hours DECIMAL(6,2)  NOT NULL DEFAULT 208,  -- công chuẩn/tháng (26 ngày × 8h)
    ot_multiplier          DECIMAL(4,2)  NOT NULL DEFAULT 1.50, -- hệ số tăng ca
    monthly_allowance      DECIMAL(12,2) NOT NULL DEFAULT 0,    -- phụ cấp cố định/tháng
    updated_by             BIGINT,
    updated_at             DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uq_epp_user UNIQUE (user_id),                    -- 1 cấu hình / nhân viên
    CONSTRAINT fk_epp_user FOREIGN KEY (user_id)    REFERENCES users(id),
    CONSTRAINT fk_epp_upd  FOREIGN KEY (updated_by) REFERENCES users(id),
    CONSTRAINT chk_epp_rate CHECK (base_rate >= 0 AND standard_monthly_hours > 0 AND ot_multiplier >= 1)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 16b. Kỳ lương: 1 / (chi nhánh, tháng). Vòng đời duyệt 2 bước DRAFT→PENDING_APPROVAL→APPROVED→PAID.
CREATE TABLE IF NOT EXISTS payroll_periods (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id     BIGINT  NOT NULL,
    period_month CHAR(7) NOT NULL,                              -- 'YYYY-MM'
    status       ENUM('DRAFT','PENDING_APPROVAL','APPROVED','PAID') NOT NULL DEFAULT 'DRAFT',
    note         VARCHAR(255),
    created_by   BIGINT,
    created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    submitted_by BIGINT,                                        -- người lập trình duyệt
    submitted_at DATETIME,
    approved_by  BIGINT,                                        -- người duyệt (tách trách nhiệm)
    approved_at  DATETIME,
    paid_at      DATETIME,
    CONSTRAINT uq_pp UNIQUE (store_id, period_month),           -- 1 kỳ / chi nhánh / tháng
    CONSTRAINT fk_pp_store FOREIGN KEY (store_id)   REFERENCES stores(id),
    CONSTRAINT fk_pp_user  FOREIGN KEY (created_by) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- MIGRATION (idempotent) cho CSDL CŨ có payroll_periods kiểu DRAFT→LOCKED→PAID:
--   thêm cột duyệt 2 bước + mở rộng enum trạng thái + chuyển 'LOCKED' → 'APPROVED'.
SET @pp_add_cols := (SELECT IF(
    EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE()
           AND table_name = 'payroll_periods' AND column_name = 'submitted_by'),
    'SELECT 1',
    'ALTER TABLE payroll_periods ADD COLUMN submitted_by BIGINT NULL, ADD COLUMN submitted_at DATETIME NULL, ADD COLUMN approved_by BIGINT NULL, ADD COLUMN approved_at DATETIME NULL'));
PREPARE pp1 FROM @pp_add_cols; EXECUTE pp1; DEALLOCATE PREPARE pp1;
ALTER TABLE payroll_periods MODIFY COLUMN status
    ENUM('DRAFT','PENDING_APPROVAL','APPROVED','PAID','LOCKED') NOT NULL DEFAULT 'DRAFT';
UPDATE payroll_periods SET status = 'APPROVED' WHERE status = 'LOCKED';
ALTER TABLE payroll_periods MODIFY COLUMN status
    ENUM('DRAFT','PENDING_APPROVAL','APPROVED','PAID') NOT NULL DEFAULT 'DRAFT';

-- 16c. Phiếu lương: 1 / (kỳ, nhân viên). SNAPSHOT mọi số liệu → khóa kỳ là cố định.
CREATE TABLE IF NOT EXISTS payslips (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    period_id       BIGINT NOT NULL,
    user_id         BIGINT NOT NULL,
    pay_type        ENUM('HOURLY','MONTHLY') NOT NULL,
    base_rate       DECIMAL(12,2) NOT NULL,
    standard_hours  DECIMAL(8,2)  NOT NULL,
    worked_hours    DECIMAL(8,2)  NOT NULL DEFAULT 0,           -- Σ giờ công ca đã đóng
    regular_hours   DECIMAL(8,2)  NOT NULL DEFAULT 0,
    ot_hours        DECIMAL(8,2)  NOT NULL DEFAULT 0,
    shift_count     INT NOT NULL DEFAULT 0,
    regular_pay     DECIMAL(14,2) NOT NULL DEFAULT 0,
    ot_pay          DECIMAL(14,2) NOT NULL DEFAULT 0,
    allowance       DECIMAL(14,2) NOT NULL DEFAULT 0,
    gross_pay       DECIMAL(14,2) NOT NULL DEFAULT 0,           -- regular + ot + allowance
    total_bonus     DECIMAL(14,2) NOT NULL DEFAULT 0,
    total_deduction DECIMAL(14,2) NOT NULL DEFAULT 0,
    net_pay         DECIMAL(14,2) NOT NULL DEFAULT 0,           -- gross + bonus − deduction (thực lĩnh)
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uq_ps UNIQUE (period_id, user_id),
    CONSTRAINT fk_ps_period FOREIGN KEY (period_id) REFERENCES payroll_periods(id),
    CONSTRAINT fk_ps_user   FOREIGN KEY (user_id)   REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 16d. Điều chỉnh phiếu lương: thưởng/phạt/tạm ứng (cộng/trừ vào thực lĩnh).
CREATE TABLE IF NOT EXISTS payslip_adjustments (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    payslip_id  BIGINT NOT NULL,
    type        ENUM('BONUS','DEDUCTION') NOT NULL,
    amount      DECIMAL(14,2) NOT NULL,
    reason      VARCHAR(255) NOT NULL,
    created_by  BIGINT,
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_pa_payslip (payslip_id),
    CONSTRAINT fk_pa_payslip FOREIGN KEY (payslip_id) REFERENCES payslips(id),
    CONSTRAINT fk_pa_user    FOREIGN KEY (created_by)  REFERENCES users(id),
    CONSTRAINT chk_pa_amount CHECK (amount > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 16e. CHẤM CÔNG THỦ CÔNG & NGHỈ PHÉP — bổ sung công NGOÀI ca thu ngân (NV kho/bảo vệ không mở ca,
--      sửa công, nghỉ phép). Payroll cộng giờ WORK + LEAVE_PAID vào giờ công khi tính lương.
CREATE TABLE IF NOT EXISTS attendance_entries (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT NOT NULL,
    store_id   BIGINT NOT NULL,
    work_date  DATE NOT NULL,
    type       ENUM('WORK','LEAVE_PAID','LEAVE_UNPAID') NOT NULL DEFAULT 'WORK',
    hours      DECIMAL(5,2) NOT NULL,                  -- WORK/LEAVE_PAID: tính lương; LEAVE_UNPAID: chỉ ghi nhận
    reason     VARCHAR(255),
    created_by BIGINT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_att_user_date (user_id, work_date),
    KEY idx_att_store_date (store_id, work_date),
    CONSTRAINT fk_att_user  FOREIGN KEY (user_id)    REFERENCES users(id),
    CONSTRAINT fk_att_store FOREIGN KEY (store_id)   REFERENCES stores(id),
    CONSTRAINT fk_att_cb    FOREIGN KEY (created_by) REFERENCES users(id),
    CONSTRAINT chk_att_hours CHECK (hours > 0 AND hours <= 24)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =====================================================================
--  MIGRATION nhẹ cho CSDL CŨ (idempotent): thêm cột points_used nếu thiếu.
--  (MySQL không có ADD COLUMN IF NOT EXISTS → kiểm tra qua information_schema.)
-- =====================================================================
SET @add_points_used := (SELECT IF(
    EXISTS(SELECT 1 FROM information_schema.columns
           WHERE table_schema = DATABASE() AND table_name = 'invoices' AND column_name = 'points_used'),
    'SELECT 1',
    'ALTER TABLE invoices ADD COLUMN points_used INT NOT NULL DEFAULT 0 AFTER points_earned'));
PREPARE stmt_apu FROM @add_points_used;
EXECUTE stmt_apu;
DEALLOCATE PREPARE stmt_apu;

-- Cột HỦY hóa đơn (ai/khi nào/lý do) — bổ sung cho CSDL cũ nếu thiếu.
SET @add_cancelled_by := (SELECT IF(
    EXISTS(SELECT 1 FROM information_schema.columns
           WHERE table_schema = DATABASE() AND table_name = 'invoices' AND column_name = 'cancelled_by'),
    'SELECT 1',
    'ALTER TABLE invoices ADD COLUMN cancelled_by BIGINT NULL, ADD COLUMN cancelled_at DATETIME NULL, ADD COLUMN cancel_reason VARCHAR(255) NULL'));
PREPARE stmt_acb FROM @add_cancelled_by;
EXECUTE stmt_acb;
DEALLOCATE PREPARE stmt_acb;

-- Cột khóa chống trùng (idempotency) — bổ sung cho CSDL cũ nếu thiếu.
SET @add_idem := (SELECT IF(
    EXISTS(SELECT 1 FROM information_schema.columns
           WHERE table_schema = DATABASE() AND table_name = 'invoices' AND column_name = 'idempotency_key'),
    'SELECT 1',
    'ALTER TABLE invoices ADD COLUMN idempotency_key VARCHAR(64) NULL UNIQUE'));
PREPARE stmt_idem FROM @add_idem;
EXECUTE stmt_idem;
DEALLOCATE PREPARE stmt_idem;

-- Cột VAT: products.tax_rate & invoices.tax_amount — bổ sung cho CSDL cũ nếu thiếu.
SET @add_taxrate := (SELECT IF(
    EXISTS(SELECT 1 FROM information_schema.columns
           WHERE table_schema = DATABASE() AND table_name = 'products' AND column_name = 'tax_rate'),
    'SELECT 1',
    'ALTER TABLE products ADD COLUMN tax_rate DECIMAL(5,2) NOT NULL DEFAULT 8.00 AFTER sale_price'));
PREPARE stmt_tr FROM @add_taxrate; EXECUTE stmt_tr; DEALLOCATE PREPARE stmt_tr;

SET @add_taxamt := (SELECT IF(
    EXISTS(SELECT 1 FROM information_schema.columns
           WHERE table_schema = DATABASE() AND table_name = 'invoices' AND column_name = 'tax_amount'),
    'SELECT 1',
    'ALTER TABLE invoices ADD COLUMN tax_amount DECIMAL(14,2) NOT NULL DEFAULT 0 AFTER status'));
PREPARE stmt_ta FROM @add_taxamt; EXECUTE stmt_ta; DEALLOCATE PREPARE stmt_ta;

-- Cột đơn vị quy đổi (thùng↔lon): products.pack_size & pack_unit_id — bổ sung cho CSDL cũ nếu thiếu.
SET @add_pack := (SELECT IF(
    EXISTS(SELECT 1 FROM information_schema.columns
           WHERE table_schema = DATABASE() AND table_name = 'products' AND column_name = 'pack_size'),
    'SELECT 1',
    'ALTER TABLE products ADD COLUMN pack_size INT NOT NULL DEFAULT 1 AFTER tax_rate, ADD COLUMN pack_unit_id BIGINT NULL AFTER pack_size'));
PREPARE stmt_pk FROM @add_pack; EXECUTE stmt_pk; DEALLOCATE PREPARE stmt_pk;

-- CSDL cũ có bảng trả hàng → dọn bỏ (đã loại chức năng trả hàng). Bỏ qua nếu bảng không tồn tại.
DROP TABLE IF EXISTS sales_return_items;
DROP TABLE IF EXISTS sales_returns;

-- Mở rộng enum trạng thái hóa đơn: thêm PENDING_PAYMENT (HĐ QR chờ xác nhận tiền) cho CSDL cũ.
-- MODIFY COLUMN an toàn chạy lại nhiều lần (idempotent về kết quả).
ALTER TABLE invoices
    MODIFY COLUMN status ENUM('COMPLETED','CANCELLED','PENDING_PAYMENT') NOT NULL DEFAULT 'COMPLETED';

-- Obj 1.1: kho trung tâm = 1 chi nhánh đặc biệt (stores.is_warehouse) + nguồn gốc phiếu nhập
-- (goods_receipts.source: PURCHASE mua ngoài / TRANSFER nhận điều chuyển). Bổ sung cho CSDL cũ.
SET @add_is_wh := (SELECT IF(
    EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='stores' AND column_name='is_warehouse'),
    'SELECT 1',
    'ALTER TABLE stores ADD COLUMN is_warehouse TINYINT(1) NOT NULL DEFAULT 0'));
PREPARE stmt_iswh FROM @add_is_wh; EXECUTE stmt_iswh; DEALLOCATE PREPARE stmt_iswh;

SET @add_gr_src := (SELECT IF(
    EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='goods_receipts' AND column_name='source'),
    'SELECT 1',
    'ALTER TABLE goods_receipts ADD COLUMN source ENUM(''PURCHASE'',''TRANSFER'') NOT NULL DEFAULT ''PURCHASE'''));
PREPARE stmt_grsrc FROM @add_gr_src; EXECUTE stmt_grsrc; DEALLOCATE PREPARE stmt_grsrc;

-- Mở rộng lý do điều chỉnh tồn: thêm TRANSFER_OUT (xuất điều chuyển). MODIFY idempotent về kết quả.
ALTER TABLE stock_adjustments
    MODIFY COLUMN reason ENUM('EXPIRED','DAMAGED','LOST','OTHER','TRANSFER_OUT') NOT NULL;

-- Phiếu nhập do điều chuyển nội bộ không có NCC → cho supplier_id nullable. MODIFY idempotent.
ALTER TABLE goods_receipts MODIFY COLUMN supplier_id BIGINT NULL;

-- SNAPSHOT đối soát quỹ lúc ĐÓNG CA (finding #3): chốt tiền-mặt-bán tại thời điểm đóng để hủy HĐ tiền mặt
-- của ca ĐÃ ĐÓNG về sau KHÔNG làm lệch đối soát quỹ của ca đó (tiền thực tế ĐÃ nằm trong két lúc đóng).
SET @add_fcs := (SELECT IF(
    EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='work_shifts' AND column_name='final_cash_sales'),
    'SELECT 1',
    'ALTER TABLE work_shifts ADD COLUMN final_cash_sales DECIMAL(14,2) NULL'));
PREPARE stmt_fcs FROM @add_fcs; EXECUTE stmt_fcs; DEALLOCATE PREPARE stmt_fcs;

-- Backfill các ca ĐÃ ĐÓNG còn thiếu snapshot (idempotent: chỉ đụng dòng NULL).
-- Nguồn sự thật của quy tắc doanh thu tiền mặt theo ca: InvoiceRepository.cashSalesByShiftIds
UPDATE work_shifts s
LEFT JOIN ( SELECT shift_id, SUM(total_amount) cash FROM invoices
            WHERE status='COMPLETED' AND payment_method='CASH' GROUP BY shift_id ) cs ON cs.shift_id = s.id
SET s.final_cash_sales = COALESCE(cs.cash, 0)
WHERE s.status='CLOSED' AND s.final_cash_sales IS NULL;

-- =====================================================================
--  MIGRATION ĐA CHUỖI (idempotent): thêm chi nhánh mặc định CH01 (id=1) và cột store_id
--  cho CSDL CŨ (single-store) — backfill toàn bộ dữ liệu cũ về chi nhánh 1.
--  Bảng tạo mới (CREATE TABLE ở trên) đã có sẵn cột nên các ALTER này chỉ chạy cho DB cũ.
-- =====================================================================
-- Chi nhánh mặc định (dữ liệu cũ thuộc về đây). INSERT IGNORE: chạy lại không nhân đôi.
INSERT IGNORE INTO stores (id, code, name, address, phone, status)
VALUES (1, 'CH01', 'Cửa hàng tiện lợi MiniMart', '123 Đường Lê Lợi, Q.1, TP.HCM', '0901234567', 'ACTIVE');

-- users.store_id cho CSDL cũ (NULL hợp lệ cho ADMIN toàn chuỗi).
SET @add_user_store := (SELECT IF(
    EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='users' AND column_name='store_id'),
    'SELECT 1',
    'ALTER TABLE users ADD COLUMN store_id BIGINT NULL, ADD KEY idx_user_store (store_id), ADD CONSTRAINT fk_user_store FOREIGN KEY (store_id) REFERENCES stores(id)'));
PREPARE s FROM @add_user_store; EXECUTE s; DEALLOCATE PREPARE s;

-- CHUYỂN ĐỔI VAI TRÒ về mô hình mới (ADMIN/MANAGER/STAFF) cho CSDL cũ — làm theo 3 bước an toàn:
--  1) nới enum thành SIÊU TẬP (cũ ∪ mới) để UPDATE giá trị mới hợp lệ;
--  2) đổi CASHIER→STAFF, CHAIN_ADMIN→ADMIN; backfill cửa hàng (MANAGER/STAFF→CH01, ADMIN→NULL);
--  3) thu enum về tập CUỐI CÙNG. Idempotent: chạy lại không còn dòng cũ nên các UPDATE là no-op.
ALTER TABLE users MODIFY COLUMN role ENUM('ADMIN','MANAGER','STAFF','CASHIER','CHAIN_ADMIN') NOT NULL;
UPDATE users SET role = 'STAFF' WHERE role = 'CASHIER';
UPDATE users SET role = 'ADMIN' WHERE role = 'CHAIN_ADMIN';
UPDATE users SET store_id = 1 WHERE store_id IS NULL AND role <> 'ADMIN';
UPDATE users SET store_id = NULL WHERE role = 'ADMIN';
ALTER TABLE users MODIFY COLUMN role ENUM('ADMIN','MANAGER','STAFF') NOT NULL;

-- goods_receipts.store_id (NOT NULL sau backfill).
SET @add_gr_store := (SELECT IF(
    EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='goods_receipts' AND column_name='store_id'),
    'SELECT 1',
    'ALTER TABLE goods_receipts ADD COLUMN store_id BIGINT NULL, ADD KEY idx_receipt_store (store_id), ADD CONSTRAINT fk_receipt_store FOREIGN KEY (store_id) REFERENCES stores(id)'));
PREPARE s FROM @add_gr_store; EXECUTE s; DEALLOCATE PREPARE s;
UPDATE goods_receipts SET store_id = 1 WHERE store_id IS NULL;

-- work_shifts.store_id.
SET @add_ws_store := (SELECT IF(
    EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='work_shifts' AND column_name='store_id'),
    'SELECT 1',
    'ALTER TABLE work_shifts ADD COLUMN store_id BIGINT NULL, ADD KEY idx_shift_store (store_id), ADD CONSTRAINT fk_shift_store FOREIGN KEY (store_id) REFERENCES stores(id)'));
PREPARE s FROM @add_ws_store; EXECUTE s; DEALLOCATE PREPARE s;
UPDATE work_shifts SET store_id = 1 WHERE store_id IS NULL;

-- shelves.store_id (mã kệ cũ duy nhất toàn cục vẫn hợp lệ trong 1 chi nhánh).
SET @add_sh_store := (SELECT IF(
    EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='shelves' AND column_name='store_id'),
    'SELECT 1',
    'ALTER TABLE shelves ADD COLUMN store_id BIGINT NULL, ADD KEY idx_shelf_store (store_id), ADD CONSTRAINT fk_shelf_store FOREIGN KEY (store_id) REFERENCES stores(id)'));
PREPARE s FROM @add_sh_store; EXECUTE s; DEALLOCATE PREPARE s;
UPDATE shelves SET store_id = 1 WHERE store_id IS NULL;

-- invoices.store_id (chốt từ ca: = ca.store_id).
SET @add_inv_store := (SELECT IF(
    EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='invoices' AND column_name='store_id'),
    'SELECT 1',
    'ALTER TABLE invoices ADD COLUMN store_id BIGINT NULL, ADD KEY idx_invoice_store (store_id, status, created_at), ADD CONSTRAINT fk_invoice_store FOREIGN KEY (store_id) REFERENCES stores(id)'));
PREPARE s FROM @add_inv_store; EXECUTE s; DEALLOCATE PREPARE s;
UPDATE invoices i JOIN work_shifts w ON w.id = i.shift_id SET i.store_id = w.store_id WHERE i.store_id IS NULL;

-- Sau khi backfill xong: siết NOT NULL cho các bảng vận hành (users để NULL vì ADMIN toàn chuỗi).
-- An toàn chạy lại; chỉ thành công khi không còn dòng store_id NULL (đã backfill ở trên).
ALTER TABLE goods_receipts MODIFY COLUMN store_id BIGINT NOT NULL;
ALTER TABLE work_shifts    MODIFY COLUMN store_id BIGINT NOT NULL;
ALTER TABLE shelves        MODIFY COLUMN store_id BIGINT NOT NULL;
ALTER TABLE invoices       MODIFY COLUMN store_id BIGINT NOT NULL;

-- store_config: chuyển từ singleton (TINYINT id=1, CHECK id=1) sang 1 dòng / chi nhánh (BIGINT = stores.id).
-- Bỏ CHECK cũ nếu còn (tên ràng buộc chk_store_singleton). Bỏ qua lỗi nếu không tồn tại.
SET @drop_chk := (SELECT IF(
    EXISTS(SELECT 1 FROM information_schema.table_constraints
           WHERE table_schema=DATABASE() AND table_name='store_config' AND constraint_name='chk_store_singleton'),
    'ALTER TABLE store_config DROP CHECK chk_store_singleton',
    'SELECT 1'));
PREPARE s FROM @drop_chk; EXECUTE s; DEALLOCATE PREPARE s;
-- Nới kiểu id & config_id lên BIGINT cho CSDL cũ (an toàn chạy lại). Bỏ FK telegram trước khi đổi kiểu.
SET @drop_tele_fk := (SELECT IF(
    EXISTS(SELECT 1 FROM information_schema.table_constraints
           WHERE table_schema=DATABASE() AND table_name='telegram_recipients' AND constraint_name='fk_tele_config'),
    'ALTER TABLE telegram_recipients DROP FOREIGN KEY fk_tele_config',
    'SELECT 1'));
PREPARE s FROM @drop_tele_fk; EXECUTE s; DEALLOCATE PREPARE s;
ALTER TABLE store_config        MODIFY COLUMN id BIGINT NOT NULL;
ALTER TABLE telegram_recipients MODIFY COLUMN config_id BIGINT NOT NULL;
-- store_config.id giờ = stores.id → ràng buộc khóa ngoại (chỉ thêm nếu chưa có).
SET @add_cfg_fk := (SELECT IF(
    EXISTS(SELECT 1 FROM information_schema.table_constraints
           WHERE table_schema=DATABASE() AND table_name='store_config' AND constraint_name='fk_config_store'),
    'SELECT 1',
    'ALTER TABLE store_config ADD CONSTRAINT fk_config_store FOREIGN KEY (id) REFERENCES stores(id)'));
PREPARE s FROM @add_cfg_fk; EXECUTE s; DEALLOCATE PREPARE s;
-- Khôi phục FK telegram→config sau khi đổi kiểu (chỉ thêm nếu chưa có).
SET @readd_tele_fk := (SELECT IF(
    EXISTS(SELECT 1 FROM information_schema.table_constraints
           WHERE table_schema=DATABASE() AND table_name='telegram_recipients' AND constraint_name='fk_tele_config'),
    'SELECT 1',
    'ALTER TABLE telegram_recipients ADD CONSTRAINT fk_tele_config FOREIGN KEY (config_id) REFERENCES store_config(id)'));
PREPARE s FROM @readd_tele_fk; EXECUTE s; DEALLOCATE PREPARE s;

-- =====================================================================
--  MIGRATION ràng buộc toàn vẹn bổ sung cho CSDL CŨ (idempotent): chỉ thêm khi CHƯA có.
--  (Bản cài mới đã có sẵn trong CREATE TABLE ở trên.)
-- =====================================================================
-- CHECK: % giảm ≤ 100 (chỉ áp cho PERCENT).
SET @add_chk_pct := (SELECT IF(
    EXISTS(SELECT 1 FROM information_schema.table_constraints
           WHERE table_schema=DATABASE() AND table_name='promotions' AND constraint_name='chk_promo_percent'),
    'SELECT 1',
    'ALTER TABLE promotions ADD CONSTRAINT chk_promo_percent CHECK (discount_type <> ''PERCENT'' OR discount_value <= 100)'));
PREPARE s FROM @add_chk_pct; EXECUTE s; DEALLOCATE PREPARE s;

-- CHECK: số lần dùng ≤ hạn mức (nếu có hạn mức).
SET @add_chk_lim := (SELECT IF(
    EXISTS(SELECT 1 FROM information_schema.table_constraints
           WHERE table_schema=DATABASE() AND table_name='promotions' AND constraint_name='chk_promo_limit'),
    'SELECT 1',
    'ALTER TABLE promotions ADD CONSTRAINT chk_promo_limit CHECK (usage_limit IS NULL OR used_count <= usage_limit)'));
PREPARE s FROM @add_chk_lim; EXECUTE s; DEALLOCATE PREPARE s;

-- Cột audit_logs.store_id cho CSDL CŨ (entity AuditLog đã có; bản tạo mới đã có sẵn). Thiếu cột này
--   khiến MỌI hành động ghi audit (mở/đóng ca, hủy HĐ, đổi giá...) lỗi "Unknown column 'store_id'".
SET @add_audit_store := (SELECT IF(
    EXISTS(SELECT 1 FROM information_schema.columns
           WHERE table_schema=DATABASE() AND table_name='audit_logs' AND column_name='store_id'),
    'SELECT 1',
    'ALTER TABLE audit_logs ADD COLUMN store_id BIGINT NULL AFTER actor_username, ADD KEY idx_audit_store (store_id)'));
PREPARE s FROM @add_audit_store; EXECUTE s; DEALLOCATE PREPARE s;

SET @add_audit_store_fk := (SELECT IF(
    EXISTS(SELECT 1 FROM information_schema.table_constraints
           WHERE table_schema=DATABASE() AND table_name='audit_logs' AND constraint_name='fk_audit_store'),
    'SELECT 1',
    'ALTER TABLE audit_logs ADD CONSTRAINT fk_audit_store FOREIGN KEY (store_id) REFERENCES stores(id)'));
PREPARE s FROM @add_audit_store_fk; EXECUTE s; DEALLOCATE PREPARE s;

-- FK: invoices.cancelled_by → users(id) (vết kiểm toán "ai hủy"). Cột thêm ở migration phía trên.
SET @add_fk_cancel := (SELECT IF(
    EXISTS(SELECT 1 FROM information_schema.table_constraints
           WHERE table_schema=DATABASE() AND table_name='invoices' AND constraint_name='fk_invoice_cancelled_by'),
    'SELECT 1',
    'ALTER TABLE invoices ADD CONSTRAINT fk_invoice_cancelled_by FOREIGN KEY (cancelled_by) REFERENCES users(id)'));
PREPARE s FROM @add_fk_cancel; EXECUTE s; DEALLOCATE PREPARE s;

-- FK: payroll_periods.submitted_by → users(id) (vết "ai trình duyệt" — duyệt 2 bước).
SET @add_fk_pp_sub := (SELECT IF(
    EXISTS(SELECT 1 FROM information_schema.table_constraints
           WHERE table_schema=DATABASE() AND table_name='payroll_periods' AND constraint_name='fk_pp_submitted'),
    'SELECT 1',
    'ALTER TABLE payroll_periods ADD CONSTRAINT fk_pp_submitted FOREIGN KEY (submitted_by) REFERENCES users(id)'));
PREPARE s FROM @add_fk_pp_sub; EXECUTE s; DEALLOCATE PREPARE s;

-- FK: payroll_periods.approved_by → users(id) (vết "ai duyệt" — tách trách nhiệm lập/duyệt).
SET @add_fk_pp_app := (SELECT IF(
    EXISTS(SELECT 1 FROM information_schema.table_constraints
           WHERE table_schema=DATABASE() AND table_name='payroll_periods' AND constraint_name='fk_pp_approved'),
    'SELECT 1',
    'ALTER TABLE payroll_periods ADD CONSTRAINT fk_pp_approved FOREIGN KEY (approved_by) REFERENCES users(id)'));
PREPARE s FROM @add_fk_pp_app; EXECUTE s; DEALLOCATE PREPARE s;

-- FK: stock_transfers.cancelled_by → users(id) (vết "ai hủy" phiếu điều chuyển).
SET @add_fk_tr_caby := (SELECT IF(
    EXISTS(SELECT 1 FROM information_schema.table_constraints
           WHERE table_schema=DATABASE() AND table_name='stock_transfers' AND constraint_name='fk_tr_caby'),
    'SELECT 1',
    'ALTER TABLE stock_transfers ADD CONSTRAINT fk_tr_caby FOREIGN KEY (cancelled_by) REFERENCES users(id)'));
PREPARE s FROM @add_fk_tr_caby; EXECUTE s; DEALLOCATE PREPARE s;

-- ĐỒNG NHẤT COLLATION: các bảng thêm sau (cash_movements, stock_adjustments) ở vài CSDL cũ bị tạo với
--   collation khác phần còn lại → trộn collation gây lỗi "illegal mix of collations" khi so chuỗi chéo bảng.
--   Quy 2 bảng này về ĐÚNG collation của bảng 'stores' (đại diện toàn bộ). No-op nếu đã khớp.
SET @std_coll := (SELECT table_collation FROM information_schema.tables
                  WHERE table_schema=DATABASE() AND table_name='stores');
SET @fix_cm_coll := (SELECT IF(
    (SELECT table_collation FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='cash_movements') <> @std_coll,
    CONCAT('ALTER TABLE cash_movements CONVERT TO CHARACTER SET utf8mb4 COLLATE ', @std_coll),
    'SELECT 1'));
PREPARE s FROM @fix_cm_coll; EXECUTE s; DEALLOCATE PREPARE s;
SET @fix_adj_coll := (SELECT IF(
    (SELECT table_collation FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='stock_adjustments') <> @std_coll,
    CONCAT('ALTER TABLE stock_adjustments CONVERT TO CHARACTER SET utf8mb4 COLLATE ', @std_coll),
    'SELECT 1'));
PREPARE s FROM @fix_adj_coll; EXECUTE s; DEALLOCATE PREPARE s;

-- LƯU Ý: các TRIGGER toàn vẹn (chống bán/đặt chéo chi nhánh, sai sản phẩm lô, bán vượt tồn, 1 lô/1 kệ,
--   hóa đơn ≠ ca, 2 ca mở) KHÔNG đặt ở đây vì trình spring.sql.init tách lệnh theo ';'. Chúng được tạo
--   bằng SchemaTriggersInitializer (chạy native DDL qua JDBC). Bản cài tay xem sql/schema.sql (có DELIMITER).

-- =====================================================================
--  VIEW (suy ra tồn kho & các tổng)
-- =====================================================================
-- Tồn từng LÔ tách KHO/KỆ:
--   quantity_remaining = nhập − đã bán (tổng còn)
--   transferred (net)  = đã lên kệ − đã trả về kho
--   on_shelf  (tồn kệ)  = lên kệ ròng − đã bán          (POS bán từ đây)
--   in_warehouse (tồn kho) = đã nhập − lên kệ ròng
-- TỐI ƯU PERF: gộp sẵn (pre-aggregate) từng nguồn biến động theo batch_id rồi LEFT JOIN
-- MỘT lần — thay cho 4 subquery TƯƠNG QUAN chạy lại cho TỪNG lô (cũ: ~O(lô × dòng bán)).
-- Tất cả khóa JOIN (batch_id) đều đã có index. Cột & ngữ nghĩa giữ NGUYÊN 100% —
-- tương thích view-entity BatchStockView và v_product_stock.
--   sold (A)        = phân bổ bán của HĐ chưa hủy (HĐ hủy ⇒ tồn tự hoàn)
--   transferred (T) = đã lên kệ ; shelf_returned (SR) = đã lấy từ kệ về kho
--   adjusted (ADJ)  = đã XUẤT HỦY/giảm tồn (rút khỏi KHO: hết hạn/hư hỏng/thất thoát)
--   quantity_remaining = nhập − A − ADJ ; on_shelf = (T−SR) − A ; in_warehouse = nhập − (T−SR) − ADJ
--   (bất biến: on_shelf + in_warehouse = quantity_remaining — xuất hủy trừ ở KHO nên tồn kệ không đổi)
CREATE OR REPLACE VIEW v_batch_stock AS
SELECT  gri.id        AS batch_id,
        gr.store_id,                                   -- ĐA CHUỖI: lô thừa hưởng chi nhánh từ phiếu nhập
        gri.product_id,
        gri.expiry_date,
        gri.quantity  AS quantity_in,
        fs.shelf_id,                                   -- kệ của phiếu lên kệ ĐẦU TIÊN (MIN id) — xác định
        (gri.quantity - COALESCE(sa.sold,0) - COALESCE(adj.adjusted,0))                        AS quantity_remaining,
        ((COALESCE(tr.transferred,0) - COALESCE(sret.shelf_returned,0)) - COALESCE(sa.sold,0)) AS on_shelf,
        (gri.quantity - (COALESCE(tr.transferred,0) - COALESCE(sret.shelf_returned,0)) - COALESCE(adj.adjusted,0)) AS in_warehouse
FROM goods_receipt_items gri
JOIN goods_receipts gr ON gr.id = gri.receipt_id       -- chi nhánh của lô
LEFT JOIN ( SELECT iib.batch_id, SUM(iib.quantity) AS sold
            FROM invoice_item_batches iib
            JOIN invoice_items ii ON ii.id = iib.invoice_item_id
            JOIN invoices      i  ON i.id  = ii.invoice_id
            WHERE i.status <> 'CANCELLED'
            GROUP BY iib.batch_id )                       sa   ON sa.batch_id   = gri.id
LEFT JOIN ( SELECT batch_id, SUM(quantity) AS transferred
            FROM shelf_transfers GROUP BY batch_id )      tr   ON tr.batch_id   = gri.id
LEFT JOIN ( SELECT batch_id, SUM(quantity) AS shelf_returned
            FROM shelf_returns GROUP BY batch_id )        sret ON sret.batch_id = gri.id
LEFT JOIN ( SELECT batch_id, SUM(quantity) AS adjusted
            FROM stock_adjustments GROUP BY batch_id )    adj  ON adj.batch_id  = gri.id
LEFT JOIN ( SELECT batch_id, MIN(id) AS first_id
            FROM shelf_transfers GROUP BY batch_id )      fmin ON fmin.batch_id = gri.id
LEFT JOIN shelf_transfers fs ON fs.id = fmin.first_id;

-- Tồn từng sản phẩm TÁCH THEO CHI NHÁNH (đa chuỗi): grain = (product_id, store_id).
-- JOIN (không LEFT JOIN): sản phẩm chưa từng nhập tại chi nhánh nào sẽ không có dòng tồn ở chi nhánh đó.
CREATE OR REPLACE VIEW v_product_stock AS
SELECT  p.id AS product_id,
        bs.store_id,
        p.barcode,
        p.name,
        p.min_stock,
        COALESCE(SUM(bs.quantity_remaining), 0) AS current_stock,
        COALESCE(SUM(bs.on_shelf), 0)           AS shelf_stock,
        COALESCE(SUM(bs.in_warehouse), 0)       AS warehouse_stock
FROM products p
JOIN v_batch_stock bs ON bs.product_id = p.id
GROUP BY p.id, bs.store_id, p.barcode, p.name, p.min_stock;

CREATE OR REPLACE VIEW v_expiring_batches AS
SELECT  bs.batch_id,
        bs.store_id,
        bs.product_id,
        p.name AS product_name,
        bs.quantity_remaining,
        bs.expiry_date,
        DATEDIFF(bs.expiry_date, CURDATE()) AS days_left
FROM v_batch_stock bs
JOIN products p ON p.id = bs.product_id
WHERE bs.quantity_remaining > 0
  AND bs.expiry_date IS NOT NULL
  AND bs.expiry_date <= DATE_ADD(CURDATE(), INTERVAL 30 DAY);

CREATE OR REPLACE VIEW v_customer_spending AS
SELECT  c.id AS customer_id,
        c.full_name,
        c.phone,
        c.loyalty_points,
        COALESCE(SUM(i.total_amount), 0) AS total_spent,
        COUNT(i.id) AS invoice_count
FROM customers c
LEFT JOIN invoices i ON i.customer_id = c.id AND i.status = 'COMPLETED'
GROUP BY c.id, c.full_name, c.phone, c.loyalty_points;

CREATE OR REPLACE VIEW v_pending_payments AS
SELECT  pt.id,
        pt.invoice_id,
        i.code AS invoice_code,
        pt.amount,
        pt.transfer_content,
        pt.created_at,
        pt.expired_at
FROM payment_transactions pt
JOIN invoices i ON i.id = pt.invoice_id
WHERE pt.status = 'PENDING';

CREATE OR REPLACE VIEW v_shift_summary AS
SELECT  s.id AS shift_id,
        s.store_id,
        s.user_id,
        u.full_name AS cashier_name,
        s.opening_cash,
        s.closing_cash,
        s.opened_at,
        s.closed_at,
        s.status,
        COALESCE(SUM(i.total_amount), 0) AS total_sales,
        COUNT(i.id) AS invoice_count
FROM work_shifts s
JOIN users u ON u.id = s.user_id
LEFT JOIN invoices i ON i.shift_id = s.id AND i.status = 'COMPLETED'
GROUP BY s.id, s.store_id, s.user_id, u.full_name, s.opening_cash, s.closing_cash, s.opened_at, s.closed_at, s.status;
