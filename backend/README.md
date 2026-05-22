# Backend — POS Cửa hàng tiện lợi (Spring Boot REST API)

REST API cho hệ thống POS: bán hàng (FIFO theo lô/HSD), kho, hóa đơn, khách hàng,
khuyến mãi, dashboard/báo cáo, tích hợp VietQR / WEB2M / Telegram.

## 1. Yêu cầu môi trường

| Thành phần | Phiên bản |
|-----------|-----------|
| JDK | **17+** (bắt buộc — máy hiện chưa có, cần cài) |
| Maven | 3.9+ (hoặc mở bằng IntelliJ/Eclipse đã tích hợp Maven) |
| MySQL | 8.x (ServBay cung cấp, cổng `3306`) |

> Cài JDK nhanh trên Windows: `winget install Microsoft.OpenJDK.17`
> rồi mở lại terminal. Hoặc mở thư mục `backend/` bằng **IntelliJ IDEA** (tự tải Maven + JDK).

## 2. Chuẩn bị cơ sở dữ liệu

1. Mở **ServBay**, đảm bảo MySQL đang chạy ở `localhost:3306`.
2. Vào **phpMyAdmin** → **Import** file [`../sql/schema.sql`](../sql/schema.sql)
   (tạo DB `pos_convenience_store` gồm 16 bảng + 7 view + dữ liệu mẫu).

Cấu hình kết nối trong [`src/main/resources/application.yml`](src/main/resources/application.yml)
mặc định `root` / `root` — khớp ServBay. Đổi `DB_PASSWORD` nếu MySQL của bạn khác.

## 3. Chạy backend

```bash
cd backend
mvn spring-boot:run          # chạy ở http://localhost:8080
# hoặc đóng gói:  mvn clean package  &&  java -jar target/pos-convenience-store-1.0.0.jar
```

Khi khởi động, `DemoDataInitializer` tự đảm bảo 3 tài khoản demo đăng nhập được bằng **`123456`**.

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
| Ca/Bán hàng | `POST /api/shifts/open`, `GET /api/shifts/current`, `POST /api/invoices` |
| Hóa đơn | `GET /api/invoices`, `POST /api/invoices/{id}/cancel`, `GET /api/invoices/{id}/pdf` |
| Khách/KM | `/api/customers`, `/api/customers/{id}/history`, `/api/promotions`, `POST /api/promotions/validate` |
| Kho/Dashboard/Báo cáo | `/api/inventory/*`, `/api/dashboard`, `/api/reports/revenue`, `/api/reports/export` |
| Cấu hình/Tích hợp | `/api/store-config`, `/api/payments/qr`, `/api/payments/{id}/status`, `POST /api/payments/web2m/sync`, `/api/integrations/*` |

## 8. Bật đối soát WEB2M tự động (tùy chọn)

Job nền tắt mặc định. Bật bằng biến môi trường rồi chạy lại:
```bash
# PowerShell
$env:WEB2M_ENABLED="true"; mvn spring-boot:run
```
Và nhập `web2m_api_url` thật trong màn hình cấu hình (`PUT /api/store-config`).
