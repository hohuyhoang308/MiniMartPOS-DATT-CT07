# 06. BÁO CÁO ĐỒ ÁN

> Tài liệu tổng hợp bám **Bảng tiêu chí chấm điểm (10đ)**. Các phần chi tiết tham chiếu sang
> `02_SRS`, `04_Thiet_ke_CSDL_ERD`, `05_Thiet_ke_UML`. Bổ sung **đặc tả REST API** và **cấu trúc
> Frontend React** để code bám sát tài liệu.

## 6.1. Tổng quan đề tài

- **Tên:** Website POS cho cửa hàng tiện lợi (Đề tài số 10).
- **Mục tiêu:** thu ngân bán hàng nhanh tại quầy bằng quét mã vạch; quản lý kiểm soát kho – doanh thu –
  nhân viên – khách hàng theo thời gian thực.
- **Hình thức:** đồ án **cá nhân (1 người)** thực hiện toàn bộ FE + BE + CSDL + tài liệu.
- **Công nghệ chốt:** Frontend **React (Vite)** · Backend **Java – Spring Boot REST API** ·
  CSDL **MySQL 8** (quản trị qua phpMyAdmin/ServBay).

## 6.2. Phân tích yêu cầu (Mục 1)

Tóm tắt — chi tiết ở [`02_SRS_Dac_ta_yeu_cau.md`](02_SRS_Dac_ta_yeu_cau.md):
- **Actor:** Admin (Chủ cửa hàng), Manager (Quản lý), Cashier (Thu ngân), System.
- **10 nhóm chức năng (FR1–FR10)** + nhóm nâng cao (QR, PDF, chatbot).
- **21 Use Case** (UC01–UC21), sơ đồ ở [`03_Use_Case_Diagram.md`](03_Use_Case_Diagram.md).
- **Yêu cầu phi chức năng:** hiệu năng (<1s tra mã vạch), bảo mật (BCrypt + JWT), toàn vẹn
  (transaction), khả dụng (POS tối giản).

## 6.3. Thiết kế hệ thống (Mục 2)

### 6.3.1. Kiến trúc

Mô hình **client–server tách lớp**: React SPA ↔ REST API (Spring Boot) ↔ MySQL.
Backend phân lớp **Controller → Service → Repository → Entity** (sơ đồ ở `05` mục 5.1).

| Lớp | Trách nhiệm |
|-----|-------------|
| Controller (`@RestController`) | Nhận request, validate đầu vào, trả JSON |
| Service (`@Service`, `@Transactional`) | Xử lý nghiệp vụ, đảm bảo giao dịch |
| Repository (Spring Data JPA) | Truy xuất CSDL, truy vấn tùy biến |
| Entity | Ánh xạ bảng (xem `04`) |

### 6.3.2. CSDL & UML

- **ERD + đặc tả bảng:** [`04_Thiet_ke_CSDL_ERD.md`](04_Thiet_ke_CSDL_ERD.md) — 16 bảng + 7 view,
  chuẩn 3NF, **không bảng/cột thừa**, đã kiểm thử chạy trên MySQL 8.4.7.
- **Class/Sequence/Activity Diagram:** [`05_Thiet_ke_UML.md`](05_Thiet_ke_UML.md).

## 6.4. Đặc tả REST API (để code bám theo)

> Tiền tố chung `/api`. Trả JSON. Xác thực bằng **JWT** ở header `Authorization: Bearer <token>`
> (trừ `/api/auth/login`). Cột quyền: A=Admin, M=Manager, C=Cashier.

### Xác thực (FR1)
| Method | Endpoint | Mô tả | Quyền |
|--------|----------|-------|:-----:|
| POST | `/api/auth/login` | Đăng nhập, trả JWT | tất cả |
| POST | `/api/auth/logout` | Đăng xuất (client xóa token) | đã login |
| GET | `/api/auth/me` | Thông tin tài khoản hiện tại | đã login |

### Tài khoản (FR1.3 - UC02)
| Method | Endpoint | Mô tả | Quyền |
|--------|----------|-------|:-----:|
| GET/POST | `/api/users` | Danh sách / thêm nhân viên | A |
| PUT/DELETE | `/api/users/{id}` | Sửa / khóa | A |
| PUT | `/api/users/{id}/reset-password` | Đặt lại mật khẩu | A |

