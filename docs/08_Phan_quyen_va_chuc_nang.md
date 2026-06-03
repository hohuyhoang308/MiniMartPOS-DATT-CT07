# 08 — Phân quyền & chức năng theo vai trò

Hệ thống có **3 vai trò**: `ADMIN` (Chủ cửa hàng), `MANAGER` (Quản lý), `CASHIER` (Thu ngân).
Mỗi vai trò thấy **menu khác nhau** và được phép **thao tác khác nhau** — không phải chức năng nào cũng dùng chung.

## 1. Cơ chế phân quyền (2 lớp)

| Lớp | Nơi thực thi | Cách làm |
|-----|--------------|----------|
| **Frontend** | `components/navConfig.js` | Mỗi mục menu khai báo `roles: [...]` → Sidebar chỉ render mục hợp vai trò |
| | `routes/PrivateRoute.jsx` | Chặn truy cập route theo `roles` (vào URL trực tiếp cũng bị đẩy về trang chủ) |
| | `context/AuthContext.jsx` → `hasRole(...)` | Ẩn/hiện **nút thao tác** trong trang dùng chung (vd nút Hủy HĐ, Sửa/Xóa khách) |
| **Backend** | `@PreAuthorize` trên Controller/method | Chốt chặn thật: kể cả gọi API trực tiếp vẫn bị từ chối (403) nếu sai vai trò |
| | `SecurityConfig` + JWT | Mọi request (trừ `/api/auth/login`) phải có JWT hợp lệ |

> Frontend chỉ để **trải nghiệm** (ẩn cái không dùng được); backend mới là **bảo mật thật**.

## 2. Ma trận chức năng × vai trò

| Nhóm | Chức năng | Endpoint chính | ADMIN | MANAGER | CASHIER |
|------|-----------|----------------|:-----:|:-------:|:-------:|
| Auth | Đăng nhập | `POST /api/auth/login` | ✅ | ✅ | ✅ |
| Bán hàng | POS, tạo hóa đơn | `POST /api/invoices` | ✅ | ✅ | ✅ |
| | Mở/đóng ca của mình | `POST /api/shifts/open`, `/{id}/close` | ✅ | ✅ | ✅ |
| Hóa đơn | Xem / lọc | `GET /api/invoices` | ✅ | ✅ | ✅¹ |
| | In PDF | `GET /api/invoices/{id}/pdf` | ✅ | ✅ | ✅ |
| | **Hủy hóa đơn** (bắt buộc lý do, ghi audit) | `POST /api/invoices/{id}/cancel` | ✅ | ✅ | ❌ |
| | **Trả hàng / hoàn tiền** | `POST /api/returns` | ✅ | ✅ | ❌ |
| Khách hàng | Xem / tìm / thêm | `GET/POST /api/customers` | ✅ | ✅ | ✅ |
| | **Sửa / xóa** | `PUT/DELETE /api/customers/{id}` | ✅ | ✅ | ❌ |
| Sản phẩm | Xem (POS cần) | `GET /api/products` | ✅ | ✅ | ✅ |
| | Thêm/sửa/xóa | `POST/PUT/DELETE /api/products` | ✅ | ✅ | ❌ |
| Danh mục & Đơn vị | CRUD | `/api/categories`, `/api/units` | ✅ | ✅ | ❌ |
| Nhà cung cấp | CRUD | `/api/suppliers` | ✅ | ✅ | ❌ |
| Nhập kho | Lập phiếu nhập (nhập theo thùng → quy đổi) | `/api/goods-receipts` | ✅ | ✅ | ❌ |
| Tồn kho | Tồn / cảnh báo / đề xuất nhập / ABC-XYZ | `/api/inventory/**` | ✅ | ✅ | ❌ |
| **Kệ hàng (thao tác)** | Xem kệ, **lên kệ**, **về kho** | `GET /api/shelves`, `/{id}/inventory`, `POST /transfer`, `/return` | ✅ | ✅ | ✅ |
| **Cấu hình kệ** | Thêm/sửa/xoá kệ, sức chứa | `POST/PUT/DELETE /api/shelves` | ✅ | ✅ | ❌ |
| Khuyến mãi | CRUD (validate dùng chung) | `/api/promotions` | ✅ | ✅ | ❌ |
| Dashboard | KPI tổng quan (doanh thu/lợi nhuận RÒNG) | `GET /api/dashboard` | ✅ | ✅ | ❌ |
| Báo cáo | Doanh thu/lợi nhuận (trừ hàng trả), Excel | `/api/reports/**` | ✅ | ✅ | ❌ |
| **Quản lý ca** | Xem mọi ca, **đóng hộ** | `GET /api/shifts`, `/{id}/close` | ✅ | ✅ | ❌ |
| **Tài khoản** | CRUD người dùng | `/api/users/**` | ✅ | ❌ | ❌ |
| **Cấu hình** | Cửa hàng/ngân hàng/WEB2M | `/api/store-config`, `/api/integrations/**` | ✅ | ❌ | ❌ |
| **Nhật ký kiểm toán** | Xem ai làm gì (hủy/đổi giá/trả hàng) | `GET /api/audit` | ✅ | ❌ | ❌ |

