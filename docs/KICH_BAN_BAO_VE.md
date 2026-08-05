# KỊCH BẢN BẢO VỆ ĐỒ ÁN — LỜI THOẠI THEO SLIDE + NGÂN HÀNG CÂU HỎI

> Dùng kèm: slide `docs/Báo Cáo/SLIDE_BAO_VE_MiniMartPOS_HoHuyHoang_2305XCT0393.pptx` (16 slide)
> và checklist kỹ thuật trong `docs/ON_TAP_BAO_VE.md`.
> Tổng thời lượng nói: **12–13 phút** (chừa 1–2 phút đệm). Nói chậm, dứt câu, KHÔNG đọc slide —
> slide chỉ là gạch đầu dòng, lời thoại dưới đây là phần "nói thêm" của mình.

---

## PHẦN 1 — LỜI THOẠI THEO TỪNG SLIDE

### Slide 1 — Bìa (30 giây)
> Kính thưa quý thầy cô và hội đồng, em là Hồ Huy Hoàng, MSSV 2305XCT0393.
> Hôm nay em xin trình bày đồ án Công nghệ phần mềm với đề tài: **"Phân tích, thiết kế,
> xây dựng và triển khai website POS cho cửa hàng tiện lợi"** — hệ thống MiniMart POS,
> có khả năng quản lý **chuỗi nhiều cửa hàng**.

*(Mẹo: nhìn hội đồng, không nhìn màn hình. Câu đầu tiên quyết định nhịp cả buổi.)*

### Slide 2 — Nội dung (20 giây)
> Bài trình bày của em gồm 6 phần, tương ứng 6 chương của báo cáo: tổng quan đề tài,
> cơ sở lý thuyết, phân tích và thiết kế, xây dựng hệ thống, kiểm thử và đánh giá,
> cuối cùng là kết luận và hướng phát triển.

*(Không đọc từng ô — lướt 1 câu là đủ, để dành thời gian cho phần sau.)*

### Slide 3 — Bối cảnh (1 phút)
> Cửa hàng tiện lợi có đặc thù vận hành rất riêng: số mặt hàng lớn, giao dịch dày,
> nhiều mặt hàng thực phẩm có **hạn sử dụng ngắn**, và nhiều ca nhân viên luân phiên.
> Khi quản lý bằng sổ sách hoặc Excel, cửa hàng gặp hàng loạt vấn đề: tính tiền chậm,
> không nắm được tồn thực tế ở kho và trên kệ, hàng cận hạn không được cảnh báo dẫn đến
> thất thoát, và cuối ngày rất khó đối soát tiền mặt – chuyển khoản theo từng ca.
> Khi mở rộng thành chuỗi, bài toán khó hơn nữa: danh mục – giá – khuyến mãi cần dùng chung,
> nhưng tồn kho, ca làm việc, doanh thu của từng cửa hàng phải tách bạch.
>
> Điểm em muốn nhấn mạnh: đề tài xuất phát từ **khảo sát thực tế** tại Siêu thị tiện lợi
> Super 138 Mini Mart ở quận Tân Phú — yêu cầu được thu bằng phiếu khảo sát trực tiếp,
> không phải đề bài giả định.

### Slide 4 — Mục tiêu & phạm vi (1 phút)
> Mục tiêu của em là xây dựng một website POS hoàn chỉnh bám sát luồng bán lẻ thực tế:
> nhập kho theo lô, lên kệ, bán hàng quét mã vạch, thanh toán tiền mặt hoặc QR,
> in hóa đơn, và báo cáo doanh thu – **lợi nhuận gộp** theo từng cửa hàng và toàn chuỗi.
> Một vài con số của hệ thống: cơ sở dữ liệu **28 bảng chuẩn 3NF cộng 6 view**,
> **3 vai trò phân tầng** ADMIN – MANAGER – STAFF, và **56 trên 56 unit test PASS**.
> Về phạm vi, em chủ động khoanh vùng: làm bán tại quầy cho chuỗi nhiều cửa hàng;
> KHÔNG làm bán online, trả hàng sau bán và kế toán công nợ — các phần này em nêu rõ
> trong chương 1 và đưa vào hướng phát triển.

*(Nếu bị hỏi "sao không làm trả hàng": phạm vi khoanh có chủ đích ngay từ đầu, cửa hàng
khảo sát không nhận trả hàng — trả lời tự tin, không phải "em không kịp".)*