### Danh mục – Đơn vị – Sản phẩm (FR2)
| Method | Endpoint | Mô tả | Quyền |
|--------|----------|-------|:-----:|
| GET/POST/PUT/DELETE | `/api/categories` | CRUD danh mục | M (GET: C) |
| GET/POST/PUT/DELETE | `/api/units` | CRUD đơn vị tính | M (GET: C) |
| GET/POST/PUT/DELETE | `/api/products` | CRUD sản phẩm; query `?keyword=&categoryId=` | M (GET: C) |
| GET | `/api/products/barcode/{code}` | Tra cứu nhanh theo mã vạch (POS) | C |

### Nhà cung cấp & Nhập kho (FR3 - UC07)
| Method | Endpoint | Mô tả | Quyền |
|--------|----------|-------|:-----:|
| GET/POST/PUT/DELETE | `/api/suppliers` | CRUD nhà cung cấp | M |
| GET/POST | `/api/goods-receipts` | Lịch sử / lập phiếu nhập (tăng tồn) | M |
| GET | `/api/goods-receipts/{id}` | Chi tiết phiếu nhập | M |

### Ca làm việc & Bán hàng (FR4 - UC08/09/10)
| Method | Endpoint | Mô tả | Quyền |
|--------|----------|-------|:-----:|
| POST | `/api/shifts/open` | Mở ca (nhập tiền đầu ca) | C |
| POST | `/api/shifts/{id}/close` | Đóng ca (đối soát) | C |
| GET | `/api/shifts/current` | Ca đang mở của tôi | C |
| POST | `/api/invoices` | **Tạo hóa đơn** (transaction: trừ tồn, tích điểm) | C |
| GET | `/api/invoices` | Danh sách HĐ; query `?date=&customerId=&status=` | M (C: ca mình) |
| GET | `/api/invoices/{id}` | Chi tiết hóa đơn | M/C |
| POST | `/api/invoices/{id}/cancel` | Hủy hóa đơn → hoàn tồn | M |
| GET | `/api/invoices/{id}/pdf` | Xuất hóa đơn PDF (UC12) | M/C |

### Khuyến mãi – Khách hàng (FR6, FR7)
| Method | Endpoint | Mô tả | Quyền |
|--------|----------|-------|:-----:|
| GET/POST/PUT/DELETE | `/api/customers` | CRUD khách thân thiết | M (C: xem/thêm) |
| GET | `/api/customers/{id}/history` | Lịch sử mua & tổng chi tiêu | M/C |
| GET/POST/PUT/DELETE | `/api/promotions` | CRUD khuyến mãi | M |
| POST | `/api/promotions/validate` | Kiểm tra mã hợp lệ khi áp dụng | C |

### Kho – Dashboard – Báo cáo (FR8, FR9)
| Method | Endpoint | Mô tả | Quyền |
|--------|----------|-------|:-----:|
| GET | `/api/inventory/stock` | Tồn kho hiện tại (`v_product_stock`) | M |
| GET | `/api/inventory/low-stock` | Cảnh báo tồn thấp | M |
| GET | `/api/inventory/expiring` | Cảnh báo cận/quá HSD | M |
| GET | `/api/dashboard` | Doanh thu hôm nay/tháng, biểu đồ, top SP | M |
| GET | `/api/reports/revenue?from=&to=` | Báo cáo doanh thu | M |
| GET | `/api/reports/export?type=excel\|pdf` | Xuất Excel/PDF | M |

### Cấu hình & Nâng cao (FR10, FR-A)
| Method | Endpoint | Mô tả | Quyền |
|--------|----------|-------|:-----:|
| GET/PUT | `/api/store-config` | Cấu hình cửa hàng + bank + WEB2M + Telegram | A |
| GET | `/api/payments/qr?invoiceId=` | **Hiển thị VietQR**: trả URL ảnh QR theo số tiền + nội dung CK | C |
| GET | `/api/payments/{invoiceId}/status` | Trạng thái thanh toán (PENDING/PAID) — FE poll | C |
| POST | `/api/payments/web2m/sync` | Kích hoạt **đối soát WEB2M** thủ công (bình thường chạy job nền) | A/M |
| POST | `/api/integrations/web2m/test` | Kiểm tra kết nối API WEB2M | A |
| GET/POST/DELETE | `/api/integrations/telegram/recipients` | Quản lý danh sách Chat ID | A |
| POST | `/api/integrations/telegram/test` | Gửi tin nhắn thử | A |

