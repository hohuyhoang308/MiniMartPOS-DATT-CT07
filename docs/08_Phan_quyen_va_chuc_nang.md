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
| | **Hủy hóa đơn** | `POST /api/invoices/{id}/cancel` | ✅ | ✅ | ❌ |
| Khách hàng | Xem / tìm / thêm | `GET/POST /api/customers` | ✅ | ✅ | ✅ |
| | **Sửa / xóa** | `PUT/DELETE /api/customers/{id}` | ✅ | ✅ | ❌ |
| Sản phẩm | Xem (POS cần) | `GET /api/products` | ✅ | ✅ | ✅ |
| | Thêm/sửa/xóa | `POST/PUT/DELETE /api/products` | ✅ | ✅ | ❌ |
| Danh mục & Đơn vị | CRUD | `/api/categories`, `/api/units` | ✅ | ✅ | ❌ |
| Nhà cung cấp | CRUD | `/api/suppliers` | ✅ | ✅ | ❌ |
| Nhập kho | Lập phiếu nhập | `/api/goods-receipts` | ✅ | ✅ | ❌ |
| Tồn kho | Tồn / cảnh báo / đề xuất nhập | `/api/inventory/**` | ✅ | ✅ | ❌ |
| Khuyến mãi | CRUD (validate dùng chung) | `/api/promotions` | ✅ | ✅ | ❌ |
| Dashboard | KPI tổng quan | `GET /api/dashboard` | ✅ | ✅ | ❌ |
| Báo cáo | Doanh thu/lợi nhuận, Excel | `/api/reports/**` | ✅ | ✅ | ❌ |
| **Quản lý ca** | Xem mọi ca, **đóng hộ** | `GET /api/shifts`, `/{id}/close` | ✅ | ✅ | ❌ |
| **Tài khoản** | CRUD người dùng | `/api/users/**` | ✅ | ❌ | ❌ |
| **Cấu hình** | Cửa hàng/ngân hàng/WEB2M | `/api/store-config`, `/api/integrations/**` | ✅ | ❌ | ❌ |

¹ Thu ngân chỉ thấy hóa đơn thuộc **ca của chính mình** (lọc trong `InvoiceService.search`).

## 3. Khác biệt rõ giữa các vai trò

- **CASHIER (Thu ngân)** — chỉ tập trung **bán hàng**: POS, hóa đơn (của mình), khách hàng (xem/thêm để gắn tích điểm). Không thấy menu Kho/Báo cáo/Quản lý ca/Hệ thống; không hủy được hóa đơn, không sửa/xóa khách.
- **MANAGER (Quản lý)** — toàn bộ **vận hành cửa hàng**: hàng hóa, kho, nhập hàng, khuyến mãi, báo cáo, **quản lý & đóng hộ ca** của thu ngân. Không quản trị tài khoản & cấu hình hệ thống.
- **ADMIN (Chủ cửa hàng)** — như Manager **cộng thêm** quản lý **tài khoản người dùng** và **cấu hình hệ thống** (thông tin cửa hàng, ngân hàng VietQR, WEB2M, Telegram).

## 4. Bảo mật liên quan
- Mật khẩu băm **BCrypt** (không lưu plain text).
- Xác thực **JWT** stateless; token đính ở header `Authorization: Bearer`.
- Kiểm tra dữ liệu đầu vào bằng Bean Validation (`@NotNull`, `@DecimalMin`…), xử lý lỗi tập trung trả `ApiResponse` thống nhất.