### Slide 5 — Cơ sở lý thuyết (1 phút 30)
> Em xin nêu ba nền tảng lý thuyết quan trọng nhất.
> **Thứ nhất, FEFO thay vì FIFO.** FIFO là nhập trước – xuất trước. Nhưng với hàng có hạn
> sử dụng, tiêu chí đúng là **hạn hết trước – xuất trước**, tức FEFO. Hệ thống của em
> phân bổ lô theo hạn sử dụng gần nhất, và **loại hẳn lô đã quá hạn** khỏi việc bán.
> **Thứ hai, lợi nhuận gộp.** Hệ thống tính lợi nhuận bằng doanh thu trừ giá vốn hàng bán,
> trong đó giá vốn được tính **đích danh theo từng lô đã xuất** — đây là lợi nhuận GỘP.
> Em phân biệt rõ với lợi nhuận ròng, vì ròng phải trừ thêm chi phí vận hành như mặt bằng,
> lương, điện nước — phần hệ thống chưa quản lý, và em ghi rõ đây là giới hạn.
> **Thứ ba, toàn vẹn giao dịch đồng thời.** Một hóa đơn gồm nhiều bước ghi phải nằm trong
> MỘT transaction; hai quầy bán cùng mặt hàng được xử lý bằng khóa bi quan; và mạng chập
> chờn bấm thanh toán hai lần được chống bằng idempotency key.

### Slide 6 — Kiến trúc (1 phút)
> Hệ thống theo kiến trúc Client–Server tách lớp: **React SPA** ở trình duyệt,
> gọi **REST API Spring Boot** qua JWT, phía dưới là **MySQL 8**.
> API là stateless — mọi request mang token, phân quyền bằng @PreAuthorize kết hợp
> Role Hierarchy. Toàn bộ endpoint trả về một phong bì phản hồi thống nhất.
> Hệ thống còn tích hợp ngoài: VietQR và WEB2M để đối soát chuyển khoản tự động,
> Telegram để cảnh báo, và xuất PDF, Excel.

### Slide 7 — CSDL & Kho↔Kệ (1 phút 30) ⭐ trọng tâm
> Đây là phần thiết kế em tâm đắc nhất. Cơ sở dữ liệu 28 bảng chuẩn 3NF, nhưng điểm khác
> biệt là: **em KHÔNG lưu cột tồn kho**. Mọi con số tồn được suy ra qua VIEW từ chứng từ
> gốc — phiếu nhập, phiếu lên kệ, hóa đơn. Chứng từ thì bất biến, chỉ thêm không sửa.
> Cách này loại bỏ nguyên nhân phổ biến nhất gây lệch số: dữ liệu lưu trùng hai nơi.
> Hủy hóa đơn cũng không cần cộng trả tồn — view tự bỏ qua hóa đơn đã hủy.
> Tồn kho chia hai tầng: **KHO và KỆ** — POS chỉ bán phần hàng ĐANG TRÊN KỆ,
> đúng như vận hành thật của cửa hàng.
> Về quy ước **"một lô – một kệ"**: em xin nói rõ đây KHÔNG phải quy luật của ngành bán lẻ,
> mà là **quyết định đơn giản hóa thiết kế có chủ đích** của em — đổi một phần linh hoạt
> lấy tính đúng đắn dễ kiểm soát. Giới hạn và hướng khắc phục bằng bảng phân bổ
> nhiều-nhiều em đã trình bày trong báo cáo.

*(Chủ động nói trước phần "một lô một kệ" — thầy đã góp ý phần này, nói trước thể hiện tiếp thu.)*

### Slide 8 — Luồng bán hàng (1 phút 30) ⭐ trọng tâm
> Luồng bán hàng là nơi tập trung nhiều kỹ thuật nhất. Khi thu ngân quét mã và thanh toán:
> hệ thống **khóa các sản phẩm theo thứ tự id tăng dần** — mọi giao dịch cùng một thứ tự
> khóa toàn cục nên tránh được chu trình chờ, giảm mạnh nguy cơ deadlock.
> Sau đó **phân bổ lô theo FEFO**, loại lô quá hạn. Toàn bộ việc ghi — hóa đơn, chi tiết,
> phân bổ lô, điểm tích lũy, lượt khuyến mãi — nằm trong MỘT transaction: thiếu tồn ở bất kỳ
> dòng nào là rollback toàn bộ và trả mã 409, không cho phép bán vượt tồn.
> Trường hợp mạng chập chờn, thu ngân bấm thanh toán lại: client sinh **idempotency key**,
> ràng buộc UNIQUE ở CSDL bảo đảm chỉ một hóa đơn được tạo — request lặp nhận lại đúng
> hóa đơn cũ, chống thu tiền hai lần.
> Thuế GTGT nằm TRONG giá bán theo chuẩn bán lẻ Việt Nam, được bóc tách bằng công thức
> A nhân r chia (100 cộng r), phân bổ theo tỉ lệ sau giảm giá.

