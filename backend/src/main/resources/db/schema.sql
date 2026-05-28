-- =====================================================================
--  SCHEMA TỰ KHỞI TẠO (idempotent) cho POS — chạy bởi Spring Boot
--  spring.sql.init mỗi lần khởi động. An toàn chạy lại nhiều lần:
--    - CREATE TABLE IF NOT EXISTS  → đã có thì bỏ qua (không mất dữ liệu)
--    - CREATE OR REPLACE VIEW      → cập nhật định nghĩa view
--  Dữ liệu nền (users/sản phẩm/khách/khuyến mãi...) do các *DataInitializer
--  trong code seed (idempotent), KHÔNG đặt ở đây.
--  => Drop database rồi khởi động lại: cấu trúc + view tự dựng, seeders tự nạp.
-- =====================================================================

-- 1. Người dùng & phân quyền ------------------------------------------
CREATE TABLE IF NOT EXISTS users (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    full_name     VARCHAR(100) NOT NULL,
    role          ENUM('ADMIN','MANAGER','CASHIER') NOT NULL,
    status        ENUM('ACTIVE','LOCKED') NOT NULL DEFAULT 'ACTIVE',
    created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- 2. Danh mục, đơn vị, nhà cung cấp -----------------------------------
CREATE TABLE IF NOT EXISTS categories (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(255),
    status      ENUM('ACTIVE','INACTIVE') NOT NULL DEFAULT 'ACTIVE'
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS units (
    id   BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS suppliers (
    id      BIGINT AUTO_INCREMENT PRIMARY KEY,
    name    VARCHAR(150) NOT NULL,
    phone   VARCHAR(20),
    email   VARCHAR(100),
    address VARCHAR(255),
    status  ENUM('ACTIVE','INACTIVE') NOT NULL DEFAULT 'ACTIVE'
) ENGINE=InnoDB;

-- 3. Sản phẩm ---------------------------------------------------------
CREATE TABLE IF NOT EXISTS products (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    barcode     VARCHAR(50)  NOT NULL UNIQUE,
    name        VARCHAR(150) NOT NULL,
    category_id BIGINT NOT NULL,
    unit_id     BIGINT NOT NULL,
    cost_price  DECIMAL(12,2) NOT NULL DEFAULT 0,
    sale_price  DECIMAL(12,2) NOT NULL,
    image_url   VARCHAR(255),
    min_stock   INT NOT NULL DEFAULT 0,
    status      ENUM('ACTIVE','INACTIVE') NOT NULL DEFAULT 'ACTIVE',
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_product_name (name),
    KEY idx_product_category (category_id),
    KEY idx_product_unit (unit_id),
    CONSTRAINT fk_product_category  FOREIGN KEY (category_id) REFERENCES categories(id),
    CONSTRAINT fk_product_unit      FOREIGN KEY (unit_id)     REFERENCES units(id),
    CONSTRAINT chk_product_price    CHECK (sale_price >= 0 AND cost_price >= 0),
    CONSTRAINT chk_product_minstock CHECK (min_stock >= 0)
) ENGINE=InnoDB;

-- 4. Nhập kho (goods_receipt_items = LÔ HÀNG) -------------------------
CREATE TABLE IF NOT EXISTS goods_receipts (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    code         VARCHAR(30) NOT NULL UNIQUE,
    supplier_id  BIGINT NOT NULL,
    created_by   BIGINT NOT NULL,
    total_amount DECIMAL(14,2) NOT NULL DEFAULT 0,
    note         VARCHAR(255),
    created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_receipt_supplier FOREIGN KEY (supplier_id) REFERENCES suppliers(id),
    CONSTRAINT fk_receipt_user     FOREIGN KEY (created_by)  REFERENCES users(id)
) ENGINE=InnoDB;

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
) ENGINE=InnoDB;

-- 5. Khách hàng & khuyến mãi ------------------------------------------
CREATE TABLE IF NOT EXISTS customers (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    full_name      VARCHAR(100) NOT NULL,
    phone          VARCHAR(20)  NOT NULL UNIQUE,
    email          VARCHAR(100),
    loyalty_points INT NOT NULL DEFAULT 0,
    created_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_customer_points CHECK (loyalty_points >= 0)
) ENGINE=InnoDB;

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
    CONSTRAINT chk_promo_value CHECK (discount_value >= 0),
    CONSTRAINT chk_promo_date  CHECK (end_date >= start_date),
    CONSTRAINT chk_promo_used  CHECK (used_count >= 0)
) ENGINE=InnoDB;

-- 6. Ca làm việc ------------------------------------------------------
CREATE TABLE IF NOT EXISTS work_shifts (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id      BIGINT NOT NULL,
    opening_cash DECIMAL(12,2) NOT NULL DEFAULT 0,
    closing_cash DECIMAL(12,2),
    opened_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    closed_at    DATETIME,
    status       ENUM('OPEN','CLOSED') NOT NULL DEFAULT 'OPEN',
    KEY idx_shift_user (user_id),
    KEY idx_shift_status (status),
    CONSTRAINT fk_shift_user FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB;

-- 7. Hóa đơn & chi tiết ----------------------------------------------
CREATE TABLE IF NOT EXISTS invoices (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    code            VARCHAR(30) NOT NULL UNIQUE,
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
    status          ENUM('COMPLETED','CANCELLED') NOT NULL DEFAULT 'COMPLETED',
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_invoice_shift (shift_id),
    KEY idx_invoice_customer (customer_id),
    KEY idx_invoice_created (created_at),
    KEY idx_invoice_status (status, created_at),
    CONSTRAINT fk_invoice_shift     FOREIGN KEY (shift_id)     REFERENCES work_shifts(id),
    CONSTRAINT fk_invoice_customer  FOREIGN KEY (customer_id)  REFERENCES customers(id),
    CONSTRAINT fk_invoice_promotion FOREIGN KEY (promotion_id) REFERENCES promotions(id),
    CONSTRAINT chk_invoice_amount   CHECK (subtotal >= 0 AND discount_amount >= 0)
) ENGINE=InnoDB;

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
) ENGINE=InnoDB;

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
) ENGINE=InnoDB;

