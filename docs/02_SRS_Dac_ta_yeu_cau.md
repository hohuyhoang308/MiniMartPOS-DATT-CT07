# 02. ĐẶC TẢ YÊU CẦU PHẦN MỀM (SRS)

## 2.1. Bối cảnh & phát biểu bài toán

Cửa hàng tiện lợi (convenience store) là mô hình bán lẻ với **số lượng mặt hàng lớn** (nước giải
khát, đồ ăn nhanh, hàng tiêu dùng, đồ đông lạnh...), **tần suất giao dịch cao**, nhiều mặt hàng có
**hạn sử dụng**, và thường có **nhiều ca làm việc/nhiều thu ngân** trong ngày.

Hiện trạng nếu quản lý thủ công (sổ sách, Excel) gặp các vấn đề:
- Tính tiền chậm, dễ sai khi khách đông; khó tra cứu giá theo mã vạch.
- Không nắm được **tồn kho thực tế**, dễ hết hàng hoặc tồn hàng cận/quá **hạn sử dụng**.
- Khó tổng hợp **doanh thu**, mặt hàng bán chạy, hiệu suất từng ca/thu ngân.
- Không quản lý được **khách hàng thân thiết**, khuyến mãi rời rạc.

**Mục tiêu hệ thống:** xây dựng website POS giúp thu ngân **bán hàng nhanh tại quầy bằng quét mã
vạch**, tự động tính tiền – tồn kho – hóa đơn; giúp quản lý **kiểm soát kho, doanh thu, nhân viên,
khách hàng** theo thời gian thực.

### Phạm vi (Scope)

| Trong phạm vi | Ngoài phạm vi (hướng phát triển) |
|---------------|----------------------------------|
| Bán hàng tại quầy (POS), quét mã vạch | Bán hàng online/giao hàng tận nơi |
| Quản lý sản phẩm, danh mục, đơn vị, nhà cung cấp | Đa chi nhánh/chuỗi (đề tài 38) |
| Nhập kho, tồn kho, cảnh báo tồn thấp & HSD | Quản lý kế toán – công nợ chi tiết |
| Hóa đơn, thanh toán (tiền mặt, QR), lịch sử | Tích hợp máy POS phần cứng chuyên dụng |
| Khách hàng thân thiết, tích điểm, khuyến mãi | App di động riêng |
| Ca làm việc, phân quyền, dashboard, báo cáo | |
| Tính năng nâng cao: thanh toán QR, xuất hóa đơn PDF | |

## 2.2. Người dùng (Actors) & vai trò

| Actor | Vai trò | Mô tả |
|-------|---------|-------|
| **Chủ cửa hàng (Admin/Owner)** | Toàn quyền | Quản lý tài khoản nhân viên, phân quyền, xem toàn bộ báo cáo, cấu hình cửa hàng, quản lý khuyến mãi. |
| **Quản lý (Manager)** | Quản trị vận hành | Quản lý sản phẩm/kho/nhà cung cấp, nhập hàng, xem báo cáo, quản lý khách hàng & khuyến mãi. Không quản lý tài khoản. |
| **Thu ngân (Cashier)** | Bán hàng | Đăng nhập, mở/đóng ca, bán hàng tại quầy, tạo đơn, thanh toán, in hóa đơn, tra cứu sản phẩm/khách hàng. |
| **Hệ thống (System)** | Tự động | Cảnh báo tồn thấp/cận HSD, cập nhật tồn kho, tích điểm, sinh mã hóa đơn, **đối soát thanh toán qua WEB2M**, **gửi thông báo Telegram**. |

> Khách mua hàng **không trực tiếp dùng hệ thống** (giao dịch qua thu ngân), nên không phải actor
> đăng nhập. Thông tin khách chỉ được lưu khi là **khách hàng thân thiết**.

### Ma trận phân quyền (tóm tắt)

