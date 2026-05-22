# 01. KẾ HOẠCH THỰC HIỆN & PHÂN CÔNG

## 1.1. Thông tin đồ án

| Thông tin | Nội dung |
|-----------|----------|
| Tên đồ án | Phân tích, thiết kế, xây dựng và triển khai Website POS cho cửa hàng tiện lợi |
| Đề tài | Số 10 — POS cho cửa hàng tiện lợi |
| Giảng viên hướng dẫn | *(điền)* |
| Lớp / Học kỳ | *(điền)* |
| Hình thức | **Đồ án cá nhân — 1 người thực hiện** |
| Sinh viên thực hiện | *(điền họ tên – MSSV)* |

## 1.2. Phân chia công việc (đồ án 1 người)

> Đồ án do **một mình sinh viên thực hiện toàn bộ** từ phân tích, thiết kế, lập trình
> Frontend (React) + Backend (Spring Boot REST API) đến kiểm thử, viết báo cáo & triển khai.
> Lịch sử **commit Git** ghi nhận tiến độ theo từng chức năng (tiêu chí mục 10).

| Mảng công việc | Nội dung phụ trách |
|----------------|--------------------|
| Phân tích & thiết kế | Khảo sát nghiệp vụ, đặc tả SRS, use case, thiết kế CSDL (ERD) & UML |
| Backend (Spring Boot REST) | Entity/Repository/Service, REST API, nghiệp vụ bán hàng – kho – hóa đơn, bảo mật JWT/phân quyền |
| Frontend (React) | SPA React, màn hình POS bán hàng, các trang quản trị, dashboard, responsive |
| QA & tài liệu | Kiểm thử, dữ liệu mẫu, viết báo cáo & slide, tính năng nâng cao (QR, PDF) |

> Vì làm một mình: ưu tiên làm **backend API + CSDL** chạy được trước, sau đó dựng **giao diện React**
> gọi API; giữ thói quen **commit đều theo từng chức năng** để thể hiện quá trình.

## 1.3. Timeline (8 tuần – tùy chỉnh theo lịch thực tế)

| Tuần | Mốc công việc | Sản phẩm bàn giao |
|------|---------------|-------------------|
| 1 | Khảo sát nghiệp vụ cửa hàng tiện lợi, chốt phạm vi | `02_SRS`, danh sách use case |
| 2 | Thiết kế hệ thống & CSDL | `04_Thiet_ke_CSDL` (ERD), `05_UML` |
| 3 | Khởi tạo **backend Spring Boot REST** + cấu hình DB; khởi tạo **frontend React (Vite)**; **đăng nhập JWT/phân quyền** | Khung BE+FE chạy được, login thông API |
| 4 | API + giao diện React quản lý sản phẩm – danh mục – đơn vị – nhà cung cấp (CRUD) | Module quản trị danh mục |
| 5 | Nghiệp vụ **nhập kho**, tồn kho, cảnh báo tồn thấp/HSD (API + trang React) | Module kho |
| 6 | **Màn hình POS bán hàng (React SPA)**, giỏ hàng, thanh toán, hóa đơn, ca làm việc | Luồng bán hàng hoàn chỉnh |
| 7 | Khách hàng thân thiết, khuyến mãi, **dashboard & báo cáo**, tính năng nâng cao (QR/PDF) | Báo cáo, thống kê |
| 8 | Kiểm thử, sửa lỗi, dữ liệu demo, hoàn thiện báo cáo & slide, triển khai | Sản phẩm + báo cáo cuối |

## 1.4. Cấu trúc thư mục dự án (đề xuất – tách Backend Spring Boot REST + Frontend React)

Dự án gồm **2 thư mục con**: `backend/` (Spring Boot REST API) và `frontend/` (React SPA).

