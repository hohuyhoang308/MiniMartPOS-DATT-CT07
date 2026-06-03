# 10 — Nâng cấp & bổ sung (giải thích chi tiết)

Tài liệu này tổng hợp **toàn bộ tính năng mới và phần đã sửa** sau khi rà soát hệ thống theo
góc nhìn nghiệp vụ bán lẻ thực tế. Mỗi mục nêu: **vì sao cần** (thực tế), **làm gì** (bảng/Code),
và **kiểm chứng**. Tất cả đã build + test + commit.

---

## A. Mô hình tồn kho 2 tầng: KHO ↔ KỆ

**Thực tế:** Hàng nhập về nằm trong **kho**; muốn bán phải đưa ra **kệ** trưng bày. POS chỉ bán phần **trên kệ**.

- **Kệ vật lý** (`shelves`): mã (K01…), khu vực, **sức chứa** (capacity). Một LÔ chỉ nằm trên **một kệ**.
- **Lên kệ** (`shelf_transfers`): chuyển 1 LÔ từ kho lên 1 kệ — chọn lô **cận hạn trước (FIFO)**, tôn trọng sức chứa.
- **Về kho** (`shelf_returns`): lấy hàng từ kệ trả lại kho ("đặt lên thì có đặt xuống").
- Tồn suy ra qua view `v_batch_stock`:
  - `on_shelf` (tồn kệ) = (lên kệ − về kho − **trả hàng**) − đã bán
  - `in_warehouse` (tồn kho) = đã nhập − (lên kệ − về kho − **trả hàng**)
  - Bảo toàn: `on_shelf + in_warehouse = quantity_remaining`.

**Phân vai (đã sửa cho hợp lý):** thao tác kệ hằng ngày (xem/lên kệ/về kho) = **thu ngân**;
cấu hình kệ (thêm/sửa/xoá, sức chứa) = **quản lý**. Xem [08_Phan_quyen_va_chuc_nang](08_Phan_quyen_va_chuc_nang.md).

---

## B. Trả hàng / hoàn tiền (`sales_returns`, `sales_return_items`)

**Thực tế:** Khách trả 1 phần đơn mỗi ngày — không thể chỉ "hủy cả hóa đơn".

- Chứng từ trả tham chiếu **HĐ gốc** (giữ COMPLETED), **trả từng phần** theo dòng; phân bổ số trả vào đúng **lô** đã bán.
- Hàng trả **về KHO** (view trừ phần trả ở cả `sold` và `transferred` → kệ giữ nguyên, kho tăng).
- **Hoàn tiền theo số THỰC TRẢ**: nếu HĐ có giảm giá thì hoàn theo tỉ lệ `total/subtotal` (không hoàn dư).
- **Thu hồi điểm tích** đã thưởng cho phần hàng trả (theo tỉ lệ).
- Ghi **audit** `RETURN`. Chặn trả vượt số đã bán.

**Kiểm chứng:** bán 5 trả 2 → kho +2, kệ giữ nguyên, tổng tồn bảo toàn; HĐ 50k giảm 5k trả hết → hoàn **45k** (đúng).

---

## C. Thuế GTGT (VAT)

**Thực tế:** Hóa đơn bán lẻ VN tách thuế GTGT; giá niêm yết **đã gồm VAT**.

- `products.tax_rate` (0/8/10%), `invoices.tax_amount`.
- VAT gồm trong giá: `thuế = tiền × r/(100+r)`, **co giãn theo giảm giá**.
- Hiện trên **chi tiết hóa đơn** + **dòng VAT trên phiếu PDF**; form sản phẩm chọn thuế suất.

**Kiểm chứng:** bán 20.000đ @8% → VAT = 20000×8/108 = **1.481,48đ**.

---

## D. Đơn vị quy đổi thùng ↔ lon

**Thực tế:** Nhập theo **thùng/lốc**, bán theo **lon** — nếu cộng chung 2 đơn vị thì tồn sai.

- `products.pack_size` (1 thùng = N lon) + `pack_unit_id` (đơn vị mua).
- Tồn **luôn ở đơn vị cơ bản (lon)**; **quy đổi khi nhập kho** (số thùng × pack_size; giá/thùng → giá/lon) → **không đụng view tồn** (an toàn).

**Kiểm chứng:** đặt 1 lốc = 24 lon; nhập 2 lốc → kho **+48**.

---

## E. Nhật ký kiểm toán + lý do hủy HĐ (`audit_logs`)

**Thực tế:** Void-rồi-thủ-tiền-mặt là gian lận thu ngân số 1 — phải truy được "ai/khi nào/lý do".