### Slide 9 — Đa cửa hàng & bảo mật (1 phút)
> Về phân quyền, em dùng mô hình phân tầng: ADMIN quản toàn chuỗi, MANAGER quản một
> cửa hàng, STAFF bán hàng trong ca của mình — quyền cấp trên bao trùm cấp dưới qua
> Spring Role Hierarchy.
> Cô lập dữ liệu chuỗi được thực hiện bằng **StoreContext**: người dùng gắn cửa hàng thì
> mọi truy vấn tự lọc theo cửa hàng đó, kể cả cố tình gửi header giả cũng bị bỏ qua.
> Nhân viên cửa hàng A không thể đọc hay sửa dữ liệu cửa hàng B — em có test chứng minh.
> Ngoài ra: mật khẩu băm BCrypt, JWT hạn 24 giờ, khóa tạm sau 5 lần đăng nhập sai,
> khóa theo IP sau 20 lần, và audit log cho các thao tác nhạy cảm như hủy hóa đơn, đổi giá.

### Slide 10 — Phân tích dữ liệu (1 phút)
> Hệ thống không dừng ở ghi nhận mà còn hỗ trợ quyết định nhập hàng.
> Tồn kho an toàn tính theo công thức **SS bằng z nhân sigma nhân căn L**, trong đó
> mức phục vụ z gán theo nhóm ABC: nhóm A quan trọng nhất dùng z 2,05 tương ứng
> mức phục vụ 98%, nhóm B 1,65, nhóm C 1,28.
> Điểm đặt hàng lại ROP, lượng đặt kinh tế EOQ có trần 60 ngày bán để tránh ôm hàng
> cận hạn. Cuối cùng là gợi ý mua kèm dựa trên chỉ số **lift** — lift lớn hơn 1 nghĩa là
> hai sản phẩm hay được mua cùng nhau hơn ngẫu nhiên.

### Slide 11–12 — Demo (30 giây + demo)
> Đây là một số màn hình chính của hệ thống. Nếu hội đồng cho phép, em xin demo trực tiếp
> luồng bán hàng trong khoảng 2–3 phút.

*(Xem kịch bản demo rút gọn ở Phần 2. Nếu không được demo: nói theo caption 2 slide,
mỗi ảnh 2 câu, nhấn "báo cáo hiển thị LỢI NHUẬN GỘP theo từng cửa hàng".)*

### Slide 13 — Kiểm thử (1 phút)
> Em kiểm thử ở ba tầng. Tầng unit test: **56 trên 56 test PASS** bằng lệnh mvn test,
> phủ các nghiệp vụ quan trọng nhất theo đúng góp ý của thầy: bán hàng và phân bổ lô FEFO,
> xác thực và chống brute-force, phân quyền và cô lập đa cửa hàng, báo cáo có kiểm tra
> round-trip file Excel, cùng khuyến mãi và các tiện ích tiền tệ.
> Cộng thêm 16 test của phân hệ lương, tổng cộng là 72 trên 72.
> Tầng tích hợp: 13 kịch bản end-to-end. Tầng giao diện: kiểm theo kịch bản demo 15 bước.

### Slide 14 — Kết quả & hạn chế (1 phút)
> Về kết quả: hệ thống chạy đủ luồng nghiệp vụ đầu-cuối cho chuỗi nhiều cửa hàng và
> được kiểm chứng bằng kiểm thử thật.
> Về hạn chế, em xin chủ động nêu: hệ thống mới dừng ở **lợi nhuận gộp** vì chưa quản lý
> chi phí vận hành; quy ước một lô một kệ là đơn giản hóa; chưa có bán online và trả hàng
> sau bán; và triển khai hiện ở dạng đơn nút. Các hạn chế này em đều đã phân tích trong
> báo cáo kèm hướng khắc phục cụ thể.

*(Chủ động nêu hạn chế TRƯỚC khi bị hỏi — biến điểm trừ thành điểm cộng "hiểu rõ hệ thống".)*

### Slide 15 — Hướng phát triển (40 giây)
> Hướng phát triển em xếp theo độ ưu tiên: thứ nhất, bảng phân bổ lô–kệ nhiều-nhiều để
> gỡ giới hạn một lô một kệ; thứ hai, phân hệ chi phí vận hành để tính được lợi nhuận
> RÒNG thật sự; tiếp theo là trả hàng sau bán, kênh online, và mở rộng hạ tầng nhiều nút
> cho chuỗi lớn.

### Slide 16 — Cảm ơn (15 giây)
> Em xin chân thành cảm ơn quý thầy cô và hội đồng đã lắng nghe.
> Em sẵn sàng trả lời các câu hỏi của hội đồng ạ.

---

## PHẦN 2 — KỊCH BẢN DEMO RÚT GỌN (2–3 PHÚT)

> Bản đầy đủ 15 bước ở Bảng 4.7 trong báo cáo. Bản rút gọn khi thời gian ít:

