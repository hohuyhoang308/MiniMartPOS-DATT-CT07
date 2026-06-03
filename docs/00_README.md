# ĐỒ ÁN CÔNG NGHỆ PHẦN MỀM
## Phân tích, thiết kế, xây dựng và triển khai Website POS cho Cửa hàng tiện lợi

> Đề tài số **10 — POS cho cửa hàng tiện lợi (Convenience Store)** trong danh sách 40 đề tài.

---

### 1. Giới thiệu nhanh

Hệ thống **POS (Point of Sale) cho cửa hàng tiện lợi** là một website giúp cửa hàng:
bán hàng tại quầy (quét mã vạch), quản lý sản phẩm – danh mục – kho hàng, nhập hàng từ nhà
cung cấp, cảnh báo tồn kho thấp & hạn sử dụng, quản lý khách hàng thân thiết & tích điểm,
áp dụng khuyến mãi/mã giảm giá, in/xuất hóa đơn, quản lý ca làm việc của thu ngân, phân quyền
người dùng và xem dashboard/báo cáo doanh thu.

### 2. Công nghệ sử dụng

Hệ thống tách thành **2 phần** rõ ràng: **Frontend (React)** gọi **Backend (REST API – Spring Boot)** qua HTTP/JSON.

**Backend (giữ nguyên Spring Boot):**

| Thành phần | Công nghệ |
|------------|-----------|
| Ngôn ngữ | Java 17 |
| Framework | Spring Boot 3 (Spring Web/REST, Spring Data JPA, Spring Security + **JWT**) |
| Kiểu API | **RESTful API** trả về **JSON** (`@RestController`) |
| Cơ sở dữ liệu | MySQL 8 |
| Build | Maven |
| Kiến trúc | Nhiều lớp: **Controller (REST) → Service → Repository → Entity** |
| Tích hợp | **VietQR** (hiển thị QR), **WEB2M API** (poll đối soát ngân hàng), **Telegram Bot** (thông báo) |

**Frontend (React):**

| Thành phần | Công nghệ |
|------------|-----------|
| Thư viện UI | **React 18** (SPA – Single Page Application) |
| Công cụ build | **Vite** |
| Ngôn ngữ | JavaScript / JSX (có thể dùng TypeScript) |
| Điều hướng | **React Router** |
| Gọi API | **Axios** (kèm interceptor đính JWT) |
| Giao diện | Bootstrap 5 / React-Bootstrap (hoặc Ant Design / MUI) |
| Quản lý trạng thái | React Context / Redux Toolkit (cho giỏ hàng POS, phiên đăng nhập) |
| Quản lý mã nguồn | Git / GitHub |

> **Lý do chọn React (SPA) thay vì render phía server:** màn hình **POS bán hàng** có nhiều thao tác
> tương tác thời gian thực (quét mã, cập nhật giỏ, tính tiền tức thì) → React xử lý mượt phía client,
> không reload trang. Tách Frontend/Backend giúp **kiến trúc rõ ràng**, backend trở thành **REST API**
> có thể tái sử dụng (web, sau này thêm app di động), đúng định hướng kiến trúc hiện đại.

### 3. Cấu trúc bộ tài liệu (docs/)

| File | Nội dung | Bám tiêu chí |
|------|----------|--------------|
| `00_README.md` | Trang tổng quan này | — |
| `01_Ke_hoach_phan_cong.md` | Kế hoạch thực hiện, timeline, phân công, cấu trúc thư mục dự án | Mục 8, 10 |
| `02_SRS_Dac_ta_yeu_cau.md` | Đặc tả yêu cầu phần mềm: bối cảnh, actor, yêu cầu chức năng & phi chức năng, danh sách + mô tả use case | **Mục 1** |
| `03_Use_Case_Diagram.md` | Sơ đồ Use Case (Mermaid) cho từng nhóm chức năng | Mục 1 |
| `04_Thiet_ke_CSDL_ERD.md` | Sơ đồ ERD, đặc tả **chi tiết từng bảng** (không dư thừa), ràng buộc, script SQL | **Mục 2, 5** |
| `05_Thiet_ke_UML.md` | Class Diagram, Sequence Diagram, Activity Diagram | **Mục 2** |
| `06_Bao_cao_do_an.md` | Báo cáo đồ án đầy đủ theo cấu trúc tiêu chí mục 8 | **Mục 8** (tổng hợp tất cả) |
| `08_Phan_quyen_va_chuc_nang.md` | Ma trận chức năng × vai trò (ADMIN/MANAGER/CASHIER), phân vai kệ, bảo mật | Mục 5 |
| `09_Doi_chieu_tieu_chi.md` | Đối chiếu hệ thống với bảng tiêu chí chấm | Mục 8 |
| `10_Nang_cap_va_bo_sung.md` | **Giải thích chi tiết toàn bộ tính năng mới & phần đã sửa** (trả hàng, VAT, quy đổi, audit, sổ cái điểm, toàn vẹn dữ liệu, ABC/XYZ…) | **Mục 3, 5, 9** |
| `sql/schema.sql` | Script tạo CSDL MySQL + dữ liệu mẫu | Mục 5, 7 |

### 4. Ánh xạ tài liệu ↔ Bảng tiêu chí chấm điểm (10đ)

| # | Tiêu chí | Điểm | Tài liệu phụ trách |
|---|----------|------|--------------------|
| 1 | Phân tích bài toán & yêu cầu | 1.0 | `02_SRS`, `03_Use_Case` |
| 2 | Thiết kế hệ thống (MVC, ERD, UML) | 1.5 | `04_Thiet_ke_CSDL`, `05_UML`, `06_Bao_cao` |
| 3 | Chức năng & nghiệp vụ POS | 2.0 | `02_SRS` (nghiệp vụ), code |
| 4 | Giao diện & UX | 1.0 | code (định hướng ở `06_Bao_cao`) |
| 5 | CSDL, validation, bảo mật | 1.0 | `04_Thiet_ke_CSDL`, `06_Bao_cao` |
| 6 | Dashboard, thống kê, báo cáo | 0.75 | `02_SRS`, code |
| 7 | Triển khai & demo | 1.0 | `01_Ke_hoach`, `sql/schema.sql`, `06_Bao_cao` |
| 8 | Báo cáo, slide, trình bày | 0.75 | `06_Bao_cao` |
| 9 | Tính năng nâng cao (API/AI/QR) | 0.5 | `02_SRS` (mục nâng cao), `06_Bao_cao` |
| 10 | Chất lượng mã nguồn & quản lý dự án | 0.5 | `01_Ke_hoach`, Git |

### 5. Cách dùng bộ docs

- Bộ docs viết bằng **Markdown** → dễ đọc, đưa lên Git, và **chuyển sang Word/PDF** bằng `pandoc`
  hoặc copy vào Word. Sơ đồ dùng **Mermaid** (xem trực tiếp trên VS Code/GitHub) — có thể export ảnh PNG để dán vào báo cáo.
- Khi bắt tay vào code: bám đúng **danh sách bảng trong `04_Thiet_ke_CSDL_ERD.md`** (đã chuẩn hóa,
  không có bảng/cột thừa) và **danh sách use case trong `02_SRS`**.
