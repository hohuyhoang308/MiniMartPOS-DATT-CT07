# BÁO CÁO TỔNG KẾT ĐỒ ÁN
## Xây dựng & triển khai Website POS cho Cửa hàng tiện lợi

> Đề tài số 10 — Hệ thống POS (Point of Sale). Tài liệu này tổng hợp toàn bộ kết quả thực hiện:
> phân tích, thiết kế, lập trình (Frontend React + Backend Spring Boot REST API + MySQL), kiểm thử
> và triển khai. Các tài liệu chi tiết tham chiếu: [`02_SRS`](02_SRS_Dac_ta_yeu_cau.md),
> [`03_Use_Case`](03_Use_Case_Diagram.md), [`04_ERD`](04_Thiet_ke_CSDL_ERD.md),
> [`05_UML`](05_Thiet_ke_UML.md). Ảnh chụp màn hình ở thư mục [`../frontend/shots/`](../frontend/shots).

---

## 1. Tổng quan đề tài

| Mục | Nội dung |
|-----|----------|
| Tên hệ thống | Website POS cho cửa hàng tiện lợi (MiniMart POS) |
| Mục tiêu | Thu ngân bán hàng nhanh tại quầy bằng quét mã vạch; quản lý kiểm soát kho, doanh thu, nhân viên, khách hàng theo thời gian thực |
| Hình thức | Đồ án cá nhân — thực hiện toàn bộ FE + BE + CSDL + tài liệu |
| Kiến trúc | Client–Server tách lớp: **React SPA ↔ REST API (Spring Boot) ↔ MySQL** |

### 1.1. Bối cảnh & bài toán

Cửa hàng tiện lợi có **số mặt hàng lớn**, **tần suất giao dịch cao**, nhiều hàng có **hạn sử dụng (HSD)**,
nhiều **ca/thu ngân**. Quản lý thủ công gây tính tiền chậm, khó kiểm soát tồn kho thực tế, dễ tồn hàng
cận/quá hạn, khó tổng hợp doanh thu và quản lý khách thân thiết. Hệ thống POS giải quyết các vấn đề này.

---

## 2. Phân tích yêu cầu (Tiêu chí 1)

### 2.1. Tác nhân (Actor)

| Actor | Vai trò |
|-------|---------|
| **Chủ cửa hàng (Admin)** | Toàn quyền + quản lý tài khoản + cấu hình hệ thống |
| **Quản lý (Manager)** | Sản phẩm, kho, nhập hàng, khuyến mãi, báo cáo |
| **Thu ngân (Cashier)** | Bán hàng POS, ca làm việc, khách hàng |
| **Hệ thống (System)** | Cảnh báo tồn/HSD, đối soát WEB2M, gửi Telegram, tích điểm, sinh mã |

### 2.2. Chức năng chính (FR)

FR1 Xác thực & phân quyền · FR2 Danh mục/đơn vị/sản phẩm · FR3 Nhà cung cấp & nhập kho ·
FR4 **Bán hàng POS** (trọng tâm) · FR5 Hóa đơn & lịch sử · FR6 Khách thân thiết · FR7 Khuyến mãi ·
FR8 Kho & cảnh báo · FR9 Dashboard & báo cáo · FR10 Cấu hình cửa hàng ·
FR-A Nâng cao: VietQR, WEB2M, Telegram, PDF/Excel.

Chi tiết 24 use case xem [`02_SRS`](02_SRS_Dac_ta_yeu_cau.md) và sơ đồ [`03_Use_Case`](03_Use_Case_Diagram.md).

---

## 3. Thiết kế hệ thống (Tiêu chí 2)

### 3.1. Kiến trúc tổng thể

```
┌─────────────────────┐   HTTP/JSON (/api/**)   ┌──────────────────────────────┐   JDBC   ┌──────────┐
│  Frontend React SPA │ ──── Axios + JWT ─────▶  │  Backend Spring Boot REST    │ ───────▶ │ MySQL 8  │
│  (Vite, Bun)        │ ◀──────────────────────  │  Controller→Service→Repo     │ ◀─────── │ 16 bảng  │
└─────────────────────┘                          │  →Entity (+ Security/JWT)    │          │ +7 view  │
                                                 └──────────────────────────────┘          └──────────┘
```

Backend phân lớp rõ: **Controller (`@RestController`) → Service (`@Transactional`) → Repository
(Spring Data JPA) → Entity**. Sơ đồ Class/Sequence/Activity ở [`05_UML`](05_Thiet_ke_UML.md).