1. **Đăng nhập** thu ngân → vào màn POS → **mở ca** với tiền đầu ca.
2. **Quét mã / bấm chọn 2 sản phẩm** — nói: *"giá hiển thị do máy chủ quyết định,
   cửa hàng nào có giá riêng thì tự áp giá đó"*.
3. **Thanh toán tiền mặt** → chỉ vào hóa đơn: *"VAT được bóc tách trong giá,
   hàng vừa bán bị trừ đúng lô có hạn gần nhất — FEFO"*.
4. Mở **Tồn kho & cảnh báo** → chỉ cột tồn kệ/kho vừa giảm + cảnh báo cận hạn.
5. Mở **Báo cáo doanh thu** → chỉ: *"doanh thu và LỢI NHUẬN GỘP cập nhật ngay,
   xuất được Excel"*. (Hết — đừng tham demo thêm.)

**Phòng hờ:** nếu máy/mạng trục trặc → dùng video quay sẵn + PDF hóa đơn đã xuất
(chuẩn bị theo checklist trong ON_TAP_BAO_VE.md). Câu nói chữa cháy: *"Để tiết kiệm
thời gian của hội đồng, em xin trình bày bằng video đã quay sẵn luồng này."*

---

## PHẦN 3 — NGÂN HÀNG CÂU HỎI DỰ KIẾN (HỌC THUỘC Ý, KHÔNG HỌC VẸT)

### Nhóm A — Lý thuyết & nghiệp vụ

**A1. FEFO khác FIFO thế nào? Vì sao em chọn FEFO?**
→ FIFO: nhập trước – xuất trước, theo thứ tự nhập. FEFO: **hết hạn trước – xuất trước**,
theo hạn sử dụng. Cửa hàng tiện lợi nhiều thực phẩm HSD ngắn, mục tiêu là giảm hàng hết
hạn phải hủy → FEFO đúng bản chất hơn. Trong code: sắp lô theo `expiry_date` tăng dần,
lô không có HSD xếp cuối, **lô quá hạn bị loại hẳn** khỏi phân bổ bán.
*Từ khóa: expiry_date, NULL xếp cuối, loại lô quá hạn, giảm hủy hàng.*

**A2. Lợi nhuận gộp và lợi nhuận ròng khác gì nhau? Hệ thống em tính cái nào?**
→ Gộp = doanh thu thuần − giá vốn hàng bán (COGS). Ròng = gộp − chi phí vận hành
(mặt bằng, lương, điện nước) − thuế TNDN. Hệ thống em quản lý doanh thu và giá vốn
theo lô nhưng **không quản lý chi phí vận hành** → mọi chỉ tiêu là lợi nhuận **GỘP**,
cột CSDL cũng tên `gross_profit`. Em ghi rõ giới hạn này và đưa phân hệ chi phí vào
hướng phát triển.

**A3. Vì sao tính giá vốn đích danh theo lô mà không dùng bình quân gia quyền?**
→ Vì hệ thống đã sẵn phân bổ lô FEFO cho từng dòng bán (bảng `invoice_item_batches`),
nên biết chính xác đơn vị hàng bán ra thuộc lô nhập giá bao nhiêu. Ưu điểm: giá vốn đúng
theo từng đợt nhập, không nhòe khi giá mua biến động, và **sửa giá vốn hiện hành của sản
phẩm không làm thay đổi lợi nhuận của hóa đơn quá khứ**.

**A4. VAT tính thế nào khi giá đã gồm thuế?**
→ Chuẩn bán lẻ VN là giá niêm yết ĐÃ GỒM VAT. Thuế của một dòng = tiền × r/(100+r).
Ví dụ 60.000đ thuế suất 8%: VAT = 60.000×8/108 ≈ 4.444đ. Khi có giảm giá, thuế được
phân bổ lại theo tỉ lệ sau giảm (hàm Money.prorate), làm tròn đến đồng.

**A5. Giải thích công thức tồn kho an toàn và các tham số?**
→ SS = z·σ·√L: σ là độ lệch chuẩn nhu cầu ngày, L là lead time (ngày chờ hàng về),
z là mức phục vụ. Em gán z **theo nhóm ABC**: nhóm A (giá trị cao) z=2,05 ≈ phục vụ 98%,
B 1,65 ≈ 95%, C 1,28 ≈ 90% — hàng càng quan trọng càng ít cho phép hết hàng.
ROP = nhu cầu bình quân ngày × L + SS: tồn chạm ROP là gợi ý đặt hàng.
EOQ = √(2DS/H) cân bằng chi phí đặt hàng và giữ hàng, có **trần 60 ngày bán** để
tránh ôm hàng cận hạn.

