# ÔN TẬP BẢO VỆ ĐỒ ÁN — MiniMart POS (chuỗi cửa hàng tiện lợi)

> Tài liệu ôn cho buổi bảo vệ. Cấu trúc: thẻ nhớ số liệu → bài giới thiệu 2 phút → ôn theo đúng
> 10 tiêu chí chấm → deep-dive phân hệ Lương → ngân hàng câu hỏi hội đồng → kịch bản demo → mẹo xử lý sự cố.

---

## 0. THẺ NHỚ NHANH — số liệu phải thuộc lòng

| Mục | Con số / tên gọi |
|---|---|
| Đề tài | Website POS quản lý **chuỗi** cửa hàng tiện lợi (đa cửa hàng, 1 chuỗi) |
| Kiến trúc | SPA React (Vite) ⇄ REST API Spring Boot 3 (Java 17) ⇄ MySQL 8; Docker Compose |
| Bảo mật | JWT (stateless) + BCrypt; phân quyền 3 tầng ADMIN > MANAGER > STAFF (RoleHierarchy) |
| CSDL | **36 bảng, 7 view, 5 trigger toàn vẹn**; InnoDB, utf8mb4; tồn kho suy ra từ chứng từ (view) |
| Mã nguồn | 25 controller, 32+ service, phân lớp Controller → Service → Repository (JPA) |
| Kiểm thử | **72 unit test** (56 POS: bán hàng/FEFO 9, xác thực 4, phân quyền + cô lập 12, báo cáo 4, khuyến mãi 9, tiền tệ 7, ca 7, tiện ích 4 + **16 lương**) — `mvn test` xanh 100% |
| Tính năng nâng cao | VietQR + Web2m tự xác nhận chuyển khoản, Telegram bot thông báo, xuất PDF/Excel, FEFO theo HSD, khuyến mãi Strategy pattern, điều chuyển kho, giá theo chi nhánh, rollup + cache, **phân hệ lương 2 cấp duyệt** |
| Vai trò dữ liệu | Cô lập chi nhánh bằng `store_id` + `StoreContext` (ThreadLocal); ADMIN drill-in bằng header `X-Store-Id` |
| Tài khoản demo | admin / manager / staff (mật khẩu demo `123456`, seed tự động khi chạy dev) |

---

## 1. BÀI GIỚI THIỆU 2 PHÚT (dàn ý học thuộc)

1. **Bài toán**: các chuỗi cửa hàng tiện lợi nhỏ ở VN thường dùng phần mềm bán lẻ đơn cửa hàng —
   không quản lý được *nhiều chi nhánh trong một hệ thống*: tồn kho, giá bán, ca thu ngân, doanh thu,
   và lương nhân viên bị tách rời từng nơi.
2. **Giải pháp**: xây website POS **đa cửa hàng** — một CSDL chung, mọi dữ liệu vận hành gắn `store_id`;
   ADMIN nhìn toàn chuỗi hoặc drill-in từng chi nhánh, MANAGER/STAFF bị "khóa" trong chi nhánh của mình.
3. **Điểm nhấn nghiệp vụ**: tồn kho 2 tầng kho↔kệ theo lô + FEFO theo hạn sử dụng; ca thu ngân có đối soát
   tiền mặt; thanh toán VietQR tự xác nhận qua Web2m; **phân hệ lương & bảng công** tính từ giờ ca thực tế,
   quy trình 2 cấp duyệt.
4. **Công nghệ**: Spring Boot + React + MySQL, JWT/BCrypt, Docker; 72 unit test; xuất báo cáo PDF/Excel.
5. **Kết quả**: demo được trọn luồng: nhập hàng → lên kệ → bán → thanh toán QR → hóa đơn → báo cáo →
   chốt ca → tính lương → duyệt → phiếu lương PDF.

> Câu chốt mở đầu: *"Điểm khác biệt của em so với một POS CRUD thông thường là hệ thống mô hình hóa
> **chuỗi**: cùng một sản phẩm nhưng mỗi chi nhánh có tồn kho, giá bán, doanh thu, ca làm và bảng lương riêng,
> trong khi ban quản trị vẫn nhìn được bức tranh hợp nhất."*