| Chức năng | Admin | Manager | Cashier |
|-----------|:-----:|:-------:|:-------:|
| Quản lý tài khoản & phân quyền | ✔ | ✘ | ✘ |
| Quản lý sản phẩm/danh mục/đơn vị | ✔ | ✔ | xem |
| Quản lý nhà cung cấp & nhập kho | ✔ | ✔ | ✘ |
| Bán hàng (POS) & ca làm việc | ✔ | ✔ | ✔ |
| Quản lý khách hàng thân thiết | ✔ | ✔ | xem/thêm |
| Quản lý khuyến mãi | ✔ | ✔ | áp dụng |
| Dashboard & báo cáo | ✔ | ✔ | ca của mình |
| Cấu hình cửa hàng | ✔ | ✘ | ✘ |

## 2.3. Yêu cầu chức năng (Functional Requirements)

### FR1 – Xác thực & phân quyền
- FR1.1 Đăng nhập bằng tài khoản (username + mật khẩu đã **băm**), đăng xuất.
- FR1.2 Phân quyền theo vai trò (Admin/Manager/Cashier); chặn truy cập trái phép.
- FR1.3 Admin quản lý tài khoản nhân viên (thêm/sửa/khóa/đặt lại mật khẩu).

### FR2 – Quản lý danh mục & sản phẩm
- FR2.1 CRUD **danh mục** sản phẩm.
- FR2.2 CRUD **đơn vị tính** (lon, chai, gói, thùng...).
- FR2.3 CRUD **sản phẩm**: mã vạch (barcode), tên, danh mục, đơn vị, giá vốn, giá bán, ảnh, mức tồn tối thiểu.
- FR2.4 Tìm kiếm/lọc sản phẩm theo tên, mã vạch, danh mục; bật/tắt kinh doanh.

### FR3 – Nhà cung cấp & nhập kho
- FR3.1 CRUD **nhà cung cấp**.
- FR3.2 Tạo **phiếu nhập kho** (chọn nhà cung cấp, thêm nhiều sản phẩm, số lượng, giá nhập, **hạn sử dụng**).
- FR3.3 Khi lưu phiếu nhập → **tự động tăng tồn kho** sản phẩm.
- FR3.4 Xem lịch sử phiếu nhập.

### FR4 – Bán hàng tại quầy (POS) — *nghiệp vụ trọng tâm*
- FR4.1 **Mở ca làm việc** (nhập tiền đầu ca) trước khi bán; **đóng ca** (đối soát tiền cuối ca).
- FR4.2 Quét/nhập **mã vạch** hoặc tìm sản phẩm → thêm vào giỏ; tự lấy giá, kiểm tra tồn.
- FR4.3 Tăng/giảm số lượng, xóa dòng; **tự tính thành tiền & tổng tiền**.
- FR4.4 Gắn **khách hàng thân thiết** (tùy chọn) để tích điểm.
- FR4.5 Áp dụng **mã giảm giá/khuyến mãi** hợp lệ → tính lại tổng tiền.
- FR4.6 Thanh toán: **tiền mặt** (nhập tiền khách đưa → tính **tiền thừa**) hoặc **QR/chuyển khoản**.
- FR4.7 Lưu **hóa đơn** + chi tiết; **tự động trừ tồn kho**; **tích điểm** cho khách; cộng vào doanh thu ca.
- FR4.8 **In/Xuất hóa đơn** (PDF / khổ in 80mm).

### FR5 – Hóa đơn & lịch sử giao dịch
- FR5.1 Xem danh sách hóa đơn, lọc theo ngày/thu ngân/khách/trạng thái.
- FR5.2 Xem chi tiết hóa đơn; in lại; **hủy hóa đơn** (Admin/Manager) → hoàn tồn kho.

### FR6 – Khách hàng thân thiết
- FR6.1 CRUD khách hàng (tên, **SĐT duy nhất**, email).
- FR6.2 Tự **tích điểm** theo giá trị mua; xem lịch sử mua & tổng chi tiêu.