**A6. Lift trong gợi ý mua kèm là gì? Vì sao không dùng confidence?**
→ lift(A→B) = P(A∩B) / (P(A)·P(B)) — đo mức "mua cùng nhau nhiều hơn ngẫu nhiên";
lift > 1 mới có ý nghĩa gợi ý. Confidence P(B|A) bị thiên lệch về các mặt hàng phổ biến
(bán chạy thì confidence cao với mọi thứ). Trong code, khi A cố định, thứ hạng theo lift
tương đương thứ hạng theo tỉ số co(A,B)/n(B) nên em dùng tỉ số rút gọn này để xếp hạng,
kèm ngưỡng hỗ trợ tối thiểu 2 lần mua chung.

### Nhóm B — Thiết kế CSDL

**B1. Vì sao KHÔNG lưu cột tồn kho? Không sợ chậm à?**
→ Lưu cột tồn nghĩa là cùng một sự thật nằm ở hai nơi (chứng từ + cột tồn) — mọi bug
quên cập nhật đều gây lệch số. Em chọn: chứng từ bất biến là nguồn sự thật duy nhất,
tồn suy qua VIEW → loại bỏ nguyên nhân phổ biến nhất gây lệch. Về hiệu năng: view
`v_batch_stock` viết dạng pre-aggregated join, có 41 chỉ mục theo trục truy vấn nóng,
và báo cáo dài hạn đọc bảng tổng hợp `daily_sales_rollup` thay vì tính sống.

**B2. "Một lô một kệ" — thực tế một lô bày nhiều chỗ thì sao?**
→ Em xin khẳng định trước: đây **không phải quy luật nghiệp vụ chung của ngành** — cửa
hàng thật hoàn toàn có thể bày một lô ở nhiều vị trí. Đây là **đơn giản hóa thiết kế có
chủ đích**: (1) tồn kệ của lô suy được từ đúng 2 loại chứng từ lên kệ/về kho; (2) luôn trả
lời được "lô này ở kệ nào" cho nhân viên tìm hàng; (3) tránh lớp lỗi chia số lượng một lô
cho nhiều vị trí. Giải pháp tình thế: tách 2 dòng trên phiếu nhập (thành 2 lô cùng hạn
cùng giá). Khắc phục căn cơ: bảng phân bổ lô–kệ nhiều-nhiều — ưu tiên số 1 trong hướng
phát triển.

**B3. Hủy hóa đơn thì hoàn tồn thế nào?**
→ Không cần cộng trả gì cả. View tồn chỉ tính các phân bổ trỏ tới hóa đơn khác CANCELLED
→ đổi trạng thái hóa đơn là tồn "tự hoàn". Đây là lợi ích trực tiếp của thiết kế
hướng-chứng-từ. Hủy có audit log: ai hủy, lúc nào, lý do.

**B4. Vì sao dòng phiếu nhập kiêm luôn vai trò lô hàng?**
→ Mỗi dòng phiếu nhập tự nhiên mang đủ thuộc tính của một lô: số lượng, giá nhập, HSD.
Tách bảng lô riêng chỉ tạo thêm một quan hệ 1-1 dư thừa. Id của dòng chính là `batch_id`
được mọi phân hệ tham chiếu.

**B5. GENERATED column dùng làm gì? Vì sao không dùng FLOAT cho tiền?**
→ `total_amount = subtotal − discount_amount` do MySQL tự tính (GENERATED) — tránh sai
lệch do tầng ứng dụng tự cộng trừ. Tiền dùng DECIMAL vì FLOAT là nhị phân, không biểu
diễn chính xác số thập phân → sai số cộng dồn.

### Nhóm C — Đồng thời & bảo mật ⭐ (hay bị hỏi nhất)

**C1. Hai quầy cùng bán sản phẩm chỉ còn 1 đơn vị — chuyện gì xảy ra?**
→ Cả hai transaction cùng xin khóa bi quan (SELECT ... FOR UPDATE) trên sản phẩm đó.
Giao dịch đến sau phải CHỜ. Giao dịch trước trừ tồn thành công; giao dịch sau đọc tồn
đã là 0 → rollback, trả 409 kèm thông báo thiếu hàng. **Tồn không bao giờ được phép âm**
(BR-03) — em có unit test mô phỏng đúng kịch bản này.

**C2. Vì sao chọn khóa bi quan mà không phải khóa lạc quan?**
→ Khóa lạc quan hợp khi xung đột HIẾM. Ở POS, hai quầy bán cùng mặt hàng chạy là chuyện
thường xuyên → lạc quan sẽ liên tục thất bại và làm lại, tốn hơn. Bi quan cho chờ ngắn
và chắc chắn. Cái giá là giảm song song — chấp nhận được vì thời gian giữ khóa rất ngắn.