---

## 2. ÔN THEO 10 TIÊU CHÍ CHẤM (bám Bảng tiêu chí của trường)

### Tiêu chí 1 — Phân tích bài toán & yêu cầu (1.0đ)
- **Mình có**: chương SRS trong `docs/BAO_CAO_DO_AN.md` — 3 actor (ADMIN/MANAGER/STAFF), FR/NFR,
  Use Case Diagram, đặc tả UC chi tiết (UC03 quản lý chuỗi, UC10 bán hàng, UC11 thanh toán).
- **Nói gì**: đọc kỹ 3 actor + 2 luồng UC10/UC11 để vẽ lại được bằng miệng.
- **Hay hỏi**: "Actor nào dùng chức năng nào?", "Yêu cầu phi chức năng của em là gì?"
  → NFR: bảo mật (JWT/BCrypt), toàn vẹn giao dịch (transaction + trigger), hiệu năng (rollup + cache), cô lập chi nhánh.

### Tiêu chí 2 — Thiết kế hệ thống (1.5đ) ⭐ điểm nặng nhất nhóm thiết kế
- **Mình có**: kiến trúc phân lớp (Controller→Service→Repository — tương đương MVC phía server),
  ERD 36 bảng, Class/Sequence/Activity diagram trong báo cáo, mô hình đa cửa hàng 3 nhóm bảng
  (gắn store_id trực tiếp / thừa hưởng / dùng chung toàn chuỗi).
- **Nói gì**: thuộc sơ đồ Sequence bán hàng (UC10/11) và cách `StoreContext` suy ra "cửa hàng hiệu lực".
- **Hay hỏi**: "MVC ở đâu trong hệ thống em?" → React = View; Controller REST = Controller;
  Service+Entity = Model. "Tại sao tách FE/BE?" → phát triển độc lập, API tái dùng cho mobile sau này, scale riêng.

### Tiêu chí 3 — Chức năng & nghiệp vụ POS (2.0đ) ⭐⭐ điểm nặng nhất toàn bảng
- **Mình có đủ chức năng lõi**: đăng nhập/phân quyền, sản phẩm/danh mục/NCC, bán hàng tại quầy (giỏ, quét mã),
  tính tiền + khuyến mãi + điểm tích lũy, thanh toán tiền mặt/QR, lưu hóa đơn, lịch sử giao dịch.
- **Nghiệp vụ đặc thù** (đây là chỗ ăn điểm "Xuất sắc"): tồn kho lô + FEFO theo HSD, kho↔kệ,
  ca thu ngân + đối soát tiền mặt, điều chuyển kho giữa chi nhánh (state machine PENDING→SHIPPING→RECEIVED),
  giá riêng từng chi nhánh, kiểm kê hao hụt, **lương & bảng công**.
- **Hay hỏi**: "Luồng bán hàng chạy thế nào từ lúc quét mã đến lúc in hóa đơn?" — tập dượt kể trơn tru 60 giây.

### Tiêu chí 4 — UI/UX (1.0đ)
- **Mình có**: React-Bootstrap, bố cục hub theo tab (Catalog hub, Warehouse hub, Reports, Payroll hub),
  màn POS thao tác nhanh bằng phím/quét mã, toast + modal xác nhận, banner hướng dẫn từng trang.
- **Nói gì**: nhấn "thiết kế cho thu ngân thao tác nhanh" — POS là màn hình một trang, ít click.

### Tiêu chí 5 — CSDL, validation & bảo mật (1.0đ)
- **Mình có**: PK/FK đầy đủ, 0 orphan row (đã audit), Bean Validation ở DTO, xử lý lỗi tập trung
  (GlobalExceptionHandler → JSON message tiếng Việt), BCrypt, JWT, @PreAuthorize 2 lớp
  (controller + service), khóa bi quan chống double-spend điểm tích lũy, throttle đăng nhập theo IP + username.