- `audit_logs` ghi: actor, action, target, detail, thời gian (append-only).
- **Hủy HĐ bắt buộc nhập lý do** (≥3 ký tự), lưu `cancelled_by/at/reason`, ghi `CANCEL_INVOICE`.
- Ghi `CHANGE_PRICE` khi đổi giá, `RETURN` khi trả hàng. Xem nhật ký: `GET /api/audit` (ADMIN).

---

## F. Sổ cái điểm tích lũy (`loyalty_point_ledger`)

**Thực tế:** Điểm phải **truy vết & đối soát** được, không chỉ là một con số.

- Mỗi thay đổi điểm = 1 dòng (delta, reason, balance_after): `EARN`, `REDEEM`, `CANCEL_REVERSAL`, `RETURN`.

---

## G. Toàn vẹn dữ liệu (các bài toán logic)

| Vấn đề thực tế | Giải pháp |
|---|---|
| 2 quầy bán đơn vị cuối cùng cùng lúc → tồn âm | **Khóa bi quan** `PESSIMISTIC_WRITE` theo sản phẩm (id tăng dần, tránh deadlock) khi bán/lên kệ/về kho |
| Mất phản hồi mạng → bấm lại = 2 hóa đơn | **Idempotency key** (`invoices.idempotency_key` UNIQUE): cùng key → trả lại HĐ cũ |
| Bán nhầm **hàng quá HSD** | FIFO bán **loại lô `expiry < hôm nay`** (an toàn thực phẩm) |
| Lợi nhuận đổi khi đổi giá vốn | **COGS chính xác theo FIFO‑lô** (`import_price` của lô đã bán), không dùng `cost_price` hiện tại |
| Hủy HĐ đã trả hàng → tồn cộng dư | **Chặn hủy** HĐ đã có phiếu trả hàng |
| Báo cáo/dashboard tính cả hàng đã trả | **Trừ hàng trả** (RÒNG): doanh thu − tiền hoàn, lợi nhuận − lãi hàng trả |
| Hoàn tiền mặt không trừ quỹ ca | Quỹ ca dự kiến = đầu ca + tiền mặt bán − **tiền hoàn** |
| Tiền lẻ không tiêu được (VND không hào) | **Làm tròn** giảm giá % về đồng; **mask ô nhập tiền** (200.000, số nguyên) |

---

## H. Tầng giải thuật / mô hình toán

| Tính năng | Mô hình |
|---|---|
| **Đề xuất nhập hàng** | Tốc độ bán + **tồn an toàn theo mức phục vụ**: `SS = z·σ·√L` (z=1.65 ≈ 95%), điểm đặt = nhu cầu trong leadtime + SS; EOQ = √(2DS/H) |
| **Gợi ý mua kèm** | Market-basket theo **lift** = co(A,B)/n(B) (lọc support tối thiểu) — không bị hàng bán chạy lấn át |
| **Phân tích ABC/XYZ** | ABC theo doanh thu luỹ kế (Pareto 80/95%) × XYZ theo biến động nhu cầu (CV=σ/μ). Trang riêng `/abc-xyz` |

---

## I. Bảo mật & vận hành

- **Chống brute-force** đăng nhập (khóa 60s sau 5 lần sai → 429).
- **JWT secret**: fail-fast ở `prod` nếu dùng giá trị mặc định; cảnh báo ở dev.
- **Ẩn lỗi 500** (chỉ log phía server); chặn **IDOR** `/shifts/{id}` (chỉ quản lý).
- POS thao tác **bằng phím**: giữ focus ô quét sau khi thêm, **nhập thẳng số lượng** trên giỏ.

---

## J. Tổng kết bảng dữ liệu MỚI

| Bảng | Vai trò |
|---|---|
| `shelves`, `shelf_transfers`, `shelf_returns` | Kệ vật lý + lên kệ + về kho |
| `sales_returns`, `sales_return_items` | Trả hàng / hoàn tiền |
| `audit_logs` | Nhật ký kiểm toán |
| `loyalty_point_ledger` | Sổ cái điểm tích lũy |
| Cột thêm: `products.tax_rate/pack_size/pack_unit_id`, `invoices.tax_amount/idempotency_key/cancelled_*` | VAT, quy đổi, idempotency, audit hủy |

> Toàn bộ thay đổi schema đều **idempotent** (CREATE TABLE IF NOT EXISTS + migration kiểm tra `information_schema`),
> nên xóa DB hoặc nâng cấp DB cũ đều tự áp dụng khi khởi động backend.