> **Đối soát tự động:** một **job nền** (`@Scheduled`) định kỳ gọi `web2m_api_url`, khớp giao dịch với
> các `payment_transactions` đang `PENDING` (theo số tiền + nội dung CK) → cập nhật `PAID` + bắn Telegram.
> Không cần webhook public; nếu muốn realtime có thể thay bằng webhook WEB2M.

## 6.5. Cấu trúc Frontend React (UC ↔ trang)

| Trang React | Route | Use Case | Quyền |
|-------------|-------|----------|:-----:|
| Login | `/login` | UC01 | tất cả |
| POS bán hàng | `/pos` | UC08–UC12, UC21 | C |
| Sản phẩm / Danh mục / Đơn vị | `/products` … | UC03–UC05 | M |
| Nhà cung cấp / Nhập kho | `/suppliers`, `/receipts` | UC06, UC07 | M |
| Hóa đơn | `/invoices` | UC13, UC14 | M/C |
| Khách hàng / Khuyến mãi | `/customers`, `/promotions` | UC15, UC16 | M |
| Tồn kho & cảnh báo | `/inventory` | UC17 | M |
| Dashboard / Báo cáo | `/dashboard`, `/reports` | UC18, UC19 | M |
| Tài khoản / Cấu hình | `/users`, `/settings` | UC02, UC20 | A |

- **Phân quyền FE:** `PrivateRoute` đọc `role` trong JWT, chặn route trái phép (đồng bộ với chặn ở BE).
- **Trạng thái:** `AuthContext` (token, role), `CartContext` (giỏ hàng POS).
- **Gọi API:** Axios instance đính token tự động; cấu hình proxy `/api` → `http://localhost:8080`.

## 6.6. Bảo mật (Mục 5)

| Hạng mục | Giải pháp |
|----------|-----------|
| Mật khẩu | Băm **BCrypt**, không lưu plaintext |
| Xác thực | **JWT** (stateless), token chứa vai trò |
| Phân quyền | `@PreAuthorize` theo vai trò ở backend + `PrivateRoute` ở frontend |
| CORS | Chỉ cho origin của frontend |
| SQL Injection | JPA tham số hóa, không nối chuỗi |
| XSS | React tự escape khi render; validate đầu vào |
| Toàn vẹn | `@Transactional` cho bán hàng/nhập kho; ràng buộc FK/CHECK trong CSDL |

## 6.7. Dashboard & Báo cáo (Mục 6)

- **Dashboard:** doanh thu hôm nay/tháng, số hóa đơn, **biểu đồ doanh thu theo ngày** (Recharts/Chart.js),
  top sản phẩm bán chạy, số mặt hàng tồn thấp.
- **Báo cáo:** doanh thu theo khoảng thời gian, theo ca/thu ngân (`v_shift_summary`), tồn kho; **xuất
  Excel/PDF**.

## 6.8. Tính năng nâng cao (Mục 9)

| Tính năng | Mô tả | Thư viện / dịch vụ |
|-----------|-------|----------------|
| **Hiển thị QR (VietQR)** | Chỉ render mã QR banking (số tiền + nội dung CK) cho khách quét | img.vietqr.io / thư viện QR |
| **Đối soát ngân hàng tự động (WEB2M)** | Job nền poll lịch sử giao dịch, khớp số tiền + nội dung CK → xác nhận đã thu tiền | API WEB2M (`historyapiopenmb`) |
| **Thông báo Telegram Bot** | Báo tự động: nhận thanh toán, tồn thấp, hóa đơn mới | Telegram Bot API |
| Xuất **hóa đơn PDF** | In khổ 80mm / gửi email | iText/OpenPDF (BE) hoặc jsPDF (FE) |
| **Chatbot/cảnh báo thông minh** (tùy chọn) | Tra nhanh SP tồn thấp, gợi ý nhập hàng | Claude API |