- **Câu trả lời mẫu về mật khẩu**: *"Em không lưu plain text — dùng BCrypt có salt; JWT ký HS256,
  secret không hard-code khi chạy production."*

### Tiêu chí 6 — Dashboard & báo cáo (0.75đ)
- **Mình có**: dashboard doanh thu/đơn/khách + **% chi phí nhân công trên doanh thu**, so sánh chi nhánh
  (ChainOverview), báo cáo lợi nhuận gộp theo SKU (COGS theo lô, xuất FEFO), hàng bán chậm, hiệu suất nhân viên,
  tổng hợp ngày (rollup bảng riêng chạy đêm — trả lời được câu "dữ liệu lớn thì sao?").
- Số liệu lấy **từ dữ liệu thật** trong DB (tiêu chí yêu cầu đúng chữ này).

### Tiêu chí 7 — Triển khai & demo (1.0đ)
- **Mình có**: Docker Compose (MySQL + BE + FE nginx), healthcheck `/actuator/health`,
  script backup/restore DB, seed dữ liệu mẫu + tài khoản demo tự động, hướng dẫn chạy trong báo cáo.
- **Chuẩn bị demo**: xem mục 5 & 6 bên dưới. **Nhớ dọn ca #175 (355,7h) trong dữ liệu dev trước khi demo lương!**

### Tiêu chí 8 — Báo cáo & trình bày (0.75đ)
- Báo cáo tổng (`BAO_CAO_DO_AN.md`) đủ các phần tiêu chí liệt kê; chương riêng
  `BaoCao_QuanLyLuong.docx` (20 trang, đúng chuẩn học thuật: bìa, mục lục tự động, hình chú thích).
- Trình bày: mở bằng bài 2 phút ở mục 1; mỗi slide 1 ý; khi bị hỏi thì trả lời theo khung
  **"Có/Không → Vì sao → Minh chứng trong hệ thống"**.

### Tiêu chí 9 — Tính năng nâng cao / API (0.5đ)
- Kể được 3 cái *gắn với nghiệp vụ* (tiêu chí chê tính năng "hình thức"):
  1. **VietQR + Web2m**: sinh QR động theo hóa đơn, tự đối soát tiền vào tài khoản → hóa đơn tự chuyển "đã thanh toán".
  2. **Telegram bot**: thông báo chốt ca, duyệt/chi lương về nhóm chi nhánh.
  3. **Xuất PDF/Excel**: hóa đơn, bảng lương, phiếu lương (STAFF chỉ tải được phiếu của mình — chống IDOR).
- Nếu hỏi "sao không có AI?": *"Em ưu tiên chiều sâu nghiệp vụ POS thực tế thay vì gắn AI hình thức —
  đúng khuyến nghị trong tiêu chí; hướng phát triển là dự báo nhập hàng từ dữ liệu rollup đã có."*

### Tiêu chí 10 — Chất lượng mã & quản lý dự án (0.5đ)
- Phân lớp rõ, DTO tách entity, dùng chung helper (useList hook, downloadBlob…), Strategy+Factory cho khuyến mãi,
  Git có lịch sử commit theo feature (`fix:`, `refactor:`, `test:`…), 72 unit test.
- Hay hỏi: "Em có dùng design pattern nào không?" → **Strategy + Factory** (giảm giá %, giảm tiền),
  **Repository**, **DTO**, **Filter/Interceptor** (StoreContextFilter), **Snapshot** (payslip, chốt tiền ca).

---

## 3. DEEP-DIVE PHÂN HỆ LƯƠNG (chương báo cáo riêng — chắc chắn bị hỏi kỹ)

### 3.1. Kể lại thiết kế trong 90 giây
- **5 bảng**: `employee_pay_profiles` (cấu hình lương/người), `payroll_periods` (kỳ = chi nhánh × tháng),
  `payslips` (phiếu lương — *snapshot*), `payslip_adjustments` (thưởng/phạt), `attendance_entries`
  (chấm công thủ công: WORK / LEAVE_PAID / LEAVE_UNPAID).
