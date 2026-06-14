-- =====================================================================
--  HỆ THỐNG POS CHO CỬA HÀNG TIỆN LỢI
--  Script tạo CSDL MySQL 8 + dữ liệu mẫu  (BẢN CÀI TAY ĐỘC LẬP / TÀI LIỆU)
--  Thiết kế chuẩn hóa 3NF, toàn vẹn tham chiếu đầy đủ, không bảng/cột dư thừa.
--  Xem docs/04_Thiet_ke_CSDL_ERD.md
--  Cách chạy:  mysql -u root -p < sql/schema.sql   (hoặc Import qua phpMyAdmin)
--
--  ⚠️ LƯU Ý: Khi CHẠY ỨNG DỤNG, backend KHÔNG dùng file này. App tự dựng cấu trúc + VIEW
--     từ 'backend/src/main/resources/db/schema.sql' (spring.sql.init) rồi seed dữ liệu nền
--     bằng các *DataInitializer trong code. File này chỉ để cài tay/đọc tài liệu —
--     nếu sửa cấu trúc, phải đồng bộ CẢ HAI file để khỏi lệch.
-- =====================================================================

DROP DATABASE IF EXISTS pos_convenience_store;
CREATE DATABASE pos_convenience_store
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;   -- = mặc định MySQL 8; đồng nhất với DB do app tự tạo (tránh trộn collation)
USE pos_convenience_store;