-- 8a. KỆ VẬT LÝ (Display Shelves): các kệ trưng bày trong cửa hàng (Kệ A1, Kệ 1...).
CREATE TABLE IF NOT EXISTS shelves (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    code       VARCHAR(30)  NOT NULL UNIQUE,           -- mã kệ: A1, B2, K01...
    name       VARCHAR(100),                           -- tên/khu vực: "Nước giải khát"
    capacity   INT NOT NULL DEFAULT 0,                 -- sức chứa tối đa (số SP); 0 = không giới hạn
    status     ENUM('ACTIVE','INACTIVE') NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_shelf_cap CHECK (capacity >= 0)
) ENGINE=InnoDB;

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
) ENGINE=InnoDB;

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
) ENGINE=InnoDB;

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
    KEY idx_payment_status (status),
    CONSTRAINT fk_payment_invoice FOREIGN KEY (invoice_id) REFERENCES invoices(id) ON DELETE CASCADE,
    CONSTRAINT chk_payment_amount CHECK (amount >= 0)
) ENGINE=InnoDB;

-- 10. Cấu hình hệ thống (singleton id=1) -----------------------------
CREATE TABLE IF NOT EXISTS store_config (
    id                 TINYINT PRIMARY KEY DEFAULT 1,
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
    CONSTRAINT chk_store_singleton CHECK (id = 1)
) ENGINE=InnoDB;

-- 11. Người nhận thông báo Telegram ----------------------------------
CREATE TABLE IF NOT EXISTS telegram_recipients (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    config_id TINYINT NOT NULL DEFAULT 1,
    chat_id   VARCHAR(50) NOT NULL UNIQUE,
    label     VARCHAR(100),
    is_active TINYINT(1) NOT NULL DEFAULT 1,
    CONSTRAINT fk_tele_config FOREIGN KEY (config_id) REFERENCES store_config(id)
) ENGINE=InnoDB;

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

-- =====================================================================
--  VIEW (suy ra tồn kho & các tổng)
-- =====================================================================
-- Tồn từng LÔ tách KHO/KỆ:
--   quantity_remaining = nhập − đã bán (tổng còn)
--   transferred (net)  = đã lên kệ − đã trả về kho
--   on_shelf  (tồn kệ)  = lên kệ ròng − đã bán          (POS bán từ đây)
--   in_warehouse (tồn kho) = đã nhập − lên kệ ròng
CREATE OR REPLACE VIEW v_batch_stock AS
SELECT  b.batch_id, b.product_id, b.expiry_date, b.quantity_in, b.shelf_id,
        (b.quantity_in  - b.sold)        AS quantity_remaining,
        (b.transferred  - b.sold)        AS on_shelf,
        (b.quantity_in  - b.transferred) AS in_warehouse
FROM (
    SELECT  gri.id AS batch_id, gri.product_id, gri.expiry_date, gri.quantity AS quantity_in,
            (SELECT st.shelf_id FROM shelf_transfers st WHERE st.batch_id = gri.id LIMIT 1) AS shelf_id,
            COALESCE((SELECT SUM(iib.quantity) FROM invoice_item_batches iib
                JOIN invoice_items ii ON ii.id = iib.invoice_item_id
                JOIN invoices      i  ON i.id  = ii.invoice_id
                WHERE iib.batch_id = gri.id AND i.status = 'COMPLETED'), 0) AS sold,
            COALESCE((SELECT SUM(st.quantity) FROM shelf_transfers st
                WHERE st.batch_id = gri.id), 0)
              - COALESCE((SELECT SUM(sr.quantity) FROM shelf_returns sr
                WHERE sr.batch_id = gri.id), 0) AS transferred
    FROM goods_receipt_items gri
) b;

CREATE OR REPLACE VIEW v_product_stock AS
SELECT  p.id AS product_id,
        p.barcode,
        p.name,
        p.min_stock,
        COALESCE(SUM(bs.quantity_remaining), 0) AS current_stock,
        COALESCE(SUM(bs.on_shelf), 0)           AS shelf_stock,
        COALESCE(SUM(bs.in_warehouse), 0)       AS warehouse_stock
FROM products p
LEFT JOIN v_batch_stock bs ON bs.product_id = p.id
GROUP BY p.id, p.barcode, p.name, p.min_stock;

CREATE OR REPLACE VIEW v_low_stock AS
SELECT * FROM v_product_stock WHERE current_stock <= min_stock;

CREATE OR REPLACE VIEW v_expiring_batches AS
SELECT  bs.batch_id,
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
GROUP BY s.id, s.user_id, u.full_name, s.opening_cash, s.closing_cash, s.opened_at, s.closed_at, s.status;