- **Nguồn giờ công (2 nguồn)**: (1) ca thu ngân đã đóng: giờ = closed_at − opened_at, tính vào tháng mở ca;
  (2) chấm công thủ công cho người không đứng quầy (kho, bảo vệ) hoặc bổ sung. Giờ hưởng lương =
  Σ giờ ca + Σ (WORK + LEAVE_PAID). LEAVE_UNPAID chỉ ghi nhận.
- **Vòng đời kỳ lương**: `DRAFT → PENDING_APPROVAL → APPROVED → PAID`.
  MANAGER/ADMIN lập & trình; **chỉ ADMIN duyệt** → *separation of duties* (người lập ≠ người duyệt).
  Duyệt/chi xong bắn Telegram cho chi nhánh.

### 3.2. Công thức tính lương (thuộc ví dụ số!)
- Giờ công chuẩn tháng: **208h** (có thể cấu hình theo hồ sơ lương).
- **HOURLY**: lương chính = giờ_thường × đơn_giá; OT = (giờ_làm − 208) × đơn_giá × hệ_số (vd 1.5).
  - Ví dụ thuộc lòng: 250h, 30.000đ/h, OT×1.5 → 208×30k = **6.240.000** + 42×30k×1.5 = **1.890.000** → 8.130.000đ.
- **MONTHLY (pro-rata)**: lương chính = lương_cơ_bản × giờ_thường/208; OT quy ra đơn giá giờ = lương/208.
  - Ví dụ: lương 8.320.000, làm 104h → nhận 4.160.000 (50% công). Đơn giá OT = 8.320.000/208 = 40.000đ/h.
  - **Vì sao pro-rata?** Trung thực với giờ làm thực tế — làm nửa công hưởng nửa lương; đây là *lựa chọn thiết kế
    có chủ đích*, đã ghi rõ trong báo cáo.
- **Thực lĩnh** = lương gộp (chính + OT + phụ cấp) + thưởng − khấu trừ. **Bất biến: thực lĩnh ≥ 0** (mới vá).

### 3.3. Ba cải tiến vừa hoàn thành (kể được là ăn điểm "xử lý nghiệp vụ")
1. **Chặn thực lĩnh âm — 2 lớp**: nhập khoản trừ vượt thực lĩnh → từ chối ngay; và khi trình duyệt,
   nếu còn phiếu âm (do tính lại) → chặn submit kèm thông báo hướng dẫn.
2. **Cảnh báo ca dài bất thường (>16h)**: khi tính lương kỳ nháp, hệ thống quét ca đóng quá dài
   (thường do quên đóng ca) và hiện khung cảnh báo "Rà soát bảng công trước khi trình duyệt".
3. **Cảnh báo trùng giờ**: ngày vừa có ca vừa có chấm công thủ công → cảnh báo lúc nhập (toast)
   và lúc tính lương — tránh tính đúp giờ công.
- Triết lý trả lời nếu bị hỏi "sao không tự sửa?": *"Hệ thống chỉ cảnh báo, không tự sửa số liệu —
  quyết định cuối thuộc người lập bảng lương, vì ca dài có thể là thật (lễ tết tăng cường)."*

### 3.4. Vì sao payslip là snapshot?
- Khi kỳ đã duyệt/chi, phiếu lương **đóng băng** — dù sau đó hồ sơ lương hay ca có sửa, số đã chi không đổi
  (giống chốt tiền mặt khi đóng ca). Kỳ DRAFT thì được "Tính lại" thoải mái.

### 3.5. Kiểm thử phân hệ lương
- **16 unit test** (JUnit 5 + Mockito), viết **TDD** (test trước, code sau): công thức HOURLY/MONTHLY/OT/phụ cấp,
  hồ sơ thiếu → mặc định an toàn, thưởng/phạt gộp đúng, chặn phiếu âm (2 test), cảnh báo ca dài,
  cảnh báo trùng ngày (3 test), tổng kỳ recompute.