### 3.2. Cơ sở dữ liệu

- **16 bảng + 7 view**, chuẩn hóa **3NF**, toàn vẹn tham chiếu đầy đủ (16 khóa ngoại).
- Quyết định chống dư thừa nổi bật:
  - **Không lưu cột tồn kho** — tồn suy ra từ lô qua view `v_product_stock` (1 nguồn sự thật).
  - **Dòng phiếu nhập = lô hàng** (bất biến); tồn còn lại = `số nhập − tổng đã bán` qua bảng nối
    `invoice_item_batches` ⇒ **hủy hóa đơn thì tồn tự hoàn**, không cập nhật tay.
  - `total_amount`, `subtotal` là **cột GENERATED** do CSDL tự tính.
- Chi tiết ERD & đặc tả bảng: [`04_ERD`](04_Thiet_ke_CSDL_ERD.md); script: [`../sql/schema.sql`](../sql/schema.sql).

---

## 4. Chức năng & nghiệp vụ đã xây dựng (Tiêu chí 3)

### 4.1. Bán hàng tại quầy (POS) — trọng tâm

Màn hình POS thiết kế theo chuẩn quầy thu ngân: **lưới sản phẩm** (chọn nhanh / lọc theo danh mục /
tìm theo tên) ở trái, **giỏ hàng (ticket)** ở phải. Luồng:

1. **Mở ca** (nhập tiền đầu ca) trước khi bán; thanh trạng thái hiện **doanh thu ca theo thời gian thực**.
2. **Quét mã vạch** (gõ + Enter) hoặc **bấm vào sản phẩm** → thêm giỏ; kiểm tra tồn, không cho vượt tồn.
3. Tăng/giảm số lượng, xóa dòng; tự tính thành tiền & tổng.
4. (Tùy chọn) Gắn **khách thân thiết** (tích điểm) + nhập **mã giảm giá** (kiểm tra hợp lệ).
5. Thanh toán **Tiền mặt** (có **nút mệnh giá nhanh** & “Đủ tiền” → tự tính tiền thừa) hoặc **QR** (sinh VietQR).
6. Lưu hóa đơn trong **một transaction**: trừ tồn **FIFO theo HSD**, ghi phân bổ lô, tích điểm, tăng lượt
   dùng KM. Thiếu tồn → **rollback**.
7. **In/Xuất hóa đơn PDF** (khổ 80mm).
8. Có sẵn **máy tính (calculator)** hỗ trợ thu ngân (bàn phím + chuột).

### 4.2. Các nghiệp vụ khác

| Nhóm | Chức năng |
|------|-----------|
| Sản phẩm/Danh mục | CRUD, tìm/lọc, badge tồn kho (đủ/thấp/hết), tính lợi nhuận/sản phẩm |
| Nhập kho | Phiếu nhập nhiều dòng theo lô (giá nhập + HSD), tự tăng tồn, cập nhật giá vốn |
| Tồn kho & cảnh báo | Bảng tồn, tab Tồn thấp / Cận HSD, biểu đồ 8 mặt hàng tồn thấp nhất |
| Hóa đơn | Lọc theo ngày/trạng thái, chi tiết, in PDF, **hủy → hoàn tồn tự động** |
| Khách hàng | CRUD, tích điểm tự động, lịch sử mua & tổng chi tiêu |
| Khuyến mãi | CRUD (%/tiền, đơn tối thiểu, thời gian, giới hạn lượt), badge hiệu lực |
| Cấu hình | Thông tin cửa hàng, ngân hàng VietQR, WEB2M, Telegram (+ kiểm tra/gửi thử) |
| Tài khoản | CRUD nhân viên, phân quyền, đặt lại mật khẩu, khóa |

> **UX:** Mỗi màn hình có **dải hướng dẫn** giải thích ngắn gọn cách sử dụng cho người dùng mới.

---

## 5. Giao diện & UX (Tiêu chí 4)

Định hướng thiết kế **“Fresh Retail Intelligence”**: nền sáng sạch, sidebar tối (ink) sang trọng, nhấn
**xanh emerald** (tươi/tăng trưởng), bo tròn mềm, biểu đồ gradient, micro-interaction. Typography đặc trưng
(**Plus Jakarta Sans** + **Be Vietnam Pro**, hỗ trợ tiếng Việt đầy đủ). Responsive, phản hồi bằng **toast**.