> **Phân vai rõ:** VietQR = **hiển thị** mã QR; WEB2M = **API đối soát** (poll giao dịch về để xác nhận
> tiền vào). Hai cái độc lập, không nhầm lẫn.

## 6.9. Triển khai (Mục 7)

```bash
# 1. CSDL: import schema (đã có dữ liệu mẫu)
#    phpMyAdmin > Import > sql/schema.sql   (hoặc: mysql -uroot -p < sql/schema.sql)

# 2. Backend (cổng 8080)
cd backend && mvn spring-boot:run

# 3. Frontend (cổng 5173, proxy /api -> 8080)
cd frontend && npm install && npm run dev
```

- **Tài khoản demo** (mật khẩu `123456`): `admin` / `manager` / `cashier`.
- Cấu hình kết nối DB + tích hợp trong `backend/src/main/resources/application.yml`.

**Mẫu `application.yml`** (giá trị nhạy cảm nên đặt qua **biến môi trường**, không hard-code/commit):

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/pos_convenience_store
    username: root
    password: ${DB_PASSWORD:root}
  jpa:
    hibernate.ddl-auto: validate     # CSDL tạo bằng sql/schema.sql, không để JPA tự sửa

app:
  jwt:
    secret: ${JWT_SECRET:doi-secret-nay-trong-production}
    expiration-ms: 86400000
  cors:
    allowed-origins: http://localhost:5173   # origin frontend React

  # Tích hợp (mặc định lấy từ store_config trong DB; biến môi trường để override khi deploy)
  web2m:
    api-url: ${WEB2M_API_URL:}          # https://api.web2m.com/historyapiopenmb/<token>
    poll-interval-ms: 30000              # chu kỳ job đối soát
  telegram:
    bot-token: ${TELEGRAM_BOT_TOKEN:}
```

> **Bảo mật:** WEB2M URL/token, Telegram bot token, JWT secret **không commit vào Git** — dùng biến môi
> trường hoặc file `.env` đã `.gitignore`. Trong `sql/schema.sql` các giá trị này chỉ là **placeholder giả**.

## 6.10. Kiểm thử (QA)

| Loại | Phạm vi |
|------|---------|
| Unit test | Service nghiệp vụ (tính tiền, áp KM, trừ tồn FIFO) — JUnit |
| Integration | REST API qua MockMvc / Postman |
| Kiểm thử giao diện | Luồng POS, login bằng **Playwright** (skill `webapp-testing`) |
| Test dữ liệu | Dữ liệu mẫu trong `schema.sql` + view kiểm tra tồn/cảnh báo |

## 6.11. Kết luận & hướng phát triển

- **Đạt được:** hệ thống POS đầy đủ nghiệp vụ bán hàng – kho – khách hàng – báo cáo; kiến trúc
  React + Spring Boot REST tách lớp rõ; CSDL chuẩn hóa không dư thừa, đã kiểm thử chạy thật.
- **Hướng phát triển:** đa chi nhánh, app di động, tích hợp máy POS phần cứng, gợi ý nhập hàng bằng AI.

## 6.12. Ánh xạ tiêu chí ↔ tài liệu (đối chiếu chấm điểm)

| # | Tiêu chí | Điểm | Bằng chứng |
|---|----------|------|------------|
| 1 | Phân tích yêu cầu | 1.0 | `02_SRS`, `03_Use_Case` |
| 2 | Thiết kế hệ thống | 1.5 | `04_ERD`, `05_UML`, mục 6.3 |
| 3 | Chức năng POS | 2.0 | `02_SRS` + code, mục 6.4 |
| 4 | Giao diện & UX | 1.0 | Frontend React, mục 6.5 |
| 5 | CSDL, validation, bảo mật | 1.0 | `04_ERD`, mục 6.6 |
| 6 | Dashboard, báo cáo | 0.75 | mục 6.7 |
| 7 | Triển khai & demo | 1.0 | `sql/schema.sql`, mục 6.9 |
| 8 | Báo cáo, slide | 0.75 | tài liệu này |
| 9 | Tính năng nâng cao | 0.5 | mục 6.8 |
| 10 | Chất lượng mã & quản lý | 0.5 | `01_Ke_hoach`, Git |