- 2 câu SQL cảnh báo đã chạy kiểm chứng trực tiếp trên MySQL thật.

### 3.6. Hạn chế phân hệ lương (trả lời trung thực — đã ghi ở mục 6.2 báo cáo)
- OT gộp theo **tháng**, chưa tách theo ngày/đêm/lễ như Bộ luật Lao động (150%/200%/300%) → hướng phát triển.
- Chưa tính BHXH/BHYT/thuế TNCN — phạm vi đồ án dừng ở lương gộp nội bộ.
- Chưa tự đóng ca quên đóng — hiện mới cảnh báo.

---

## 4. NGÂN HÀNG CÂU HỎI HỘI ĐỒNG + GỢI Ý TRẢ LỜI

### Nhóm A — Kiến trúc & công nghệ
| Câu hỏi | Trả lời ngắn gọn |
|---|---|
| Tại sao chọn Spring Boot + React? | BE: hệ sinh thái enterprise (Security, JPA, Validation), phân quyền chín muồi. FE: SPA thao tác nhanh cho POS, component tái dùng. Tách API → sau này làm app mobile không sửa BE. |
| Tại sao MySQL? | Dữ liệu quan hệ chặt (hóa đơn–dòng hàng–lô–ca), cần transaction ACID + FK; InnoDB khóa dòng phù hợp POS nhiều ghi. |
| JWT khác session? Sao chọn JWT? | JWT stateless — server không giữ phiên, scale ngang dễ; token mang role + storeId. Nhược điểm: khó thu hồi trước hạn (em để hạn 24h, nêu là hạn chế). |
| Transaction dùng ở đâu? | `@Transactional` ở service bán hàng/nhập kho/điều chuyển/tính lương — mọi bước trong 1 giao dịch, lỗi giữa chừng rollback toàn bộ (ví dụ: trừ kho + ghi hóa đơn + trừ điểm phải cùng thành công). |
| Race condition xử lý thế nào? | Điểm tích lũy: khóa bi quan `SELECT ... FOR UPDATE` khi bán → 2 quầy không tiêu cùng số điểm 2 lần. Tồn kho: suy từ chứng từ + trigger toàn vẹn. |
| Tồn kho tính thế nào? | Không lưu số tồn "cứng" — **suy ra từ chứng từ** qua view: nhập − xuất − hao hụt; bất biến `trên_kệ + trong_kho = còn_lại`. Chống lệch số vì mọi thay đổi đều có chứng từ. |
| FIFO/FEFO khác gì, hệ thống dùng cái nào? | FIFO = nhập trước xuất trước (theo NGÀY NHẬP); FEFO = hết hạn trước xuất trước (theo HSD). Hàng tiện lợi đa số có HSD nên hệ thống dùng **FEFO**: lô hạn gần xuất trước, lô quá hạn bị loại; không có HSD/trùng hạn thì rơi về FIFO theo thứ tự nhập. Nhờ gắn từng đơn vị bán vào đúng lô → COGS chính xác theo lô + cảnh báo cận hạn. |
| Lợi nhuận trong báo cáo là gộp hay ròng? | Là **lợi nhuận GỘP** = doanh thu − giá vốn theo đúng lô đã bán. KHÔNG phải lợi nhuận ròng vì hệ thống không quản chi phí vận hành (mặt bằng, lương, điện nước) — nêu rõ là giới hạn, hướng phát triển là thêm phân hệ chi phí. |
| "Một lô một kệ" là quy luật nghiệp vụ à? | Không — là **đơn giản hóa thiết kế có chủ đích**: tồn kệ suy được từ 2 chứng từ lên kệ/về kho, biết ngay lô nằm kệ nào. Cửa hàng thật có thể bày 1 lô nhiều chỗ → giới hạn; tình thế: tách 2 dòng nhập; triệt để: bảng phân bổ lô–kệ nhiều-nhiều (hướng phát triển). |
| Dữ liệu lớn thì báo cáo có chậm? | Có bảng rollup doanh thu ngày (job đêm + backfill), báo cáo đọc rollup thay vì quét hóa đơn; thêm cache giá chi nhánh. |

