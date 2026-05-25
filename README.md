# 🛒 POS — Hệ thống bán hàng cho cửa hàng tiện lợi

> **Đồ án Công nghệ phần mềm** — Phân tích, thiết kế, xây dựng và triển khai **website POS** cho mô hình kinh doanh thực tế (cửa hàng tiện lợi / minimart).

Hệ thống quản lý bán hàng tại quầy (Point of Sale) đầy đủ luồng nghiệp vụ: đăng nhập phân quyền, quản lý sản phẩm theo **lô & hạn sử dụng (FIFO)**, bán hàng — thanh toán **tiền mặt / QR (VietQR + đối soát WEB2M)**, hóa đơn, nhập kho, khách hàng thân thiết (**tích & đổi điểm**), khuyến mãi, **quản lý ca + đối soát quỹ**, **đề xuất nhập hàng**, dashboard & báo cáo doanh thu/lợi nhuận theo ngày/tuần/tháng/năm, xuất **PDF/Excel**.

---

## 1. Công nghệ sử dụng

| Tầng | Công nghệ |
|------|-----------|
| **Backend** | Java 17, Spring Boot 3 (Web, Data JPA, Security), JWT, Maven |
| **CSDL** | MySQL 8 (16 bảng + 7 view, chuẩn hóa 3NF) |
| **Frontend** | React 18 (Vite), Bun, React-Bootstrap, Recharts, Axios |
| **Tích hợp** | VietQR (sinh mã QR), WEB2M (đối soát chuyển khoản), Telegram Bot (thông báo), OpenPDF (in hóa đơn), Apache POI (xuất Excel) |
| **Bảo mật** | JWT stateless, BCrypt, phân quyền theo vai trò (`@PreAuthorize`) |

**Kiến trúc:** REST API nhiều lớp `Controller → Service → Repository → Entity/View`, tách DTO; frontend SPA gọi API qua Vite proxy.

```
React (Vite) SPA  ──HTTP/JSON──>  Spring Boot REST API  ──JDBC──>  MySQL 8
   frontend/                          backend/
```

Tồn kho **không lưu cột `current_stock`** mà suy ra từ các lô qua **VIEW** (một nguồn sự thật — hủy hóa đơn tự hoàn tồn).

---

## 2. Yêu cầu môi trường

- **JDK 17+** · **Maven 3.9+**
- **MySQL 8** đang chạy ở `localhost:3306` (mặc định `root` / `root`)
- **Bun 1.2+** (hoặc Node 18+ với `npm`) cho frontend

---

## 3. Cài đặt & chạy

### 3.1. Cơ sở dữ liệu — **tự khởi tạo, không cần chạy SQL tay**
Chỉ cần tạo database rỗng:
```sql
CREATE DATABASE pos_convenience_store CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```
Khi backend khởi động, nó **tự tạo toàn bộ bảng + view + dữ liệu mẫu** (idempotent):
- Cấu trúc + VIEW: `backend/src/main/resources/db/schema.sql` (chạy qua `spring.sql.init`, an toàn chạy lại nhiều lần).
- Dữ liệu nền (tài khoản, ~69 sản phẩm + tồn kho, khách, khuyến mãi, cấu hình): các `*DataInitializer` trong code.

> 👉 **Drop database rồi khởi động lại = tự dựng lại sạch sẽ.** Không phải import SQL thủ công.
> (Vẫn có `sql/schema.sql` để import tay nếu muốn.)

Đổi thông tin kết nối tại `backend/src/main/resources/application.yml` hoặc biến môi trường `DB_USERNAME`, `DB_PASSWORD`.

### 3.2. Backend (cổng 8080)
```bash
cd backend
mvn spring-boot:run
```

### 3.3. Frontend (cổng 5173)
```bash
cd frontend
bun install      # hoặc: npm install
bun run dev      # hoặc: npm run dev
```
Mở **http://localhost:5173** (Vite proxy `/api` → backend `8080`).

---

## 4. Tài khoản demo

| Vai trò | Tài khoản | Mật khẩu | Quyền chính |
|---------|-----------|----------|-------------|
| **Chủ cửa hàng** (ADMIN) | `admin` | `123456` | Toàn quyền + tài khoản & cấu hình hệ thống |
| **Quản lý** (MANAGER) | `manager` | `123456` | Quản lý hàng/kho/KM/báo cáo/ca, không quản trị hệ thống |
| **Thu ngân** (CASHIER) | `cashier` | `123456` | Bán hàng (POS), xem hóa đơn của mình, khách hàng |

---

## 5. Phân quyền theo vai trò

Phân quyền **2 lớp**: ẩn menu/nút ở frontend (`navConfig`, `PrivateRoute`, `hasRole`) **và** chặn API ở backend (`@PreAuthorize`).

