# 09 — Đối chiếu sản phẩm với Bảng tiêu chí đánh giá

Tổng điểm **10**. Bảng dưới ánh xạ từng tiêu chí sang **phần đã thực hiện** trong đồ án và **nơi kiểm chứng**.

| STT | Tiêu chí (điểm) | Đã thực hiện | Bằng chứng / vị trí |
|-----|-----------------|--------------|---------------------|
| **1** | Phân tích bài toán & yêu cầu (1.0) | Bài toán POS cửa hàng tiện lợi; xác định actor (Chủ/Quản lý/Thu ngân), FR/NFR, Use Case các luồng: đăng nhập, quản lý SP, tạo đơn, thanh toán, hóa đơn, báo cáo. | `docs/02_SRS_Dac_ta_yeu_cau.md`, `docs/03_Use_Case_Diagram.md` |
| **2** | Thiết kế hệ thống (1.5) | Kiến trúc nhiều lớp (Controller–Service–Repository–Entity). ERD 16 bảng + 7 view, chuẩn 3NF, tồn kho suy ra theo lô. Có Class/Sequence/Activity. | `docs/04_Thiet_ke_CSDL_ERD.md`, `docs/05_Thiet_ke_UML.md`, `backend/src/main/resources/db/schema.sql` |
| **3** | Chức năng & nghiệp vụ POS (2.0) | Đăng nhập + phân quyền, CRUD sản phẩm/danh mục/đơn vị/NCC, tạo đơn — tính tổng — thanh toán (tiền mặt/QR) — lưu hóa đơn — lịch sử. Nghiệp vụ đặc thù: **tồn kho theo lô + FIFO/HSD**, **nhập kho**, **khuyến mãi**, **tích & đổi điểm**, **đối soát quỹ ca**, **đề xuất nhập hàng**, hủy HĐ hoàn tồn. | `backend/.../service/` (SaleService, ShiftService, InventoryService, ReportService…), các trang `frontend/src/pages/` |
| **4** | Giao diện & UX (1.0) | Design system riêng (sidebar tối + nhấn emerald, Recharts), bố cục nhất quán, thao tác bán hàng nhanh, responsive ở mức phù hợp. Gộp **Danh mục & Đơn vị** một màn cho gọn; ẩn nút theo vai trò. | `frontend/src/index.css`, `frontend/src/pages/**`, `components/ui/` |
| **5** | CSDL, kiểm tra dữ liệu & bảo mật (1.0) | Khóa chính/ngoại, ràng buộc CHECK, view suy dữ liệu (tránh trùng). Validation đầu vào, JWT + BCrypt, **phân quyền 2 lớp** (`@PreAuthorize` + nav/route/hasRole). | `db/schema.sql`, `SecurityConfig`, các Controller `@PreAuthorize`, `docs/08` |
| **6** | Dashboard, thống kê & báo cáo (0.75) | Dashboard KPI (doanh thu, HĐ, top SP, cảnh báo kho), biểu đồ 7 ngày/giờ cao điểm/cơ cấu thanh toán. Báo cáo **doanh thu + lợi nhuận theo ngày/tuần/tháng/năm**, xuất Excel. Số liệu lấy từ DB thật. | `DashboardService`, `ReportService`, trang `Dashboard.jsx`, `Reports.jsx` |
| **7** | Triển khai & demo (1.0) | Chạy ổn định backend 8080 + frontend 5173. **Tự seed CSDL** (schema + dữ liệu mẫu) khi khởi động → demo được ngay. Tài khoản demo, ~69 sản phẩm + tồn kho. | `README.md` mục 3–4, `*DataInitializer`, `spring.sql.init` |
| **8** | Báo cáo, slide, trình bày (0.75) | Bộ tài liệu đầy đủ: giới thiệu, mục tiêu, phạm vi, phân tích, thiết kế, CSDL, công nghệ, chức năng, hướng dẫn chạy, kết quả, hạn chế & hướng phát triển. | `docs/06_Bao_cao_do_an.md`, `docs/07_Bao_cao_tong_ket.md`, `README.md` |
| **9** | Tính năng nâng cao / API/AI (0.5) | **Thanh toán QR (VietQR)** + **đối soát tự động qua WEB2M** (job nền), **xuất hóa đơn PDF** khổ 80mm, **thông báo Telegram**, **đề xuất nhập hàng theo tốc độ bán**. Đều gắn nghiệp vụ thật. | `util/VietQrUtil`, `job/Web2mSyncJob`, `InvoicePdfService`, `InventoryService.reorderSuggestions` |
| **10** | Chất lượng mã & quản lý dự án (0.5) | Chia module rõ (controller/service/repository/dto), đặt tên dễ hiểu, comment chỗ cần, **component dùng chung** (Recon/DiffBadge…), unit test. Dùng **Git** với lịch sử commit theo từng tính năng. | Cấu trúc `backend/`, `frontend/`, `backend/src/test/`, lịch sử Git |

## Ghi chú giải thích tính năng nâng cao (tiêu chí 9)
- **VietQR + WEB2M:** khi thanh toán QR, hệ thống sinh mã VietQR với **nội dung chuyển khoản duy nhất**; job nền định kỳ gọi API WEB2M lấy sao kê, **khớp số tiền + nội dung** để tự xác nhận đã nhận tiền (`PENDING → PAID`) và bắn Telegram. Giao dịch QR quá hạn được job dọn `PENDING → EXPIRED`.
- **PDF hóa đơn:** in khổ 80mm gồm thông tin cửa hàng, từng dòng hàng (SL × đơn giá), khuyến mãi, đổi/tích điểm, nội dung CK.
- **Đề xuất nhập hàng:** tính tốc độ bán 30 ngày → dự báo số ngày hết hàng + số lượng nên nhập, xếp theo độ khẩn — hỗ trợ ra quyết định nhập hàng thực tế.

## Hạn chế & hướng phát triển
- Chưa phân trang đầy đủ (hiện giới hạn 500 bản ghi mới nhất cho hóa đơn/khách).
- WEB2M cần token thật để đối soát; mặc định tắt trong môi trường demo.
- Hướng phát triển: app mobile cho thu ngân, in tem mã vạch, nhiều chi nhánh, AI dự báo nhu cầu nhập hàng.