**C3. Khóa nhiều sản phẩm thì có deadlock không?**
→ Deadlock cần chu trình chờ vòng tròn: A giữ 1 chờ 2, B giữ 2 chờ 1. Em áp **thứ tự
khóa toàn cục**: mọi giao dịch (bán hàng lẫn chuyển kho) đều khóa sản phẩm theo **id
tăng dần** → không hình thành chu trình giữa các giao dịch tuân thủ thứ tự đó, giảm
mạnh nguy cơ deadlock; trong phạm vi kiểm thử đồng thời em không ghi nhận deadlock nào.

**C4. Mất mạng, thu ngân bấm thanh toán lần nữa — có ra 2 hóa đơn không?**
→ Không. Client sinh **idempotency key** duy nhất cho mỗi lượt thanh toán; cột này UNIQUE
trong CSDL. Request gửi lại cùng key → hệ thống trả về đúng hóa đơn đã tạo, không ghi mới.
Kể cả 2 request tới đồng thời, ràng buộc UNIQUE bảo đảm chỉ 1 bản ghi được chấp nhận.

**C5. Nhân viên cửa hàng A có xem được dữ liệu cửa hàng B không? Chống IDOR thế nào?**
→ Không. Mỗi request đi qua StoreContext (ThreadLocal): người dùng GẮN cửa hàng thì mọi
truy vấn tự lọc theo cửa hàng đó — **kể cả gửi header X-Store-Id giả cũng bị bỏ qua**
(chỉ ADMIN mới được chọn cửa hàng qua header). Truy cập theo id chéo cửa hàng bị
`assertSameStore` chặn. Em có 5 unit test riêng cho lớp cô lập này.

**C6. Nếu người ta bỏ qua giao diện, gửi thẳng request bằng Postman thì sao?**
→ Frontend chặn chỉ để trải nghiệm tốt. Mọi quy tắc được **kiểm tra lại ở phía máy chủ**:
JWT + @PreAuthorize theo vai trò, Bean Validation, StoreContext, và tầng cuối là ràng
buộc CSDL (CHECK, UNIQUE, FK). Request gửi thẳng vẫn phải qua đủ các lớp đó.

**C7. Mật khẩu và đăng nhập được bảo vệ ra sao?**
→ Mật khẩu chỉ lưu **băm BCrypt** — mỗi mật khẩu một salt ngẫu nhiên, hệ số chi phí làm
chậm chủ đích chống dò. Chống brute-force 2 lớp: sai 5 lần liên tiếp theo tài khoản →
khóa 60 giây (HTTP 429); 20 lần sai theo IP (rải nhiều tài khoản) → khóa IP 5 phút.
Đăng nhập thành công reset bộ đếm. JWT ký HMAC hạn 24 giờ; ở profile production, ứng dụng
**từ chối khởi động** nếu còn dùng secret mặc định.

**C8. Vì sao ADMIN làm được việc của MANAGER? Cấu hình ở đâu?**
→ Spring Role Hierarchy khai báo ADMIN > MANAGER > STAFF tại một nơi duy nhất —
quyền cấp trên bao trùm cấp dưới, không phải liệt kê lặp ở từng endpoint.

### Nhóm D — Kiểm thử

**D1. Em kiểm thử những gì? Vì sao tin được con số 72/72?**
→ Ba tầng. Unit: 72 test / 12 lớp chạy bằng `mvn test` — trong đó 56 test thuộc phạm vi
POS: bán hàng 9 (FEFO 2 lô, VAT, thiếu tồn 409, idempotency, đổi điểm, QR…), xác thực 4,
phân quyền & cô lập đa cửa hàng 12, báo cáo 4 (có test ghi file Excel thật rồi đọc lại
đối chiếu số), khuyến mãi & tiện ích 27; cộng 16 test phân hệ lương. Tích hợp: 13 kịch
bản end-to-end. UI: kịch bản demo 15 bước. Hội đồng có thể chạy lại `mvn test` ngay.

**D2. Test FEFO cụ thể thế nào?**
→ Dựng 2 lô cùng sản phẩm: lô 1 hạn gần còn 2, lô 2 hạn xa còn 5. Bán 3 đơn vị →
kỳ vọng phân bổ 2 từ lô 1 + 1 từ lô 2, đúng thứ tự hạn gần trước. Test khẳng định
bảng phân bổ ghi đúng lô, đúng số lượng.

**D3. Vì sao unit test lại mock repository — thế thì test được gì?**
→ Mock để cô lập LOGIC nghiệp vụ khỏi hạ tầng: kiểm chính xác thuật toán phân bổ, tính
thuế, quy tắc chặn — chạy nhanh, tái lập được, chỉ ra đúng chỗ sai. Phần "SQL có đúng
không" được phủ bởi tầng kiểm thử tích hợp chạy trên hệ thống thật với MySQL thật.