### FR7 – Khuyến mãi / Mã giảm giá
- FR7.1 CRUD khuyến mãi: mã, loại giảm (%, số tiền), giá trị, đơn tối thiểu, thời gian hiệu lực, giới hạn lượt dùng.
- FR7.2 Kiểm tra hợp lệ khi áp dụng (còn hạn, còn lượt, đủ điều kiện đơn).

### FR8 – Kho & cảnh báo
- FR8.1 Xem tồn kho hiện tại của tất cả sản phẩm.
- FR8.2 **Cảnh báo tồn thấp** (tồn ≤ mức tối thiểu) và **cảnh báo cận/quá hạn sử dụng**.

### FR9 – Dashboard & báo cáo
- FR9.1 Dashboard: doanh thu hôm nay/tháng, số hóa đơn, **biểu đồ doanh thu theo ngày**, top sản phẩm bán chạy, số mặt hàng tồn thấp.
- FR9.2 Báo cáo doanh thu theo khoảng thời gian; báo cáo theo thu ngân/ca; báo cáo tồn kho.
- FR9.3 **Xuất báo cáo Excel/PDF**.

### FR10 – Cấu hình cửa hàng
- FR10.1 Thông tin cửa hàng (tên, địa chỉ, SĐT, mã số thuế, logo) dùng để in hóa đơn.

### FR-Nâng cao (tiêu chí mục 9)
- FR-A1 **Hiển thị mã QR thanh toán (VietQR)**: sinh **mã QR banking** (ngân hàng + số tài khoản + số tiền
  + nội dung CK) cho khách quét. *VietQR chỉ để **hiển thị QR**, không tự xác nhận thanh toán.*
- FR-A2 **Xuất hóa đơn PDF** và/hoặc **gửi hóa đơn qua email** cho khách.
- FR-A3 *(tùy chọn)* **Chatbot/cảnh báo thông minh** tra cứu nhanh sản phẩm tồn thấp / gợi ý nhập hàng.
- FR-A4 **Đối soát thanh toán tự động (API WEB2M)**: hệ thống **poll lịch sử giao dịch ngân hàng** qua
  API WEB2M, khớp giao dịch theo **số tiền + nội dung CK** (chứa mã ký hiệu + mã hóa đơn) ⇒ **tự xác nhận
  hóa đơn đã thanh toán**, lưu mã giao dịch ngân hàng. *Đây mới là cơ chế xác nhận tiền vào.*
- FR-A5 **Thông báo qua Telegram Bot**: gửi thông báo tự động (nhận thanh toán, tồn thấp, hóa đơn mới...)
  tới danh sách **Chat ID** đã cấu hình; admin bật/tắt từng loại thông báo và **gửi thử**.
- FR-A6 **Cấu hình tích hợp**: admin nhập URL/token API WEB2M, thông tin ngân hàng (BIN, STK, chủ TK,
  mã ký hiệu), Bot Token & danh sách Chat ID Telegram; có nút **kiểm tra kết nối**.

## 2.4. Yêu cầu phi chức năng (Non-Functional Requirements)

| Mã | Loại | Yêu cầu |
|----|------|---------|
| NFR1 | Hiệu năng | Tra cứu sản phẩm theo mã vạch < 1s; thao tác thêm vào giỏ mượt khi bán đông. |
| NFR2 | Khả dụng (UX) | Màn hình POS tối giản, thao tác bằng bàn phím/máy quét nhanh; ít click. |
| NFR3 | Tương thích | Responsive, chạy tốt trên trình duyệt máy tính (Chrome/Edge), dùng được trên tablet. |
| NFR4 | Bảo mật | Mật khẩu **băm (BCrypt)**; xác thực bằng **JWT** (token đính ở header `Authorization`), phân quyền theo vai trò ở backend; cấu hình **CORS** đúng origin của frontend; chống SQL Injection (dùng JPA tham số hóa) & XSS (React tự escape khi render). |
| NFR5 | Toàn vẹn dữ liệu | Ràng buộc khóa chính/khóa ngoại, **giao dịch (transaction)** khi bán hàng/nhập kho để không sai tồn kho. |
| NFR6 | Tin cậy | Validate dữ liệu đầu vào ở cả client & server; thông báo lỗi rõ ràng. |
| NFR7 | Bảo trì | Mã nguồn phân lớp rõ (Controller/Service/Repository), đặt tên thống nhất, có comment phần quan trọng. |
| NFR8 | Triển khai | Cài đặt đơn giản: chạy `schema.sql` + cấu hình `application.yml` → **backend** `mvn spring-boot:run`; **frontend** `npm install && npm run dev` (hoặc `npm run build` để đóng gói tĩnh). |