```
pos-convenience-store/
├── docs/                          # Bộ tài liệu đồ án (file này nằm ở đây)
├── sql/
│   └── schema.sql                 # Script tạo CSDL + dữ liệu mẫu
│
├── backend/                       # ===== BACKEND: Spring Boot REST API =====
│   ├── src/main/java/com/pos/
│   │   ├── PosApplication.java
│   │   ├── config/                # Spring Security + JWT, CORS, dữ liệu khởi tạo
│   │   ├── controller/            # @RestController — nhận request, trả JSON
│   │   ├── service/               # Tầng Service (xử lý nghiệp vụ)
│   │   ├── repository/            # Tầng Repository (Spring Data JPA)
│   │   ├── entity/                # Các Entity ánh xạ bảng CSDL
│   │   ├── dto/                   # Request/Response DTO (dữ liệu trao đổi với React)
│   │   ├── security/              # JWT filter, token provider, UserDetails
│   │   ├── exception/             # Xử lý ngoại lệ tập trung (@RestControllerAdvice)
│   │   └── util/                  # Tiện ích (sinh mã hóa đơn, QR, PDF...)
│   ├── src/main/resources/
│   │   └── application.yml        # Cấu hình kết nối MySQL, JWT secret, port...
│   └── pom.xml
│
├── frontend/                      # ===== FRONTEND: React (Vite) SPA =====
│   ├── src/
│   │   ├── main.jsx               # Điểm vào React
│   │   ├── App.jsx                # Khai báo Router
│   │   ├── api/                   # Axios instance + các hàm gọi REST API
│   │   ├── components/            # Component dùng chung (layout, table, modal...)
│   │   ├── pages/                 # Trang theo chức năng
│   │   │   ├── auth/              # Login
│   │   │   ├── product/ category/ supplier/ inventory/
│   │   │   ├── pos/               # Màn hình bán hàng (POS)
│   │   │   ├── order/ customer/ promotion/
│   │   │   └── dashboard/ report/
│   │   ├── context/              # AuthContext, CartContext (giỏ hàng POS)
│   │   ├── routes/               # PrivateRoute, phân quyền theo vai trò
│   │   └── assets/               # CSS, ảnh, icon
│   ├── index.html
│   ├── vite.config.js            # Cấu hình proxy `/api` → backend
│   └── package.json
│
└── README.md
```

> **Cách chạy:** mở 2 tiến trình — backend `mvn spring-boot:run` (cổng 8080),
> frontend `npm run dev` (cổng 5173, proxy `/api` sang 8080). Khi đóng gói demo có thể
> `npm run build` rồi cho Spring Boot serve thư mục build tĩnh, hoặc deploy riêng FE/BE.

## 1.5. Quy ước quản lý mã nguồn (Git)

- Mỗi chức năng làm trên 1 nhánh: `feature/<ten-chuc-nang>` → merge vào `main` qua Pull Request (tự review).
- Commit message rõ ràng, tiếng Việt không dấu hoặc tiếng Anh; nên ghi rõ phần BE hay FE, ví dụ:
  `feat(be): them API ban hang POS`, `feat(fe): man hinh POS React`, `fix: sua loi tinh tien thua`, `docs: cap nhat ERD`.
- Commit đều theo từng chức năng để **lịch sử commit phản ánh quá trình thực hiện** (tiêu chí mục 10).

## 1.6. Quản lý rủi ro

| Rủi ro | Ảnh hưởng | Cách xử lý |
|--------|-----------|-----------|
| Thiết kế CSDL phải sửa khi đang code | Mất thời gian refactor | Chốt kỹ ERD ở tuần 2 (file `04`), chỉ mở rộng – không phá vỡ |
| Màn hình POS phức tạp (giỏ hàng, tính tiền) | Trễ tiến độ | Làm sớm ở tuần 6, dùng React state/Context gọn, có dữ liệu mẫu để test |
| Làm một mình cả FE React lẫn BE | Quá tải, dễ trễ | Ưu tiên backend API + CSDL chạy trước, FE bám sát API; cắt giảm tính năng phụ nếu cần |
| Lỗi CORS / xác thực JWT giữa FE–BE | Mất thời gian debug | Cấu hình CORS & proxy Vite sớm ở tuần 3, test login thông API trước khi làm tiếp |
| Tính năng nâng cao tốn thời gian | Bỏ dở | Ưu tiên QR thanh toán + xuất PDF (nhẹ, giá trị cao) trước |