-- ---------------------------------------------------------------------
-- 0. CHI NHÁNH / CỬA HÀNG trong chuỗi (ĐA CHUỖI). Tạo TRƯỚC mọi bảng tham chiếu store_id.
-- ---------------------------------------------------------------------
CREATE TABLE stores (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    code       VARCHAR(30)  NOT NULL UNIQUE,              -- CH01, CH02...
    name       VARCHAR(150) NOT NULL,
    address    VARCHAR(255),
    phone      VARCHAR(20),
    status     ENUM('ACTIVE','INACTIVE') NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 1. NGƯỜI DÙNG & PHÂN QUYỀN (FR1)
--    store_id NULL = quản trị toàn chuỗi (CHAIN_ADMIN); còn lại gắn 1 chi nhánh.
-- ---------------------------------------------------------------------
CREATE TABLE users (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,                  -- BCrypt (~60 ký tự)
    full_name     VARCHAR(100) NOT NULL,
    role          ENUM('ADMIN','MANAGER','STAFF') NOT NULL,   -- ADMIN=toàn chuỗi; MANAGER/STAFF=một cửa hàng
    store_id      BIGINT,
    status        ENUM('ACTIVE','LOCKED') NOT NULL DEFAULT 'ACTIVE',
    created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_store FOREIGN KEY (store_id) REFERENCES stores(id)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 2. DANH MỤC, ĐƠN VỊ TÍNH (FR2) + NHÀ CUNG CẤP (FR3)  -- các bảng gốc
-- ---------------------------------------------------------------------
CREATE TABLE categories (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(255),
    status      ENUM('ACTIVE','INACTIVE') NOT NULL DEFAULT 'ACTIVE'
) ENGINE=InnoDB;

CREATE TABLE units (
    id   BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE                       -- lon, chai, gói, thùng...
) ENGINE=InnoDB;

CREATE TABLE suppliers (
    id      BIGINT AUTO_INCREMENT PRIMARY KEY,
    name    VARCHAR(150) NOT NULL,
    phone   VARCHAR(20),
    email   VARCHAR(100),
    address VARCHAR(255),
    status  ENUM('ACTIVE','INACTIVE') NOT NULL DEFAULT 'ACTIVE'
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 3. SẢN PHẨM (FR2.3) -> FK categories, units
-- ---------------------------------------------------------------------
CREATE TABLE products (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    barcode     VARCHAR(50)  NOT NULL UNIQUE,              -- mã vạch
    name        VARCHAR(150) NOT NULL,
    category_id BIGINT NOT NULL,
    unit_id     BIGINT NOT NULL,
    cost_price  DECIMAL(12,2) NOT NULL DEFAULT 0,          -- giá vốn hiện hành
    sale_price  DECIMAL(12,2) NOT NULL,                    -- giá bán (da gom VAT)
    tax_rate    DECIMAL(5,2)  NOT NULL DEFAULT 8.00,        -- thue suat GTGT %
    pack_size   INT NOT NULL DEFAULT 1,                     -- 1 thung = ? lon
    pack_unit_id BIGINT,                                    -- don vi mua (thung)
    image_url   VARCHAR(255),
    min_stock   INT NOT NULL DEFAULT 0,                    -- mức tồn tối thiểu để cảnh báo
    status      ENUM('ACTIVE','INACTIVE') NOT NULL DEFAULT 'ACTIVE',
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- KHÔNG có current_stock: tồn kho suy ra từ lô (view v_product_stock)
    CONSTRAINT fk_product_category  FOREIGN KEY (category_id) REFERENCES categories(id),
    CONSTRAINT fk_product_unit      FOREIGN KEY (unit_id)     REFERENCES units(id),
    CONSTRAINT fk_product_pack_unit FOREIGN KEY (pack_unit_id) REFERENCES units(id),
    CONSTRAINT chk_product_price    CHECK (sale_price >= 0 AND cost_price >= 0),
    CONSTRAINT chk_product_packsize CHECK (pack_size >= 1),
    CONSTRAINT chk_product_minstock CHECK (min_stock >= 0)
) ENGINE=InnoDB;

CREATE INDEX idx_product_name     ON products(name);
CREATE INDEX idx_product_category ON products(category_id);
CREATE INDEX idx_product_unit     ON products(unit_id);

-- ---------------------------------------------------------------------
-- 4. NHẬP KHO (FR3) -> goods_receipt_items đóng vai trò "LÔ HÀNG" (bất biến)
--      quantity     = số lượng nhập (cố định)
--      expiry_date  = hạn sử dụng của lô
--    Tồn còn lại KHÔNG lưu ở đây -> suy ra từ phân bổ bán (invoice_item_batches).
-- ---------------------------------------------------------------------
CREATE TABLE goods_receipts (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    code         VARCHAR(30) NOT NULL UNIQUE,              -- mã phiếu nhập
    store_id     BIGINT NOT NULL,                          -- chi nhánh nhập (LÔ thừa hưởng chi nhánh)
    supplier_id  BIGINT NOT NULL,
    created_by   BIGINT NOT NULL,                          -- người lập phiếu
    total_amount DECIMAL(14,2) NOT NULL DEFAULT 0,         -- tổng tiền chứng từ (chốt khi lưu)
    note         VARCHAR(255),
    created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_receipt_store    FOREIGN KEY (store_id)    REFERENCES stores(id),
    CONSTRAINT fk_receipt_supplier FOREIGN KEY (supplier_id) REFERENCES suppliers(id),
    CONSTRAINT fk_receipt_user     FOREIGN KEY (created_by)  REFERENCES users(id)
) ENGINE=InnoDB;
CREATE INDEX idx_receipt_store ON goods_receipts(store_id);

CREATE TABLE goods_receipt_items (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    receipt_id   BIGINT NOT NULL,
    product_id   BIGINT NOT NULL,
    quantity     INT NOT NULL,                             -- số lượng nhập của lô (cố định)
    import_price DECIMAL(12,2) NOT NULL,                   -- giá nhập tại thời điểm
    expiry_date  DATE,                                     -- HSD của lô (NULL nếu không HSD)
    CONSTRAINT fk_gri_receipt FOREIGN KEY (receipt_id) REFERENCES goods_receipts(id) ON DELETE CASCADE,
    CONSTRAINT fk_gri_product FOREIGN KEY (product_id) REFERENCES products(id),
    CONSTRAINT chk_gri_qty CHECK (quantity > 0)
) ENGINE=InnoDB;

CREATE INDEX idx_gri_product ON goods_receipt_items(product_id);
CREATE INDEX idx_gri_expiry  ON goods_receipt_items(expiry_date);

-- ---------------------------------------------------------------------
-- 5. KHÁCH HÀNG (FR6) & KHUYẾN MÃI (FR7)  -- các bảng gốc
-- ---------------------------------------------------------------------
CREATE TABLE customers (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    full_name      VARCHAR(100) NOT NULL,
    phone          VARCHAR(20)  NOT NULL UNIQUE,           -- SĐT duy nhất
    email          VARCHAR(100),
    loyalty_points INT NOT NULL DEFAULT 0,                 -- số dư điểm (trạng thái)
    created_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_customer_points CHECK (loyalty_points >= 0)
) ENGINE=InnoDB;

CREATE TABLE promotions (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    code             VARCHAR(30)  NOT NULL UNIQUE,
    name             VARCHAR(150) NOT NULL,
    discount_type    ENUM('PERCENT','AMOUNT') NOT NULL,
    discount_value   DECIMAL(12,2) NOT NULL,
    min_order_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
    start_date       DATETIME NOT NULL,
    end_date         DATETIME NOT NULL,
    usage_limit      INT,                                  -- NULL = không giới hạn
    used_count       INT NOT NULL DEFAULT 0,
    status           ENUM('ACTIVE','INACTIVE') NOT NULL DEFAULT 'ACTIVE',
    CONSTRAINT chk_promo_value   CHECK (discount_value >= 0),
    CONSTRAINT chk_promo_date    CHECK (end_date >= start_date),
    CONSTRAINT chk_promo_used    CHECK (used_count >= 0),
    CONSTRAINT chk_promo_percent CHECK (discount_type <> 'PERCENT' OR discount_value <= 100),  -- % giảm không quá 100
    CONSTRAINT chk_promo_limit   CHECK (usage_limit IS NULL OR used_count <= usage_limit)       -- không dùng quá hạn mức
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 6. CA LÀM VIỆC (FR4.1) -> FK users
-- ---------------------------------------------------------------------
CREATE TABLE work_shifts (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id     BIGINT NOT NULL,                          -- chi nhánh mở ca
    user_id      BIGINT NOT NULL,                          -- thu ngân của ca
    opening_cash DECIMAL(12,2) NOT NULL DEFAULT 0,
    closing_cash DECIMAL(12,2),                            -- đối soát cuối ca (NULL khi đang mở)
    opened_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    closed_at    DATETIME,
    status       ENUM('OPEN','CLOSED') NOT NULL DEFAULT 'OPEN',
    CONSTRAINT fk_shift_store FOREIGN KEY (store_id) REFERENCES stores(id),
    CONSTRAINT fk_shift_user  FOREIGN KEY (user_id)  REFERENCES users(id)
) ENGINE=InnoDB;

CREATE INDEX idx_shift_user   ON work_shifts(user_id);
CREATE INDEX idx_shift_status ON work_shifts(status);
CREATE INDEX idx_shift_store  ON work_shifts(store_id);


-- (Bang cash_movements dinh nghia o muc petty cash ben duoi — gan stock_adjustments.)

-- ---------------------------------------------------------------------
-- 7. HÓA ĐƠN & CHI TIẾT (FR4, FR5)
--    KHÔNG có cashier_id: thu ngân suy ra qua shift_id -> work_shifts.user_id.
--    total_amount là cột GENERATED = subtotal - discount_amount.
-- ---------------------------------------------------------------------
CREATE TABLE invoices (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    code            VARCHAR(30) NOT NULL UNIQUE,           -- mã hóa đơn
    store_id        BIGINT NOT NULL,                       -- chi nhánh bán (chốt từ ca)
    shift_id        BIGINT NOT NULL,                       -- ca làm việc (=> thu ngân)
    customer_id     BIGINT,                                -- tùy chọn (khách thân thiết)
    promotion_id    BIGINT,                                -- tùy chọn (mã giảm giá)
    subtotal        DECIMAL(14,2) NOT NULL,                -- tổng trước giảm (chốt từ chi tiết)
    discount_amount DECIMAL(14,2) NOT NULL DEFAULT 0,
    total_amount    DECIMAL(14,2) AS (subtotal - discount_amount) STORED,  -- tính sẵn
    payment_method  ENUM('CASH','QR') NOT NULL,
    customer_paid   DECIMAL(14,2),                         -- tiền khách đưa (tiền mặt)
    change_amount   DECIMAL(14,2),                         -- tiền thừa
    points_earned   INT NOT NULL DEFAULT 0,                -- điểm tích cho khách ở HĐ này
    points_used     INT NOT NULL DEFAULT 0,                -- điểm khách dùng để giảm trừ ở HĐ này
    status          ENUM('COMPLETED','CANCELLED','PENDING_PAYMENT') NOT NULL DEFAULT 'COMPLETED',
    tax_amount      DECIMAL(14,2) NOT NULL DEFAULT 0,       -- phan VAT trong tong
    cancelled_by    BIGINT,                                -- ai hủy (audit)
    cancelled_at    DATETIME,                              -- khi nào hủy
    cancel_reason   VARCHAR(255),                          -- lý do hủy (bắt buộc khi hủy)
    idempotency_key VARCHAR(64) UNIQUE,                    -- chong tao HD trung khi gui lai
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_invoice_store     FOREIGN KEY (store_id)     REFERENCES stores(id),
    CONSTRAINT fk_invoice_shift     FOREIGN KEY (shift_id)     REFERENCES work_shifts(id),
    CONSTRAINT fk_invoice_customer  FOREIGN KEY (customer_id)  REFERENCES customers(id),
    CONSTRAINT fk_invoice_promotion FOREIGN KEY (promotion_id) REFERENCES promotions(id),
    CONSTRAINT fk_invoice_cancelled_by FOREIGN KEY (cancelled_by) REFERENCES users(id),  -- vết "ai hủy"
    CONSTRAINT chk_invoice_amount   CHECK (subtotal >= 0 AND discount_amount >= 0)
) ENGINE=InnoDB;

CREATE INDEX idx_invoice_shift    ON invoices(shift_id);
CREATE INDEX idx_invoice_customer ON invoices(customer_id);
CREATE INDEX idx_invoice_created  ON invoices(created_at);
CREATE INDEX idx_invoice_status   ON invoices(status, created_at);
CREATE INDEX idx_invoice_store    ON invoices(store_id, status, created_at);

CREATE TABLE invoice_items (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    invoice_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity   INT NOT NULL,
    unit_price DECIMAL(12,2) NOT NULL,                     -- giá bán tại thời điểm (snapshot)
    subtotal   DECIMAL(14,2) AS (quantity * unit_price) STORED,  -- tính sẵn
    CONSTRAINT fk_ii_invoice FOREIGN KEY (invoice_id) REFERENCES invoices(id) ON DELETE CASCADE,
    CONSTRAINT fk_ii_product FOREIGN KEY (product_id) REFERENCES products(id),
    CONSTRAINT chk_ii_qty CHECK (quantity > 0)
) ENGINE=InnoDB;

CREATE INDEX idx_ii_invoice ON invoice_items(invoice_id);
CREATE INDEX idx_ii_product ON invoice_items(product_id);

-- ---------------------------------------------------------------------
-- 8. PHÂN BỔ TỒN KHO KHI BÁN  (BẢNG NỐI bán <-> lô)  *** MỚI ***
--    Nối invoice_items <-> goods_receipt_items: mỗi dòng bán lấy hàng từ
--    lô nào, số lượng bao nhiêu (FIFO theo HSD). Đảm bảo:
--      - Truy vết & toàn vẹn tồn kho theo lô.
--      - Hủy hóa đơn (UC14): tồn TỰ HOÀN vì phân bổ trỏ tới HĐ CANCELLED
--        không còn được tính (xem view v_batch_stock).
--    Ràng buộc: SUM(quantity theo invoice_item) = invoice_items.quantity.
-- ---------------------------------------------------------------------
CREATE TABLE invoice_item_batches (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    invoice_item_id BIGINT NOT NULL,
    batch_id        BIGINT NOT NULL,                       -- = goods_receipt_items.id
    quantity        INT NOT NULL,                          -- số lượng lấy từ lô này
    CONSTRAINT fk_iib_item  FOREIGN KEY (invoice_item_id) REFERENCES invoice_items(id) ON DELETE CASCADE,
    CONSTRAINT fk_iib_batch FOREIGN KEY (batch_id)        REFERENCES goods_receipt_items(id),
    CONSTRAINT chk_iib_qty  CHECK (quantity > 0)
) ENGINE=InnoDB;

CREATE INDEX idx_iib_item  ON invoice_item_batches(invoice_item_id);
CREATE INDEX idx_iib_batch ON invoice_item_batches(batch_id);

-- ---------------------------------------------------------------------
-- 8a. KỆ VẬT LÝ (Display Shelves): các kệ trưng bày (Kệ A1, K01...).
-- ---------------------------------------------------------------------
CREATE TABLE shelves (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id   BIGINT NOT NULL,                          -- chi nhánh đặt kệ
    code       VARCHAR(30)  NOT NULL,                    -- mã kệ: K01, A1... (duy nhất TRONG chi nhánh)
    name       VARCHAR(100),                             -- khu vực: "Nước giải khát"
    capacity   INT NOT NULL DEFAULT 0,                   -- sức chứa tối đa (số SP); 0 = không giới hạn
    status     ENUM('ACTIVE','INACTIVE') NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_shelf_store_code UNIQUE (store_id, code),
    CONSTRAINT fk_shelf_store FOREIGN KEY (store_id) REFERENCES stores(id),
    CONSTRAINT chk_shelf_cap CHECK (capacity >= 0)
) ENGINE=InnoDB;
CREATE INDEX idx_shelf_store ON shelves(store_id);

-- ---------------------------------------------------------------------
-- 8b. CHUYỂN HÀNG TỪ KHO LÊN KỆ (mỗi dòng = số lượng của 1 LÔ đưa lên 1 KỆ).
--     Quy ước: một LÔ chỉ nằm trên MỘT kệ.
--     Tồn kệ của lô = đã lên kệ − đã bán; Tồn kho của lô = đã nhập − đã lên kệ.
-- ---------------------------------------------------------------------
CREATE TABLE shelf_transfers (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    batch_id    BIGINT NOT NULL,                       -- = goods_receipt_items.id
    shelf_id    BIGINT NOT NULL,                       -- kệ đích
    quantity    INT NOT NULL,
    created_by  BIGINT,
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_st_batch FOREIGN KEY (batch_id) REFERENCES goods_receipt_items(id) ON DELETE CASCADE,
    CONSTRAINT fk_st_shelf FOREIGN KEY (shelf_id) REFERENCES shelves(id),
    CONSTRAINT fk_st_user  FOREIGN KEY (created_by) REFERENCES users(id),
    CONSTRAINT chk_st_qty  CHECK (quantity > 0)
) ENGINE=InnoDB;

CREATE INDEX idx_st_batch ON shelf_transfers(batch_id);
CREATE INDEX idx_st_shelf ON shelf_transfers(shelf_id);

-- Lay hang tu ke ve kho (doi ung voi len ke).
CREATE TABLE IF NOT EXISTS shelf_returns (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    batch_id    BIGINT NOT NULL,
    shelf_id    BIGINT NOT NULL,
    quantity    INT NOT NULL,
    created_by  BIGINT,
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_sr_batch FOREIGN KEY (batch_id) REFERENCES goods_receipt_items(id) ON DELETE CASCADE,
    CONSTRAINT fk_sr_shelf FOREIGN KEY (shelf_id) REFERENCES shelves(id),
    CONSTRAINT fk_sr_user  FOREIGN KEY (created_by) REFERENCES users(id),
    CONSTRAINT chk_sr_qty  CHECK (quantity > 0)
) ENGINE=InnoDB;

CREATE INDEX idx_sr_batch ON shelf_returns(batch_id);
CREATE INDEX idx_sr_shelf ON shelf_returns(shelf_id);

-- (Đã bỏ chức năng TRẢ HÀNG / HOÀN TIỀN: cửa hàng tiện lợi không nhận trả hàng;
--  hủy nhầm hóa đơn dùng HỦY HĐ → tồn tự hoàn qua view. Không còn bảng sales_returns.)

-- ---------------------------------------------------------------------
-- 9. GIAO DỊCH THANH TOÁN ĐIỆN TỬ — VietQR + WEB2M (FR-A1, FR-A4)
--    Chỉ dùng cho thanh toán QR/chuyển khoản cần ĐỐI SOÁT TỰ ĐỘNG qua WEB2M.
--    Tiền mặt KHÔNG tạo dòng ở đây (đã thu tại quầy).
--    Mỗi cột đều có vai trò riêng (không thừa):
--      amount           = số tiền cần khớp với giao dịch ngân hàng (snapshot)
--      transfer_content = nội dung CK DUY NHẤT để WEB2M map giao dịch -> đúng HĐ
--      bank_reference   = mã giao dịch NH do WEB2M trả về (bằng chứng đã nhận tiền)
-- ---------------------------------------------------------------------
CREATE TABLE payment_transactions (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    invoice_id       BIGINT NOT NULL,
    amount           DECIMAL(14,2) NOT NULL,             -- số tiền cần khớp
    transfer_content VARCHAR(50) NOT NULL UNIQUE,         -- nội dung CK duy nhất (WEB2M đối soát)
    status           ENUM('PENDING','PAID','EXPIRED','FAILED') NOT NULL DEFAULT 'PENDING',
    bank_reference   VARCHAR(100),                        -- mã GD ngân hàng (khi khớp)
    paid_at          DATETIME,                            -- thời điểm xác nhận đã nhận tiền
    expired_at       DATETIME,                            -- hạn hiệu lực mã QR
    created_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_payment_invoice FOREIGN KEY (invoice_id) REFERENCES invoices(id) ON DELETE CASCADE,
    CONSTRAINT chk_payment_amount CHECK (amount >= 0)
) ENGINE=InnoDB;

CREATE INDEX idx_payment_invoice ON payment_transactions(invoice_id);
CREATE INDEX idx_payment_status  ON payment_transactions(status);

-- ---------------------------------------------------------------------
-- 10. CẤU HÌNH HỆ THỐNG (FR10, FR-A) - bảng 1 dòng (singleton) làm HUB cấu hình:
--     thông tin cửa hàng (in HĐ) + ngân hàng (sinh VietQR) + WEB2M (poll đối soát)
--     + Telegram Bot (thông báo). Mỗi cột có vai trò riêng, không thừa.
--     LƯU Ý: web2m_api_url & telegram_bot_token là NHẠY CẢM - giới hạn quyền đọc;
--            production nên override bằng biến môi trường trong application.yml.
-- ---------------------------------------------------------------------
CREATE TABLE store_config (
    id                BIGINT PRIMARY KEY,                  -- = stores.id (mỗi chi nhánh 1 dòng)
    -- Thông tin cửa hàng (in hóa đơn - FR10)
    name              VARCHAR(150) NOT NULL,
    address           VARCHAR(255),
    phone             VARCHAR(20),
    tax_code          VARCHAR(30),
    logo_url          VARCHAR(255),
    -- Ngân hàng nhận tiền - để SINH MÃ VietQR hiển thị (FR-A1)
    bank_name         VARCHAR(50),                         -- vd 'MB Bank' (hiển thị)
    bank_bin          VARCHAR(20),                         -- mã BIN cho VietQR, vd 970422 (MB)
    bank_account_no   VARCHAR(30),                         -- số tài khoản nhận tiền
    bank_account_name VARCHAR(100),                        -- tên chủ tài khoản
    transfer_prefix   VARCHAR(20),                         -- mã ký hiệu trong nội dung CK, vd 'POS'
    -- WEB2M - API poll lịch sử giao dịch để ĐỐI SOÁT tự động (FR-A4)
    web2m_api_url     VARCHAR(255),                        -- URL đầy đủ (token nằm trong path)
    -- Telegram Bot - thông báo tự động (FR-A5)
    telegram_bot_token VARCHAR(255),
    telegram_enabled   TINYINT(1) NOT NULL DEFAULT 0,
    notify_payment     TINYINT(1) NOT NULL DEFAULT 1,      -- báo khi nhận thanh toán
    notify_low_stock   TINYINT(1) NOT NULL DEFAULT 1,      -- báo tồn thấp
    notify_new_invoice TINYINT(1) NOT NULL DEFAULT 0,      -- báo hóa đơn mới
    updated_at        DATETIME,
    CONSTRAINT fk_config_store FOREIGN KEY (id) REFERENCES stores(id)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 11. NGƯỜI NHẬN THÔNG BÁO TELEGRAM (FR-A5) - danh sách Chat ID.
--     FK -> store_config (nối về hub cấu hình, không đứng riêng). Tách bảng
--     để giữ 1NF (mỗi Chat ID một dòng), thêm/xóa người nhận linh hoạt.
-- ---------------------------------------------------------------------
CREATE TABLE telegram_recipients (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    config_id BIGINT NOT NULL,                             -- = store_config.id = stores.id
    chat_id   VARCHAR(50) NOT NULL,                        -- ID số hoặc @username kênh
    label     VARCHAR(100),                                -- ghi chú người nhận
    is_active TINYINT(1) NOT NULL DEFAULT 1,
    CONSTRAINT uq_tele_store_chat UNIQUE (config_id, chat_id),
    CONSTRAINT fk_tele_config FOREIGN KEY (config_id) REFERENCES store_config(id)
) ENGINE=InnoDB;

-- Nhat ky kiem toan (audit log): ai lam gi, khi nao - cho hanh dong nhay cam.
CREATE TABLE audit_logs (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    actor_user_id  BIGINT,
    actor_username VARCHAR(50),
    store_id       BIGINT,                                -- chi nhanh phat sinh thao tac (NULL = toan chuoi)
    action         VARCHAR(60) NOT NULL,
    target_type    VARCHAR(40),
    target_id      BIGINT,
    detail         VARCHAR(500),
    created_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_audit_action (action),
    KEY idx_audit_target (target_type, target_id),
    KEY idx_audit_actor (actor_user_id),
    KEY idx_audit_store (store_id),
    CONSTRAINT fk_audit_user  FOREIGN KEY (actor_user_id) REFERENCES users(id),
    CONSTRAINT fk_audit_store FOREIGN KEY (store_id)      REFERENCES stores(id)
) ENGINE=InnoDB;

-- So cai diem tich luy (loyalty ledger): moi thay doi diem la 1 dong (truy vet, doi soat).
CREATE TABLE loyalty_point_ledger (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id   BIGINT NOT NULL,
    invoice_id    BIGINT,
    delta         INT NOT NULL,
    reason        VARCHAR(40) NOT NULL,
    balance_after INT NOT NULL,
    created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_lpl_customer (customer_id),
    KEY idx_lpl_invoice (invoice_id),
    CONSTRAINT fk_lpl_customer FOREIGN KEY (customer_id) REFERENCES customers(id),
    CONSTRAINT fk_lpl_invoice  FOREIGN KEY (invoice_id)  REFERENCES invoices(id) ON DELETE SET NULL
) ENGINE=InnoDB;

-- Xuat huy / dieu chinh giam ton (kiem ke & hao hut): rut hang het han/hu hong/that thoat khoi ton KHO
-- cua mot LO. Moi dong GIAM `quantity` don vi (append-only). v_batch_stock tru tong nay.
CREATE TABLE stock_adjustments (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id   BIGINT NOT NULL,                            -- chi nhanh phat sinh (= chi nhanh cua lo)
    batch_id   BIGINT NOT NULL,                            -- = goods_receipt_items.id (lo bi giam ton)
    quantity   INT NOT NULL,                               -- so luong GIAM (duong)
    reason     ENUM('EXPIRED','DAMAGED','LOST','OTHER') NOT NULL,  -- het han / hu hong / that thoat / khac
    note       VARCHAR(255),
    created_by BIGINT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_adj_store (store_id),
    KEY idx_adj_batch (batch_id),
    CONSTRAINT fk_adj_store FOREIGN KEY (store_id)   REFERENCES stores(id),
    CONSTRAINT fk_adj_batch FOREIGN KEY (batch_id)   REFERENCES goods_receipt_items(id),
    CONSTRAINT fk_adj_user  FOREIGN KEY (created_by) REFERENCES users(id),
    CONSTRAINT chk_adj_qty  CHECK (quantity > 0)
) ENGINE=InnoDB;

-- Thu/chi tien mat NGOAI ban hang trong ca (petty cash). Append-only. Doi soat quy cuoi ca:
-- tien du kien = dau ca + tien mat ban + SUM(IN) - SUM(OUT).
CREATE TABLE cash_movements (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    shift_id   BIGINT NOT NULL,                            -- ca phat sinh
    store_id   BIGINT NOT NULL,                            -- chi nhanh (= chi nhanh cua ca)
    type       ENUM('IN','OUT') NOT NULL,                  -- THU vao / CHI ra khoi ket
    amount     DECIMAL(14,2) NOT NULL,                     -- so tien (duong)
    reason     VARCHAR(255) NOT NULL,                      -- ly do/dien giai (bat buoc)
    created_by BIGINT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_cash_shift (shift_id),
    KEY idx_cash_store (store_id),
    CONSTRAINT fk_cash_shift FOREIGN KEY (shift_id)   REFERENCES work_shifts(id),
    CONSTRAINT fk_cash_store FOREIGN KEY (store_id)   REFERENCES stores(id),
    CONSTRAINT fk_cash_user  FOREIGN KEY (created_by) REFERENCES users(id),
    CONSTRAINT chk_cash_amount CHECK (amount > 0)
) ENGINE=InnoDB;

-- =====================================================================
--  VIEW (suy ra tồn kho & các tổng - thay cột dư thừa)
-- =====================================================================

-- Tồn còn lại của từng LÔ = số nhập - tổng đã bán (chỉ HĐ COMPLETED).
-- Hủy HĐ (CANCELLED) => phân bổ không được tính => tồn tự hoàn.
-- Tồn từng LÔ tách KHO/KỆ: on_shelf (kệ, POS bán) + in_warehouse (kho)
-- TỐI ƯU PERF: gộp sẵn (pre-aggregate) từng nguồn biến động theo batch_id rồi LEFT JOIN
-- MỘT lần — thay cho 4 subquery TƯƠNG QUAN chạy lại cho TỪNG lô (cũ: ~O(lô × dòng bán)).
-- Tất cả khóa JOIN (batch_id) đều đã có index. Cột & ngữ nghĩa giữ NGUYÊN 100%.
--   sold (A) = phân bổ bán HĐ chưa hủy (HĐ hủy ⇒ tồn tự hoàn)
--   transferred (T) = đã lên kệ ; shelf_returned (SR) = đã lấy từ kệ về kho
--   adjusted (ADJ) = đã XUẤT HỦY/giảm tồn (rút khỏi KHO: hết hạn/hư hỏng/thất thoát)
--   quantity_remaining = nhập − A − ADJ ; on_shelf = (T−SR) − A ; in_warehouse = nhập − (T−SR) − ADJ
--   store_id = chi nhánh của lô (thừa hưởng từ phiếu nhập) — ĐA CHUỖI.
CREATE OR REPLACE VIEW v_batch_stock AS
SELECT  gri.id        AS batch_id,
        gr.store_id,
        gri.product_id,
        gri.expiry_date,
        gri.quantity  AS quantity_in,
        fs.shelf_id,
        (gri.quantity - COALESCE(sa.sold,0) - COALESCE(adj.adjusted,0))                        AS quantity_remaining,
        ((COALESCE(tr.transferred,0) - COALESCE(sret.shelf_returned,0)) - COALESCE(sa.sold,0)) AS on_shelf,
        (gri.quantity - (COALESCE(tr.transferred,0) - COALESCE(sret.shelf_returned,0)) - COALESCE(adj.adjusted,0)) AS in_warehouse
FROM goods_receipt_items gri
JOIN goods_receipts gr ON gr.id = gri.receipt_id
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

-- Tồn hiện tại từng sản phẩm TÁCH THEO CHI NHÁNH (đa chuỗi): grain = (product_id, store_id)
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

-- Lô còn hàng & cận/quá HSD trong 30 ngày tới (FR8.2)
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

-- Tổng chi tiêu của khách (FR6.2) - thay cột total_spent
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

-- Giao dịch QR đang chờ đối soát (FR-A4) - cho thu ngân theo dõi & job WEB2M quét
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

-- Doanh thu theo ca (FR9.2) - thay cột total_sales
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

-- =====================================================================
--  TRIGGER TOÀN VẸN — ép các bất biến mà khóa ngoại đơn cột KHÔNG diễn đạt được
--  (đa chi nhánh, đúng sản phẩm lô, không bán vượt tồn, 1 lô/1 kệ, HĐ↔ca, 1 ca mở).
--  Bản chạy ỨNG DỤNG tạo các trigger này bằng SchemaTriggersInitializer (JDBC).
-- =====================================================================
DELIMITER $$

DROP TRIGGER IF EXISTS trg_iib_bi_integrity $$
CREATE TRIGGER trg_iib_bi_integrity BEFORE INSERT ON invoice_item_batches FOR EACH ROW
BEGIN
    DECLARE v_item_product BIGINT; DECLARE v_item_qty INT; DECLARE v_inv_store BIGINT;
    DECLARE v_batch_product BIGINT; DECLARE v_batch_store BIGINT; DECLARE v_batch_qty INT;
    DECLARE v_alloc INT; DECLARE v_sold INT;
    SELECT ii.product_id, ii.quantity, i.store_id INTO v_item_product, v_item_qty, v_inv_store
      FROM invoice_items ii JOIN invoices i ON i.id = ii.invoice_id WHERE ii.id = NEW.invoice_item_id;
    SELECT gri.product_id, gr.store_id, gri.quantity INTO v_batch_product, v_batch_store, v_batch_qty
      FROM goods_receipt_items gri JOIN goods_receipts gr ON gr.id = gri.receipt_id WHERE gri.id = NEW.batch_id;
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
END $$

DROP TRIGGER IF EXISTS trg_st_bi_integrity $$
CREATE TRIGGER trg_st_bi_integrity BEFORE INSERT ON shelf_transfers FOR EACH ROW
BEGIN
    DECLARE v_batch_store BIGINT; DECLARE v_shelf_store BIGINT; DECLARE v_other_shelf BIGINT;
    SELECT gr.store_id INTO v_batch_store FROM goods_receipt_items gri
      JOIN goods_receipts gr ON gr.id = gri.receipt_id WHERE gri.id = NEW.batch_id;
    SELECT store_id INTO v_shelf_store FROM shelves WHERE id = NEW.shelf_id;
    IF v_batch_store <> v_shelf_store THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Vi pham toan ven: len ke khac chi nhanh voi lo';
    END IF;
    SELECT MIN(shelf_id) INTO v_other_shelf FROM shelf_transfers
      WHERE batch_id = NEW.batch_id AND shelf_id <> NEW.shelf_id;
    IF v_other_shelf IS NOT NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Vi pham toan ven: mot lo chi duoc nam tren mot ke';
    END IF;
END $$

DROP TRIGGER IF EXISTS trg_sr_bi_integrity $$
CREATE TRIGGER trg_sr_bi_integrity BEFORE INSERT ON shelf_returns FOR EACH ROW
BEGIN
    DECLARE v_batch_store BIGINT; DECLARE v_shelf_store BIGINT;
    SELECT gr.store_id INTO v_batch_store FROM goods_receipt_items gri
      JOIN goods_receipts gr ON gr.id = gri.receipt_id WHERE gri.id = NEW.batch_id;
    SELECT store_id INTO v_shelf_store FROM shelves WHERE id = NEW.shelf_id;
    IF v_batch_store <> v_shelf_store THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Vi pham toan ven: lay hang tu ke khac chi nhanh voi lo';
    END IF;
END $$

DROP TRIGGER IF EXISTS trg_inv_bi_store $$
CREATE TRIGGER trg_inv_bi_store BEFORE INSERT ON invoices FOR EACH ROW
BEGIN
    DECLARE v_shift_store BIGINT;
    SELECT store_id INTO v_shift_store FROM work_shifts WHERE id = NEW.shift_id;
    IF v_shift_store IS NOT NULL AND NEW.store_id <> v_shift_store THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Vi pham toan ven: hoa don khac chi nhanh voi ca lam viec';
    END IF;
END $$

DROP TRIGGER IF EXISTS trg_ws_bi_oneopen $$
CREATE TRIGGER trg_ws_bi_oneopen BEFORE INSERT ON work_shifts FOR EACH ROW
BEGIN
    IF NEW.status = 'OPEN'
       AND EXISTS (SELECT 1 FROM work_shifts WHERE user_id = NEW.user_id AND status = 'OPEN') THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Vi pham toan ven: nhan vien da co mot ca dang mo';
    END IF;
END $$

DELIMITER ;

-- =====================================================================
--  DỮ LIỆU MẪU
-- =====================================================================
-- Chi nhánh (đa chuỗi): CH01 = chi nhánh mặc định cho toàn bộ dữ liệu mẫu bên dưới.
INSERT INTO stores (id, code, name, address, phone) VALUES
(1, 'CH01', 'MiniMart — Chi nhánh 1 (Quận 1)', '123 Đường Lê Lợi, Q.1, TP.HCM', '0901234567'),
(2, 'CH02', 'MiniMart — Chi nhánh 2 (Thủ Đức)', '45 Võ Văn Ngân, TP.Thủ Đức', '0902223344');

-- Cấu hình MỖI chi nhánh một dòng (id = stores.id). Giá trị API/ngân hàng/token là PLACEHOLDER GIẢ.
INSERT INTO store_config
 (id, name, address, phone, tax_code,
  bank_name, bank_bin, bank_account_no, bank_account_name, transfer_prefix,
  web2m_api_url, telegram_bot_token, telegram_enabled,
  notify_payment, notify_low_stock, notify_new_invoice) VALUES
(1, 'MiniMart — Chi nhánh 1 (Quận 1)', '123 Đường Lê Lợi, Q.1, TP.HCM', '0901234567', '0312345678',
 'MB Bank', '970422', 'xxxxxxxxxxxx', 'CHU TAI KHOAN', 'POS',
 'https://api.web2m.com/historyapiopenmb/YOUR_WEB2M_TOKEN', 'YOUR_TELEGRAM_BOT_TOKEN', 0, 1, 1, 0),
(2, 'MiniMart — Chi nhánh 2 (Thủ Đức)', '45 Võ Văn Ngân, TP.Thủ Đức', '0902223344', '0312345678',
 'Vietcombank', '970436', 'yyyyyyyyyyyy', 'CHU TAI KHOAN', 'POS',
 NULL, NULL, 0, 1, 1, 0);

-- Danh sách Chat ID nhận thông báo Telegram theo chi nhánh (giá trị giả - thay khi dùng thật)
INSERT INTO telegram_recipients (config_id, chat_id, label) VALUES
(1, '100000001', 'Người nhận 1 (CN1)'),
(1, '100000002', 'Người nhận 2 (CN1)');

-- LƯU Ý: hash mẫu dưới đây là BCrypt của chuỗi "password" (hash kinh điển trong tài liệu Spring).
--   Backend có DemoDataInitializer sẽ TỰ ĐẶT LẠI mật khẩu 3 tài khoản này = "123456" khi khởi động
--   (chỉ khi chưa khớp), nên đăng nhập demo dùng mật khẩu: 123456.
INSERT INTO users (username, password_hash, full_name, role, store_id) VALUES
('admin',    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Quản trị viên (toàn chuỗi)', 'ADMIN',  NULL),
('manager',  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Quản lý cửa hàng 1', 'MANAGER', 1),
('staff',    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Nhân viên cửa hàng 1', 'STAFF',  1),
('manager2', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Quản lý cửa hàng 2', 'MANAGER', 2),
('staff2',   '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Nhân viên cửa hàng 2', 'STAFF',  2);

INSERT INTO units (name) VALUES ('Lon'), ('Chai'), ('Gói'), ('Thùng'), ('Hộp');

INSERT INTO categories (name, description) VALUES
('Nước giải khát', 'Nước ngọt, nước suối, bia...'),
('Đồ ăn nhanh',    'Mì gói, snack, bánh kẹo...'),
('Hàng tiêu dùng', 'Hóa mỹ phẩm, đồ dùng...'),
('Đồ đông lạnh',   'Kem, đồ đông lạnh...');

INSERT INTO suppliers (name, phone, email, address) VALUES
('Công ty Tân Hiệp Phát', '02838123456', 'sales@thp.com.vn', 'Bình Dương'),
('Acecook Việt Nam',      '02838234567', 'cskh@acecook.vn',  'TP.HCM');

INSERT INTO products (barcode, name, category_id, unit_id, cost_price, sale_price, min_stock) VALUES
('8934588012345', 'Trà xanh không độ 500ml', 1, 2,  6000, 10000, 24),
('8934588023456', 'Nước suối Aquafina 500ml', 1, 2,  3000,  5000, 24),
('8936000012347', 'Mì Hảo Hảo tôm chua cay', 2, 3,  3000,  4500, 30),
('8934567023458', 'Snack Oishi',              2, 3,  4000,  7000, 20),
('8938505970019', 'Coca-Cola lon 330ml',      1, 1,  6500, 10000, 24),
('8935001712345', 'Kem Merino',               4, 5,  8000, 12000, 10);

-- Phiếu nhập + lô hàng (batch = goods_receipt_items)
INSERT INTO goods_receipts (code, store_id, supplier_id, created_by, total_amount, note) VALUES
('PN20260601-001', 1, 1, 2, 1230000, 'Nhập đầu tháng 6');
INSERT INTO goods_receipt_items (receipt_id, product_id, quantity, import_price, expiry_date) VALUES
(1, 1, 48, 6000, '2026-12-31'),   -- batch id 1: Trà xanh
(1, 2, 48, 3000, '2027-06-30'),   -- batch id 2: Aquafina
(1, 5, 48, 6500, '2026-10-31');   -- batch id 3: Coca

INSERT INTO goods_receipts (code, store_id, supplier_id, created_by, total_amount, note) VALUES
('PN20260601-002', 1, 2, 2, 360000, 'Nhập mì & snack');
INSERT INTO goods_receipt_items (receipt_id, product_id, quantity, import_price, expiry_date) VALUES
(2, 3, 60, 3000, '2026-09-30'),   -- batch id 4: Mì Hảo Hảo
(2, 4, 40, 4000, '2026-08-15');   -- batch id 5: Snack Oishi

-- Kệ vật lý của chi nhánh 1: mỗi danh mục một kệ (kèm sức chứa)
INSERT INTO shelves (store_id, code, name, capacity) VALUES
(1, 'K01', 'Nước giải khát', 500), (1, 'K02', 'Đồ ăn nhanh', 500),
(1, 'K03', 'Hàng tiêu dùng', 500), (1, 'K04', 'Đồ đông lạnh', 300);

-- Đưa toàn bộ lô mẫu LÊN KỆ của danh mục tương ứng (kho → kệ) để bán được ngay (created_by = manager id 2)
INSERT INTO shelf_transfers (batch_id, shelf_id, quantity, created_by)
SELECT gri.id, sh.id, gri.quantity, 2
FROM goods_receipt_items gri
JOIN products   p  ON p.id  = gri.product_id
JOIN categories c  ON c.id  = p.category_id
JOIN shelves    sh ON sh.name = c.name;

INSERT INTO customers (full_name, phone, email, loyalty_points) VALUES
('Nguyễn Văn An', '0987654321', 'an.nguyen@email.com', 50),
('Trần Thị Bình', '0978123456', NULL, 0);

INSERT INTO promotions (code, name, discount_type, discount_value, min_order_amount, start_date, end_date, usage_limit) VALUES
('SALE10',  'Giảm 10% đơn từ 100k', 'PERCENT', 10, 100000, '2026-06-01 00:00:00', '2026-06-30 23:59:59', 100),
('GIAM20K', 'Giảm 20k đơn từ 200k', 'AMOUNT',  20000, 200000, '2026-06-01 00:00:00', '2026-12-31 23:59:59', NULL);

INSERT INTO work_shifts (store_id, user_id, opening_cash, status) VALUES
(1, 3, 500000, 'OPEN');

-- Hóa đơn mẫu: 2 Trà xanh (10000) + 1 Mì (4500) = 24500
INSERT INTO invoices (code, store_id, shift_id, customer_id, subtotal, discount_amount, payment_method, customer_paid, change_amount, points_earned) VALUES
('HD20260606-0001', 1, 1, 1, 24500, 0, 'CASH', 50000, 25500, 2);
INSERT INTO invoice_items (invoice_id, product_id, quantity, unit_price) VALUES
(1, 1, 2, 10000),   -- invoice_item id 1: Trà xanh x2
(1, 3, 1,  4500);   -- invoice_item id 2: Mì x1
-- Phân bổ tồn theo lô (FIFO): lấy từ batch tương ứng
INSERT INTO invoice_item_batches (invoice_item_id, batch_id, quantity) VALUES
(1, 1, 2),   -- 2 Trà xanh lấy từ batch 1
(2, 4, 1);   -- 1 Mì lấy từ batch 4

-- Hóa đơn 2: thanh toán QR (VietQR + WEB2M), đang CHỜ đối soát tự động
INSERT INTO invoices (code, store_id, shift_id, subtotal, discount_amount, payment_method, points_earned) VALUES
('HD20260606-0002', 1, 1, 10000, 0, 'QR', 1);
INSERT INTO invoice_items (invoice_id, product_id, quantity, unit_price) VALUES
(2, 5, 1, 10000);   -- invoice_item id 3: 1 Coca-Cola
INSERT INTO invoice_item_batches (invoice_item_id, batch_id, quantity) VALUES
(3, 3, 1);          -- lấy từ batch 3 (Coca)
-- Giao dịch QR chờ WEB2M xác nhận. Nội dung CK = '<prefix> <mã HĐ>' để WEB2M đối soát.
INSERT INTO payment_transactions (invoice_id, amount, transfer_content, status) VALUES
(2, 10000, 'POS HD20260606-0002', 'PENDING');

-- =====================================================================
--  KIỂM TRA NHANH
-- =====================================================================
-- SELECT * FROM v_batch_stock;        -- tồn từng lô (batch 1 còn 46, batch 4 còn 59)
-- SELECT * FROM v_product_stock;      -- tồn từng sản phẩm (lọc WHERE current_stock<=min_stock để xem tồn thấp)
-- SELECT * FROM v_customer_spending;  -- An: 24500
-- SELECT * FROM v_shift_summary;      -- ca 1: 24500 + 10000 = 34500
-- SELECT * FROM v_pending_payments;   -- HĐ HD20260606-0002 chờ đối soát QR
-- Hủy thử:  UPDATE invoices SET status='CANCELLED' WHERE id=1;  -> tồn batch 1,4 tự hoàn về 48,60
-- SELECT * FROM telegram_recipients WHERE is_active=1;  -- người nhận thông báo
-- WEB2M khớp tiền: UPDATE payment_transactions SET status='PAID', bank_reference='FT26...', paid_at=NOW() WHERE transfer_content='POS HD20260606-0002';
