# Backend — POS Cửa hàng tiện lợi (Spring Boot REST API)

REST API cho hệ thống POS: bán hàng (FIFO theo lô/HSD), **kho ↔ kệ** (lên kệ/về kho), hóa đơn,
**trả hàng/hoàn tiền**, khách hàng + **sổ cái điểm**, khuyến mãi, **VAT**, dashboard/báo cáo,
**nhật ký kiểm toán**, tích hợp VietQR / WEB2M / Telegram.

## 1. Yêu cầu môi trường

| Thành phần | Phiên bản |
|-----------|-----------|
| JDK | **17+** (bắt buộc) |
| Maven | 3.9+ (hoặc mở bằng IntelliJ/Eclipse đã tích hợp Maven) |
| MySQL | 8.x (ServBay cung cấp, cổng `3306`) |

> Cài JDK nhanh trên Windows: `winget install Microsoft.OpenJDK.17` rồi mở lại terminal.

## 2. Cơ sở dữ liệu — TỰ TẠO khi khởi động (không cần import tay)

Chỉ cần **MySQL đang chạy** ở `localhost:3306`. Khi `mvn spring-boot:run`, backend tự:
1. **Tạo database** `pos_convenience_store` nếu chưa có (`createDatabaseIfNotExist=true`).
2. Chạy [`src/main/resources/db/schema.sql`](src/main/resources/db/schema.sql) (`spring.sql.init.mode=always`)
   tạo **23 bảng + 7 view** — *idempotent* (`CREATE TABLE IF NOT EXISTS` + migration kiểm tra `information_schema`,
   nên DB cũ tự được bổ sung cột mới).
3. Chạy các seeder (`BaseDataSeeder`, `CatalogDemoDataInitializer`) tạo 3 tài khoản + ~69 sản phẩm + tồn kho + kệ.

→ **Xoá DB rồi khởi động lại là có dữ liệu demo ngay.** Bản import tay [`../sql/schema.sql`](../sql/schema.sql)
chỉ là tuỳ chọn (khi muốn dựng DB ngoài ứng dụng).

Cấu hình kết nối trong [`src/main/resources/application.yml`](src/main/resources/application.yml)
mặc định `root` / `root`. Đổi qua biến môi trường `DB_PASSWORD` nếu MySQL của bạn khác.

## 3. Chạy backend

```bash
cd backend
mvn spring-boot:run          # chạy ở http://localhost:8080
# hoặc đóng gói:  mvn clean package  &&  java -jar target/pos-convenience-store-1.0.0.jar
```

Khi khởi động, `BaseDataSeeder` tự đảm bảo 3 tài khoản demo đăng nhập được bằng **`123456`**.

## 4. Tài khoản demo

| Username | Mật khẩu | Vai trò |
|----------|----------|---------|
| `admin` | `123456` | ADMIN — toàn quyền |
| `manager` | `123456` | MANAGER — quản trị vận hành |
| `cashier` | `123456` | CASHIER — bán hàng POS |

## 5. Thử nhanh bằng curl

```bash
# Đăng nhập lấy JWT
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"123456"}'

# Gọi API có token
curl http://localhost:8080/api/products -H "Authorization: Bearer <TOKEN>"
```

## 6. Kiến trúc & quy ước

- Phân lớp: **Controller (`@RestController`) → Service (`@Transactional`) → Repository (JPA) → Entity**.
- Bảo mật: **JWT** (header `Authorization: Bearer ...`), **BCrypt**, phân quyền `@PreAuthorize` theo vai trò, CORS cho `http://localhost:5173`.
- Phản hồi thống nhất: `{ success, message, data, timestamp }` (xem `common/ApiResponse`).
- Lỗi tập trung: `exception/GlobalExceptionHandler` (400/401/403/404/409/500).
- Tồn kho **không lưu cột** — suy ra từ lô qua view; bán hàng trừ **FIFO theo HSD**,
  ghi `invoice_item_batches`; **hủy HĐ → tồn tự hoàn** (chỉ đổi `status=CANCELLED`).

## 7. Bản đồ endpoint chính

| Nhóm | Endpoint tiêu biểu |
|------|--------------------|
| Auth | `POST /api/auth/login`, `GET /api/auth/me` |
| Danh mục/SP | `/api/categories`, `/api/units`, `/api/products`, `/api/products/barcode/{code}` |
| NCC/Nhập kho | `/api/suppliers`, `/api/goods-receipts` |
| **Kệ** | `GET /api/shelves`, `GET /api/shelves/{id}/inventory`, `POST /api/shelves/transfer` (lên kệ), `POST /api/shelves/return` (về kho), `POST/PUT/DELETE /api/shelves` (cấu hình) |
| Ca/Bán hàng | `POST /api/shifts/open`, `GET /api/shifts/current`, `POST /api/invoices` |
| Hóa đơn | `GET /api/invoices`, `POST /api/invoices/{id}/cancel`, `GET /api/invoices/{id}/pdf` |
| **Trả hàng** | `GET /api/returns/invoice/{id}/returnable`, `POST /api/returns` |
| Khách/KM | `/api/customers`, `/api/customers/{id}/history`, `/api/promotions`, `POST /api/promotions/validate` |
| Kho/Dashboard/Báo cáo | `/api/inventory/*` (gồm `/suggestions`, `/abc-xyz`), `/api/dashboard`, `/api/reports/revenue`, `/api/reports/export` |
| **Kiểm toán** | `GET /api/audit` (ADMIN) |
| Cấu hình/Tích hợp | `/api/store-config`, `/api/payments/{id}/status`, `/api/integrations/*` |

## 8. Bật đối soát WEB2M tự động (tùy chọn)

Job nền tắt mặc định. Bật bằng biến môi trường rồi chạy lại:
```bash
# PowerShell
$env:WEB2M_ENABLED="true"; mvn spring-boot:run
```
Và nhập `web2m_api_url` thật trong màn hình cấu hình (`PUT /api/store-config`).