¹ Thu ngân chỉ thấy hóa đơn thuộc **ca của chính mình** (lọc trong `InvoiceService.search`).

> **Phân vai KỆ (quan trọng):** thao tác kệ **hằng ngày** (xem kệ chứa gì, **lên kệ**, **về kho**) là việc của
> **thu ngân** — người đứng quầy/sàn — nên backend mở cho cả 3 vai trò, và frontend gom vào trang **"Kệ hàng (lên/về)"**.
> Việc **cấu hình kệ vật lý** (thêm/sửa/xoá kệ, đặt sức chứa) là **setup** ít làm → chỉ **quản lý/chủ** ở trang
> **"Cấu hình kệ"**. Không cần thêm vai trò "nhân viên kho" riêng cho quy mô cửa hàng tiện lợi.

## 3. Khác biệt rõ giữa các vai trò

- **CASHIER (Thu ngân)** — **bán hàng + thao tác kệ hằng ngày**: POS, hóa đơn (của mình), khách hàng (xem/thêm để gắn tích điểm), **lên kệ / về kho / xem kệ**. Không thấy menu Cấu hình kệ/Kho/Báo cáo/Quản lý ca/Hệ thống; không hủy/trả được hóa đơn, không sửa/xóa khách, không cấu hình kệ.
- **MANAGER (Quản lý)** — toàn bộ **vận hành cửa hàng**: hàng hóa & giá, kho, nhập hàng, **cấu hình kệ**, khuyến mãi, **hủy & trả hàng**, báo cáo, **quản lý & đóng hộ ca**. Không quản trị tài khoản & cấu hình hệ thống & nhật ký kiểm toán.
- **ADMIN (Chủ cửa hàng)** — như Manager **cộng thêm** quản lý **tài khoản người dùng**, **cấu hình hệ thống** (cửa hàng, VietQR, WEB2M, Telegram) và **nhật ký kiểm toán**.

## 4. Bảo mật liên quan
- Mật khẩu băm **BCrypt** (không lưu plain text).
- Xác thực **JWT** stateless; token đính ở header `Authorization: Bearer`; **fail-fast** nếu `JWT_SECRET` mặc định ở profile `prod`.
- **Chống dò mật khẩu**: khóa tạm 60s sau 5 lần đăng nhập sai (HTTP 429).
- **Nhật ký kiểm toán** (`audit_logs`): ghi vết ai/khi nào/lý do cho hủy hóa đơn, đổi giá, trả hàng — chống gian lận void.
- Lỗi 500 **không lộ** chi tiết nội bộ ra client (chỉ log phía server). IDOR `/shifts/{id}` chỉ quản lý.
- Kiểm tra dữ liệu đầu vào bằng Bean Validation (`@NotNull`, `@DecimalMin`…), xử lý lỗi tập trung trả `ApiResponse` thống nhất.