### Nhóm B — Bảo mật & phân quyền
| Câu hỏi | Trả lời ngắn gọn |
|---|---|
| Phân quyền cài thế nào? | 3 tầng có thứ bậc (RoleHierarchy). 2 lớp kiểm tra: `@PreAuthorize` ở controller + kiểm tra store ở service. FE cũng chặn route theo rank nhưng chỉ là UX — quyền thật nằm ở BE. |
| Cô lập chi nhánh thế nào? | `StoreContextFilter` đọc user từ JWT: MANAGER/STAFF gắn cứng storeId của họ (header giả cũng vô hiệu); ADMIN không header = toàn chuỗi, có `X-Store-Id` = drill-in 1 chi nhánh. |
| IDOR là gì, em gặp chưa? | Truy cập tài nguyên người khác bằng cách đoán id. Em từng phát hiện khi audit: tải PDF hóa đơn chi nhánh khác theo id — đã vá bằng `assertSameStore`, và phiếu lương PDF cũng chỉ chủ nhân tải được. |
| Chống dò mật khẩu? | Khóa tạm theo username **và** theo IP (20 lần sai/IP → khóa 5 phút); mật khẩu tối thiểu 8 ký tự, BCrypt. |
| SQL Injection? | JPA/parameterized query toàn bộ — không nối chuỗi SQL; 2 câu native query của payroll cũng dùng tham số bind. |

### Nhóm C — Nghiệp vụ POS
| Câu hỏi | Trả lời ngắn gọn |
|---|---|
| Kể luồng bán hàng | Mở ca → quét mã/chọn SP (giá theo chi nhánh) → giỏ hàng → áp khuyến mãi + điểm → chọn thanh toán (tiền mặt/QR động) → QR thì Web2m tự đối soát → lưu hóa đơn + trừ kho FEFO + cộng điểm → in/PDF. |
| Hủy hóa đơn thì sao? | Đảo toàn bộ: trả tồn về lô, hoàn/thu hồi điểm có ghi sổ (ledger), nhưng **đối soát tiền ca đã đóng không đổi** (snapshot lúc đóng ca) — tiền thực tế đã đếm rồi. |
| Đối soát tiền ca? | Tiền dự kiến = đầu ca + tiền mặt bán + thu − chi trong ca; cuối ca thu ngân đếm thực tế → lệch bao nhiêu hiện ngay, lưu snapshot. |
| Khuyến mãi thiết kế sao? | Strategy pattern: mỗi loại giảm giá (%, số tiền) một strategy, Factory chọn theo loại → thêm loại mới không sửa code cũ (Open-Closed). Có điều kiện hóa đơn tối thiểu, khung thời gian, giới hạn lượt. |
| Điều chuyển kho giữa chi nhánh? | State machine PENDING→SHIPPING→RECEIVED (hoặc CANCELLED có hoàn kho); xuất ghi chứng từ TRANSFER_OUT ở nguồn, nhận tạo phiếu nhập TRANSFER ở đích — giữ nguyên lô/HSD, tổng tồn toàn chuỗi bảo toàn. |

### Nhóm D — Câu hỏi "bẫy" về hạn chế (trả lời trung thực + hướng phát triển)
| Câu hỏi | Cách trả lời |
|---|---|
| Sao không có hoàn trả hàng? | Phạm vi đồ án chủ đích bỏ (đã ghi báo cáo); kiến trúc chứng từ sẵn sàng thêm phiếu trả — hướng phát triển. |
| Có xuất hóa đơn điện tử thuế? | Hệ thống đã tính VAT nội bộ; tích hợp cổng HĐĐT (VNPT/Viettel) là hướng phát triển — cần hợp đồng nhà cung cấp thật. |
| Lương đã đúng luật lao động chưa? | Đúng cho lương gộp nội bộ; OT theo lễ/đêm và BHXH/thuế TNCN là hướng phát triển (nói rõ 150/200/300% để chứng tỏ hiểu luật). |
| JWT bị lộ thì sao? | Hạn 24h + HTTPS khi triển khai; hạn chế đã nêu: chưa có refresh/revoke — hướng phát triển dùng refresh token + blacklist. |
| Hệ thống chịu tải bao nhiêu? | Chưa đo tải chính thức (trung thực); nhưng đã có rollup/cache/index đầy đủ FK, kiến trúc stateless scale ngang được. |