## 2.5. Danh sách Use Case

| Mã | Use Case | Actor chính |
|----|----------|-------------|
| UC01 | Đăng nhập / Đăng xuất | Tất cả |
| UC02 | Quản lý tài khoản & phân quyền | Admin |
| UC03 | Quản lý danh mục | Manager |
| UC04 | Quản lý đơn vị tính | Manager |
| UC05 | Quản lý sản phẩm | Manager |
| UC06 | Quản lý nhà cung cấp | Manager |
| UC07 | Lập phiếu nhập kho | Manager |
| UC08 | Mở / Đóng ca làm việc | Cashier |
| UC09 | **Bán hàng tại quầy (tạo đơn)** | Cashier |
| UC10 | Thanh toán & lập hóa đơn | Cashier |
| UC11 | Áp dụng khuyến mãi | Cashier |
| UC12 | In / Xuất hóa đơn (PDF) | Cashier |
| UC13 | Quản lý hóa đơn & lịch sử | Manager/Cashier |
| UC14 | Hủy hóa đơn | Manager |
| UC15 | Quản lý khách hàng thân thiết | Manager/Cashier |
| UC16 | Quản lý khuyến mãi | Manager |
| UC17 | Xem tồn kho & cảnh báo tồn thấp/HSD | Manager |
| UC18 | Xem Dashboard | Manager |
| UC19 | Xem & xuất báo cáo | Manager |
| UC20 | Cấu hình cửa hàng | Admin |
| UC21 | Hiển thị QR thanh toán (VietQR) | Cashier |
| UC22 | Đối soát thanh toán tự động (WEB2M) | System |
| UC23 | Gửi thông báo Telegram | System |
| UC24 | Cấu hình tích hợp (bank/WEB2M/Telegram) | Admin |

## 2.6. Mô tả chi tiết các Use Case trọng tâm

### UC09 — Bán hàng tại quầy (tạo đơn)

| Mục | Nội dung |
|-----|----------|
| **Mã** | UC09 |
| **Actor** | Thu ngân (Cashier) |
| **Mô tả** | Thu ngân tạo đơn hàng cho khách bằng cách quét mã vạch các sản phẩm. |
| **Tiền điều kiện** | Đã đăng nhập; **đã mở ca làm việc**. |
| **Hậu điều kiện** | Giỏ hàng có sản phẩm, sẵn sàng thanh toán (xem UC10). |

**Luồng chính:**
1. Thu ngân mở màn hình **POS bán hàng**.
2. Quét/nhập **mã vạch** sản phẩm.
3. Hệ thống tìm sản phẩm, kiểm tra **còn tồn kho**, thêm vào giỏ với số lượng 1 (hoặc +1 nếu đã có).
4. Hệ thống tính **thành tiền dòng** và **tổng tiền tạm tính**.
5. Lặp lại bước 2–4 cho các sản phẩm khác.
6. (Tùy chọn) Thu ngân chỉnh số lượng / xóa dòng.
7. (Tùy chọn) Gắn khách hàng thân thiết (UC15) và áp mã giảm giá (UC11).
8. Chuyển sang thanh toán (UC10).