| Màn hình | Ảnh |
|----------|-----|
| Đăng nhập | [`shots/01-login.png`](../frontend/shots/01-login.png) |
| Dashboard | [`shots/02-dashboard.png`](../frontend/shots/02-dashboard.png) |
| Sản phẩm | [`shots/03-products.png`](../frontend/shots/03-products.png) |
| Tồn kho | [`shots/04-inventory.png`](../frontend/shots/04-inventory.png) |
| POS bán hàng | [`shots/05-pos.png`](../frontend/shots/05-pos.png) |
| Hóa đơn | [`shots/06-invoices.png`](../frontend/shots/06-invoices.png) |
| Báo cáo | [`shots/07-reports.png`](../frontend/shots/07-reports.png) |
| Khuyến mãi | [`shots/08-promotions.png`](../frontend/shots/08-promotions.png) |
| Cấu hình | [`shots/09-settings.png`](../frontend/shots/09-settings.png) |

---

## 6. CSDL, Validation & Bảo mật (Tiêu chí 5)

| Hạng mục | Giải pháp |
|----------|-----------|
| Mật khẩu | Băm **BCrypt**, không lưu plaintext |
| Xác thực | **JWT** stateless (header `Authorization: Bearer`), token chứa vai trò |
| Phân quyền | `@PreAuthorize` theo vai trò ở backend **+** `PrivateRoute`/menu lọc ở frontend |
| CORS | Chỉ cho origin của frontend (`localhost:5173`) |
| SQL Injection | JPA tham số hóa, không nối chuỗi |
| XSS | React tự escape khi render; validate đầu vào (Bean Validation) |
| Toàn vẹn | `@Transactional` cho bán hàng/nhập kho; ràng buộc FK/CHECK/UNIQUE trong CSDL |
| Lỗi | Xử lý tập trung (`@RestControllerAdvice`) trả mã 400/401/403/404/409 + thông báo rõ |

---

## 7. Dashboard & Báo cáo (Tiêu chí 6)

**Dashboard** (chuẩn POS chuyên nghiệp) gồm:
- **KPI:** doanh thu hôm nay (+ % so với hôm qua), **lợi nhuận** hôm nay/tháng, số hóa đơn, số sản phẩm bán,
  trung bình/hóa đơn, số khách thân thiết.
- **Cảnh báo nhanh:** tồn thấp / hết hàng / cận HSD (bấm để tới trang Kho).
- **Biểu đồ:** doanh thu 7 ngày (area), **cơ cấu thanh toán** (donut Tiền mặt/QR), **doanh thu theo giờ**
  (tìm giờ cao điểm), **doanh thu theo danh mục** (bar).
- **Giao dịch gần đây** (feed) + **Top sản phẩm bán chạy**.

**Báo cáo:** doanh thu & **lợi nhuận** theo khoảng thời gian (biểu đồ area), theo **ca/thu ngân**, và
**xuất Excel** (.xlsx).

---

## 8. Triển khai & Demo (Tiêu chí 7)

```bash
# 1) CSDL: import sql/schema.sql vào MySQL (ServBay / phpMyAdmin)
# 2) Backend (cổng 8080)
cd backend && mvn spring-boot:run
# 3) Frontend (cổng 5173, proxy /api -> 8080)
cd frontend && bun install && bun run dev
```

Tài khoản demo (mật khẩu `123456`): `admin` / `manager` / `cashier`.
Mở http://localhost:5173. Chi tiết: [`../backend/README.md`](../backend/README.md), [`../frontend/README.md`](../frontend/README.md).

---

## 9. Đặc tả REST API (rút gọn)

Tiền tố `/api`, trả JSON `{ success, message, data, timestamp }`. POST tạo tài nguyên trả **201 Created**.

| Nhóm | Endpoint tiêu biểu |
|------|--------------------|
| Auth | `POST /auth/login`, `GET /auth/me` |
| Sản phẩm | `GET/POST/PUT/DELETE /products`, `GET /products/barcode/{code}` |
| Nhập kho | `GET/POST /goods-receipts` |
| Bán hàng | `POST /shifts/open`, `GET /shifts/current`, `POST /invoices`, `POST /invoices/{id}/cancel`, `GET /invoices/{id}/pdf` |
| Khách/KM | `GET/POST/PUT/DELETE /customers`, `GET /customers/{id}/history`, `POST /promotions/validate` |
| Kho/BC | `GET /inventory/*`, `GET /dashboard`, `GET /reports/revenue`, `GET /reports/export` |
| Tích hợp | `GET /payments/qr`, `GET /payments/{id}/status`, `POST /payments/web2m/sync`, `/integrations/*` |