> **Khung trả lời khi không biết**: đừng bịa. Nói: *"Phần này em chưa triển khai/chưa đo, nhưng hướng xử lý
> của em sẽ là …"* — hội đồng chấm cách tư duy, không chấm việc biết mọi thứ.

---

## 5. KỊCH BẢN DEMO 7 PHÚT (theo đúng tiêu chí 7: trọn luồng nghiệp vụ)

1. **(30s) Đăng nhập ADMIN** → dashboard toàn chuỗi → đổi scope sang 1 chi nhánh (chỉ vào dropdown — nói "cô lập dữ liệu").
2. **(60s) Nhập kho**: tạo phiếu nhập có lô + HSD → tồn kho tăng → chuyển hàng lên kệ.
3. **(90s) Bán hàng (đăng nhập STAFF)**: mở ca → quét/chọn 2-3 SP → áp khuyến mãi → thanh toán QR
   (nếu không demo được Web2m thật thì thanh toán tiền mặt + nói cơ chế) → hóa đơn.
4. **(45s) Chốt ca**: đóng ca → màn đối soát tiền mặt (đầu ca + bán − chi = dự kiến, nhập thực đếm).
5. **(90s) Lương**: (MANAGER) tạo kỳ tháng này → "Tính lương" → chỉ khung **cảnh báo rà soát** (nếu có) →
   xem phiếu lương 1 nhân viên (giờ ca + chấm công) → thêm 1 khoản thưởng → thử nhập khoản trừ quá tay
   → **hệ thống chặn thực lĩnh âm** (điểm nhấn!) → trình duyệt → (ADMIN) duyệt → Telegram nhận thông báo → chi.
6. **(45s) Báo cáo**: dashboard cập nhật doanh thu vừa bán + % chi phí nhân công; mở báo cáo lợi nhuận SKU.
7. **(30s) Chốt**: "Toàn bộ luồng từ nhập hàng đến trả lương chạy trên cùng một hệ thống, tách bạch từng chi nhánh."

---

## 6. CHECKLIST TRƯỚC BUỔI BẢO VỆ & XỬ LÝ SỰ CỐ

- [ ] **Dọn dữ liệu dev**: đóng/xóa **ca #175 (355,7h)** nếu còn — hoặc *giữ lại có chủ đích* để demo tính năng cảnh báo ca dài (kịch bản đẹp: bấm Tính lương → cảnh báo hiện đúng ca này → giải thích).
- [ ] Chạy trước `mvn test` (72/72) + `npm run build` sáng hôm bảo vệ; chụp màn hình kết quả để dán slide.
- [ ] Khởi động bằng Docker Compose trước giờ demo ≥ 15 phút; kiểm tra `/actuator/health` = UP.
- [ ] Chuẩn bị **phương án B**: video quay màn hình luồng demo + PDF hóa đơn/phiếu lương đã xuất sẵn — nếu máy/mạng hỏng vẫn trình bày được.
- [ ] In sẵn: báo cáo tổng + chương lương (docx 20 trang) + bảng tiêu chí tự đối chiếu.
- [ ] Thuộc: 2 ví dụ số ở mục 3.2, câu chốt mở đầu mục 1, và 3 hạn chế + hướng phát triển.
- [ ] Web2m/Telegram cần mạng — nếu phòng bảo vệ không có mạng, chuyển kịch bản QR sang "giải thích cơ chế + ảnh chụp".

---

*Sinh viên: Hồ Huy Hoàng — MSSV 2305XCT0393 — Trường ĐH Hùng Vương TP.HCM.*