**Luồng phụ / ngoại lệ:**
- 3a. Không tìm thấy mã vạch → báo "Sản phẩm không tồn tại".
- 3b. Sản phẩm hết tồn (tồn = 0) hoặc số lượng vượt tồn → cảnh báo, không cho thêm.

---

### UC10 — Thanh toán & lập hóa đơn

| Mục | Nội dung |
|-----|----------|
| **Mã** | UC10 |
| **Actor** | Thu ngân |
| **Tiền điều kiện** | Giỏ hàng có ít nhất 1 sản phẩm (UC09). |
| **Hậu điều kiện** | Hóa đơn được lưu; **tồn kho giảm**; **điểm khách tăng**; doanh thu ca tăng. |

**Luồng chính:**
1. Thu ngân chọn **Thanh toán**.
2. Hệ thống hiển thị **tổng tiền** (đã trừ giảm giá nếu có).
3. Thu ngân chọn hình thức: **Tiền mặt** hoặc **QR/Chuyển khoản**.
4. Nếu tiền mặt: nhập **tiền khách đưa** → hệ thống tính **tiền thừa**.
   Nếu QR: hệ thống sinh **mã QR** (UC21), thu ngân xác nhận đã nhận tiền.
5. Hệ thống **lưu hóa đơn + chi tiết** trong một **transaction**:
   trừ tồn kho từng sản phẩm, cộng điểm cho khách (nếu có), cộng doanh thu ca, tăng lượt dùng mã giảm giá.
6. Hệ thống hiển thị hóa đơn và cho phép **in/xuất PDF** (UC12).

**Ngoại lệ:**
- 5a. Trong lúc lưu phát hiện một sản phẩm vừa hết tồn → **rollback** transaction, báo lỗi.
- 4a. Tiền khách đưa < tổng tiền → báo lỗi, không cho hoàn tất.

---

### UC07 — Lập phiếu nhập kho

| Mục | Nội dung |
|-----|----------|
| **Mã** | UC07 |
| **Actor** | Quản lý |
| **Tiền điều kiện** | Đã đăng nhập (Admin/Manager); có nhà cung cấp & sản phẩm. |
| **Hậu điều kiện** | Phiếu nhập được lưu; **tồn kho tăng** theo số lượng nhập. |

**Luồng chính:**
1. Quản lý tạo phiếu nhập, chọn **nhà cung cấp**.
2. Thêm nhiều dòng: chọn sản phẩm, nhập **số lượng**, **giá nhập**, **hạn sử dụng**.
3. Hệ thống tính tổng tiền phiếu nhập.
4. Lưu phiếu → trong **transaction**: tạo phiếu + chi tiết, **cộng tồn kho** từng sản phẩm, cập nhật giá vốn (nếu chọn).

---

### UC01 — Đăng nhập

| Mục | Nội dung |
|-----|----------|
| **Actor** | Tất cả người dùng |
| **Luồng chính** | Nhập username + mật khẩu → hệ thống kiểm tra (so khớp mật khẩu **đã băm**) → tạo phiên → chuyển đến trang theo vai trò. |
| **Ngoại lệ** | Sai thông tin → báo lỗi; tài khoản bị khóa → từ chối; sai quá số lần → tạm khóa (tùy chọn). |

### UC17 — Xem tồn kho & cảnh báo

| Mục | Nội dung |
|-----|----------|
| **Actor** | Quản lý |
| **Luồng chính** | Mở màn hình kho → hệ thống liệt kê tồn kho; **highlight** sản phẩm có `current_stock ≤ min_stock` (tồn thấp) và sản phẩm có lô **cận/quá HSD**. |

> Các use case CRUD còn lại (UC02–UC06, UC15, UC16, UC20) theo mẫu chuẩn:
> *Liệt kê → Thêm → Sửa → Xóa/Khóa*, có **validate dữ liệu** và **kiểm tra quyền** trước khi thực hiện.