| Chức năng | ADMIN | MANAGER | CASHIER |
|-----------|:-----:|:-------:|:-------:|
| Bán hàng (POS), mở/đóng ca của mình | ✅ | ✅ | ✅ |
| Hóa đơn — xem | ✅ | ✅ | ✅ (ca của mình) |
| Hóa đơn — **hủy** | ✅ | ✅ | ❌ |
| Khách hàng — xem / thêm | ✅ | ✅ | ✅ |
| Khách hàng — **sửa / xóa** | ✅ | ✅ | ❌ |
| Sản phẩm, Danh mục & Đơn vị, Nhà cung cấp, Nhập kho, Tồn kho | ✅ | ✅ | ❌ |
| Khuyến mãi, Dashboard, Báo cáo | ✅ | ✅ | ❌ |
| **Quản lý ca** (xem mọi ca, đóng hộ) | ✅ | ✅ | ❌ |
| **Tài khoản người dùng** | ✅ | ❌ | ❌ |
| **Cấu hình hệ thống** (ngân hàng/WEB2M/Telegram) | ✅ | ❌ | ❌ |

Chi tiết: [`docs/08_Phan_quyen_va_chuc_nang.md`](docs/08_Phan_quyen_va_chuc_nang.md).

---

## 6. Tính năng nổi bật

- **Bán hàng POS**: quét mã vạch, giỏ hàng, gắn khách thân thiết, áp mã giảm giá, **đổi điểm tích lũy** (1 điểm = 1.000đ), thanh toán **tiền mặt** (tính tiền thừa) hoặc **QR**.
- **Tồn kho theo lô + HSD, xuất FIFO**: bán hàng trừ tồn theo lô cận hạn trước; cảnh báo tồn thấp / hết hàng / cận date.
- **Đề xuất nhập hàng**: dựa trên tốc độ bán 30 ngày → dự báo số ngày hết hàng + số lượng nên nhập (độ khẩn OUT/URGENT/REORDER).
- **Quản lý ca & đối soát quỹ**: tiền đầu ca tự điền theo ca trước, đóng ca **"Khớp quỹ"** không cần đếm lại, tách **tiền mặt** (trong két) vs **QR** (vào ngân hàng), cảnh báo ca "mở qua ngày".
- **Khách hàng thân thiết**: tích điểm theo chi tiêu, đổi điểm giảm trừ, lịch sử mua & tổng chi tiêu.
- **Dashboard & Báo cáo**: KPI ngày, biểu đồ doanh thu 7 ngày, cơ cấu thanh toán, giờ cao điểm, top sản phẩm; **báo cáo doanh thu + lợi nhuận gộp theo ngày/tuần/tháng/năm**, xuất Excel.
- **Hóa đơn**: tra cứu/lọc, in **PDF khổ 80mm chi tiết** (đơn giá, KM, đổi/tích điểm, nội dung CK), hủy hóa đơn (tự hoàn tồn + trừ điểm).
- **Tích hợp nâng cao**: VietQR + **WEB2M** đối soát chuyển khoản tự động, **Telegram** thông báo.

---

## 7. Cấu trúc thư mục

```
.
├── backend/                  # Spring Boot REST API
│   └── src/main/
│       ├── java/com/pos/
│       │   ├── controller/   # REST endpoints (@PreAuthorize phân quyền)
│       │   ├── service/      # Nghiệp vụ (Sale, Shift, Report, Inventory…)
│       │   ├── repository/   # Spring Data JPA + projection + view
│       │   ├── entity/       # Bảng & VIEW (read-only)
│       │   ├── dto/          # Request/Response
│       │   ├── config/       # Security, seeders dữ liệu
│       │   ├── job/          # Job nền (WEB2M, hết hạn QR)
│       │   └── util/         # VietQR, sinh mã, PDF…
│       └── resources/
│           ├── application.yml
│           └── db/schema.sql # Tự khởi tạo cấu trúc + view
├── frontend/                 # React (Vite + Bun)
│   └── src/{pages,components,api,context,routes}/
├── sql/schema.sql            # Bản import tay (tùy chọn)
└── docs/                     # Tài liệu phân tích, thiết kế, báo cáo
```

---

## 8. Tài liệu

| File | Nội dung |
|------|----------|
| [`docs/02_SRS_Dac_ta_yeu_cau.md`](docs/02_SRS_Dac_ta_yeu_cau.md) | Đặc tả yêu cầu (FR/NFR) |
| [`docs/03_Use_Case_Diagram.md`](docs/03_Use_Case_Diagram.md) | Use Case |
| [`docs/04_Thiet_ke_CSDL_ERD.md`](docs/04_Thiet_ke_CSDL_ERD.md) | Thiết kế CSDL / ERD |
| [`docs/05_Thiet_ke_UML.md`](docs/05_Thiet_ke_UML.md) | Class / Sequence / Activity |
| [`docs/08_Phan_quyen_va_chuc_nang.md`](docs/08_Phan_quyen_va_chuc_nang.md) | Ma trận phân quyền theo vai trò |
| [`docs/09_Doi_chieu_tieu_chi.md`](docs/09_Doi_chieu_tieu_chi.md) | Đối chiếu với bảng tiêu chí đánh giá |

---

## 9. Ghi chú bảo mật
Các giá trị nhạy cảm (mật khẩu DB, JWT secret, token WEB2M/Telegram) nên đặt qua **biến môi trường**, không commit giá trị thật. Mật khẩu người dùng được băm **BCrypt**; xác thực bằng **JWT** stateless. CORS chỉ cho phép origin của frontend.