### Nhóm E — Kiến trúc & công nghệ

**E1. Vì sao chọn Spring Boot / React / MySQL?**
→ Spring Boot: Spring Data JPA giảm mạnh mã truy xuất, @Transactional + khóa bi quan
giải trực tiếp bài toán toàn vẹn đồng thời — yêu cầu sống còn của POS; Spring Security +
JWT là chuẩn công nghiệp. React: SPA thao tác bán hàng không tải lại trang. MySQL 8:
đủ CHECK, GENERATED, VIEW, khóa dòng InnoDB — các tính năng em khai thác sâu trong
thiết kế; quen thuộc, dễ triển khai cho cửa hàng nhỏ.

**E2. Cache dùng ở đâu? POS có đọc phải giá cũ không?**
→ Cache kết quả resolve giá hiệu lực theo (sản phẩm, cửa hàng) — dữ liệu đọc nhiều ghi ít.
Mọi đường ghi giá đều gắn @CacheEvict xóa cache ngay khi đổi → giảm thiểu nguy cơ đọc giá
cũ. Quan trọng hơn: giá hiệu lực **luôn do máy chủ quyết định** = COALESCE(giá riêng cửa
hàng, giá chuẩn) — không tin giá client gửi lên.

**E3. Bảng daily_sales_rollup để làm gì?**
→ Là bảng tổng hợp doanh thu – giá vốn – lợi nhuận gộp theo (cửa hàng, ngày), do job nền
chạy 00:30 mỗi đêm upsert. Mục đích: báo cáo dài hạn nhiều năm đọc bảng gọn này thay vì
quét toàn bộ lịch sử hóa đơn — đánh đổi kinh điển giữa tính tươi và tốc độ đọc.

**E4. Đối soát QR tự động hoạt động thế nào?**
→ Tạo hóa đơn QR → sinh mã VietQR với nội dung chuyển khoản duy nhất dạng "POS + mã" →
hóa đơn ở trạng thái PENDING_PAYMENT. Job nền poll API WEB2M đọc lịch sử giao dịch ngân
hàng, khớp nội dung chuyển khoản → tự chuyển COMPLETED. Quá hạn không thấy tiền →
EXPIRED. Demo dùng giả lập/xác nhận thủ công vì cần API key ngân hàng thật.

### Nhóm F — Câu hỏi "bẫy" & tình huống

**F1. "Sửa giá vốn sản phẩm hôm nay, báo cáo lợi nhuận tháng trước có đổi không?"**
→ KHÔNG. COGS của hóa đơn quá khứ chốt theo giá nhập của đúng lô đã bán
(invoice_item_batches → goods_receipt_items). Giá vốn hiện hành chỉ áp cho tương lai.
(Em có test riêng cho tình huống này.)

**F2. "Tồn kho có bao giờ lệch không? Chứng minh?"**
→ Thiết kế loại bỏ nguyên nhân chính (không lưu số dư trùng lặp — tồn luôn suy từ chứng
từ). Bảo toàn: on_shelf + in_warehouse + đã bán = tổng nhập của lô. Ngoài ra có job đối
soát định kỳ quét tồn âm/lệch điểm để phát hiện sớm. Em nói "loại bỏ nguyên nhân chính
và được kiểm chứng trong phạm vi thử nghiệm" — không tuyệt đối hóa.

**F3. "admin/123456 mà cũng đem nộp à?"**
→ Đó là tài khoản DEMO, chỉ tồn tại ở profile dev để hội đồng chấm nhanh. Ở profile
production: không seed dữ liệu demo, bắt buộc đặt mật khẩu admin qua biến môi trường,
và ứng dụng từ chối khởi động nếu JWT secret còn là giá trị mặc định.

**F4. "Nếu 1000 cửa hàng thì hệ thống này chịu nổi không?"**
→ Thiết kế dữ liệu đã sẵn sàng cho chuỗi (mọi bảng nghiệp vụ gắn store_id, cô lập theo
context), nhưng triển khai hiện là đơn nút — em ghi rõ ở hạn chế. Lộ trình scale: tách
đọc/ghi, cache phân tán, cân bằng tải nhiều instance API (stateless sẵn rồi), nhân bản
CSDL — nằm trong hướng phát triển. Với phân khúc mục tiêu (chuỗi mini vài chục cửa hàng),
cấu hình hiện tại đáp ứng tốt.

**F5. "Vì sao không dùng microservices?"**
→ Quy mô bài toán và đội ngũ (1 người) không justify chi phí vận hành của microservices
(network, consistency phân tán, deploy phức tạp). Monolith tách lớp rõ + API stateless
là lựa chọn đúng độ phức tạp; khi cần vẫn tách dần được vì ranh giới service đã rõ.