---

## 10. Tính năng nâng cao (Tiêu chí 9)

| Tính năng | Mô tả |
|-----------|-------|
| **VietQR** | Sinh **URL ảnh QR** thanh toán (số tiền + nội dung CK) cho khách quét — chỉ hiển thị |
| **WEB2M** | Job nền `@Scheduled` poll lịch sử giao dịch ngân hàng, khớp theo số tiền + nội dung CK ⇒ tự xác nhận QR đã thanh toán |
| **Telegram Bot** | Gửi thông báo (nhận tiền, tồn thấp…) tới danh sách Chat ID; có gửi thử |
| **Xuất PDF** | Hóa đơn khổ 80mm (OpenPDF), hỗ trợ tiếng Việt |
| **Xuất Excel** | Báo cáo doanh thu/lợi nhuận (Apache POI) |
| **Máy tính** | Calculator hỗ trợ thu ngân ngay trên màn hình POS |

> Phân vai rõ: **VietQR = hiển thị** mã QR; **WEB2M = đối soát** (xác nhận tiền vào). Hai cơ chế độc lập.

---

## 11. Kiểm thử (QA)

| Loại | Phạm vi | Kết quả |
|------|---------|---------|
| Unit test (JUnit + Mockito) | `PromotionService` (giảm %/tiền, cap, min order, hết hạn/lượt), `CodeGenerator`, `VietQrUtil` | **13/13 PASS** |
| Kiểm thử tích hợp thủ công | Login, bán hàng FIFO, hủy HĐ hoàn tồn, dashboard, phân quyền | Đạt (đã kiểm chứng trên MySQL thật) |
| Kiểm thử giao diện | Chụp màn hình tự động bằng Playwright (9 màn hình) | Đạt |

Ví dụ đã kiểm chứng: bán 2 sản phẩm → tồn giảm đúng; **hủy hóa đơn → tồn hoàn lại tự động**; cashier bị
chặn (HTTP 403) khi truy cập Dashboard.

---

## 12. Chất lượng mã nguồn & quản lý dự án (Tiêu chí 10)

- Mã nguồn **phân lớp rõ**, đặt tên thống nhất, có comment phần quan trọng; tách FE/BE.
- **Git**: lịch sử commit theo từng chức năng (`chore`/`docs`/`feat(be)`/`test(be)`/`feat(fe)`/`refactor(be)`),
  làm trên nhánh `feature/*` rồi merge `--no-ff` vào `main`.

---

## 13. Kết luận & hướng phát triển

**Đạt được:** hệ thống POS đầy đủ nghiệp vụ bán hàng – kho (theo lô/HSD) – khách hàng – khuyến mãi –
dashboard/báo cáo (kèm lợi nhuận) – tích hợp nâng cao; kiến trúc React + Spring Boot REST tách lớp rõ ràng,
chuẩn **RESTful**; CSDL chuẩn hóa 3NF không dư thừa; có kiểm thử tự động; giao diện chuyên nghiệp, có hướng dẫn.

**Hướng phát triển:** đa chi nhánh/chuỗi, ứng dụng di động, tích hợp máy POS phần cứng, gợi ý nhập hàng bằng AI,
giữ đơn (hold order), mục tiêu doanh thu theo ngày.

---

## 14. Ánh xạ tiêu chí chấm điểm

| # | Tiêu chí | Điểm | Bằng chứng |
|---|----------|------|------------|
| 1 | Phân tích yêu cầu | 1.0 | Mục 2, `02_SRS`, `03_Use_Case` |
| 2 | Thiết kế hệ thống | 1.5 | Mục 3, `04_ERD`, `05_UML` |
| 3 | Chức năng POS | 2.0 | Mục 4 + mã nguồn |
| 4 | Giao diện & UX | 1.0 | Mục 5 + `frontend/shots` |
| 5 | CSDL, validation, bảo mật | 1.0 | Mục 6 |
| 6 | Dashboard, báo cáo | 0.75 | Mục 7 |
| 7 | Triển khai & demo | 1.0 | Mục 8, `sql/schema.sql` |
| 8 | Báo cáo, slide | 0.75 | Tài liệu này + `docs/` |
| 9 | Tính năng nâng cao | 0.5 | Mục 10 |
| 10 | Chất lượng mã & quản lý | 0.5 | Mục 11–12, Git |