**F6. "Điểm khác biệt của đồ án em so với KiotViet/Sapo là gì?"**
→ Em không cạnh tranh sản phẩm thương mại — đồ án chứng tỏ năng lực xây dựng hệ thống
chuẩn nghiệp vụ bằng mã nguồn mở, tự chủ hoàn toàn dữ liệu và tùy biến sâu: truy vết
từng lô đến từng đơn vị bán ra, tồn hai tầng kho–kệ, đối soát QR ngân hàng nội địa —
những phần phần mềm đóng khó tùy biến.

**F7. "Trong quá trình làm, lỗi nào làm em nhớ nhất?"** *(câu hỏi mềm — chuẩn bị sẵn 1 chuyện)*
→ Gợi ý kể: bug hai quầy bán đồng thời lúc chưa có khóa — tồn âm; từ đó em hiểu vì sao
phải khóa bi quan và khóa theo thứ tự id để tránh deadlock. Kể ngắn 30 giây, kết bằng
bài học.

---

## PHẦN 4 — BẢNG SỐ LIỆU PHẢI THUỘC LÒNG

| Số liệu | Giá trị |
|---|---|
| Bảng CSDL | **28 bảng POS + 6 view** (thêm 5 bảng + 1 view phân hệ lương = 36 + 7 toàn hệ) |
| Chuẩn hóa | 3NF; 41 chỉ mục |
| Vai trò | 3: ADMIN ⊃ MANAGER ⊃ STAFF |
| Unit test | **56/56 POS — tổng 72/72** (12 lớp test, `mvn test`) |
| Kiểm thử tích hợp | 13 kịch bản; demo UI 15 bước |
| z theo ABC | A: 2,05 (~98%) · B: 1,65 (~95%) · C: 1,28 (~90%) |
| EOQ | √(2DS/H), trần 60 ngày bán |
| VAT | thuế = A×r/(100+r); mặc định 8% |
| Tích điểm | 1 điểm/10.000đ; 1 điểm = 1.000đ khi đổi |
| Brute-force | 5 lần sai → khóa 60s; 20 lần theo IP → khóa 5 phút |
| JWT | HMAC, hạn 24 giờ |
| Khảo sát | Super 138 Mini Mart, 138 Nguyễn Sơn, Phú Thọ Hòa, TP.HCM |
| Demo | admin/123456 (chỉ dev); docker compose up -d --build; localhost:8088 |

---

## PHẦN 5 — 10 ĐIỀU NHỚ KHI ĐỨNG TRƯỚC HỘI ĐỒNG

1. **Không đọc slide.** Slide là gạch đầu dòng, miệng nói phần "vì sao".
2. Thầy hỏi mà chưa nghe rõ → *"Dạ, em xin phép được hỏi lại cho rõ ý ạ"* — tốt hơn trả lời trượt đề.
3. Không biết thật → *"Dạ phần này em chưa tìm hiểu sâu, nhưng theo em hướng tiếp cận sẽ là…"* — trung thực + tư duy vẫn ăn điểm.
4. Câu hỏi về hạn chế → nhận ngay + nói hướng khắc phục đã viết trong báo cáo. KHÔNG chống chế.
5. Mọi khẳng định đi kèm bằng chứng: *"em có test cho tình huống này"*, *"phần này ở mục X của báo cáo"*.
6. Tránh từ tuyệt đối khi trả lời: dùng "giảm nguy cơ", "trong phạm vi kiểm thử" thay cho "không bao giờ", "không thể" (đúng tinh thần thầy đã góp ý).
7. Demo hỏng → chuyển video/PDF dự phòng NGAY, không loay hoay quá 30 giây.
8. Trả lời ngắn trước (2–3 câu trúng ý), hội đồng muốn nghe thêm sẽ hỏi tiếp.
9. Nhớ nhịp thở: bị hỏi dồn thì dừng 2 giây, nhấp nước, rồi trả lời — không ai trừ điểm 2 giây suy nghĩ.
10. Kết mỗi câu trả lời bằng ý chính, không lan man: *"…nên em chọn phương án này ạ."*

---

## PHẦN 6 — LỊCH ÔN GỢI Ý (3 BUỔI)

- **Buổi 1:** Đọc to Phần 1 hai lần theo slide thật, bấm giờ (mục tiêu ≤ 13 phút). Học bảng số liệu Phần 4.
- **Buổi 2:** Tự trả lời miệng toàn bộ Nhóm A + B + C (che đáp án). Chạy demo rút gọn Phần 2 hai lần trên máy thật.
- **Buổi 3 (trước hôm bảo vệ):** Nhóm D + E + F; nhờ bạn đóng vai hội đồng hỏi xoáy 15 phút; chạy checklist trong `ON_TAP_BAO_VE.md` (mvn test 72/72, npm run build, docker compose, phương án B video/PDF).
