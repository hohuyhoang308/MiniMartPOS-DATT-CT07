# ĐÁNH GIÁ ỔN ĐỊNH & HOÀN THIỆN — NÂNG CẤP ĐA CỬA HÀNG

> Tài liệu ghi lại quá trình **nâng cấp hệ thống từ một cửa hàng đơn lẻ → quản lý CHUỖI đa cửa hàng**,
> rà soát cấp senior, các lỗi đã sửa và kết quả kiểm thử. Đồng bộ với [`BAO_CAO_DO_AN.md`](BAO_CAO_DO_AN.md).

---

## 1. Mô hình phân quyền mới (phân tầng từ cao xuống thấp)

| Vai trò | Phạm vi | Quyền chính |
|---|---|---|
| **ADMIN** | TOÀN CHUỖI (không gắn cửa hàng) | Quản lý chuỗi cửa hàng, tài khoản (gán cửa hàng), **catalog/giá/khuyến mãi/NCC dùng chung (HQ)**, cấu hình từng cửa hàng, báo cáo gộp toàn chuỗi, nhật ký |
| **MANAGER** | 1 cửa hàng (N/cửa hàng) | Vận hành: nhập kho, kệ, tồn, ca, POS, hủy hóa đơn, báo cáo cửa hàng |
| **STAFF** | 1 cửa hàng (N/cửa hàng) | POS, hóa đơn của ca mình, lên/về kệ, khách hàng |

Phân tầng **ADMIN ⊃ MANAGER ⊃ STAFF** (Spring Role Hierarchy) — quyền cao tự động bao hàm quyền thấp.
(Đã **gộp** vai trò "quản trị chuỗi" vào ADMIN, **đổi tên** "Thu ngân/CASHIER" → "Nhân viên/STAFF".)

## 2. Nguyên tắc cô lập dữ liệu đa cửa hàng

- **Riêng từng cửa hàng:** tồn kho, kệ, ca, hóa đơn, phiếu nhập, cấu hình (ngân hàng/VietQR/WEB2M/Telegram).
- **Dùng chung toàn chuỗi:** sản phẩm, danh mục, đơn vị, NCC, khuyến mãi, khách hàng, sổ điểm — **chỉ ADMIN sửa**.
- **Cô lập:** FIFO bán chỉ rút lô của cửa hàng đang bán; chặn truy cập chéo cửa hàng theo id (`assertSameStore`);
  cửa hàng đóng (INACTIVE) ⇒ nhân viên cửa hàng đó không đăng nhập được.

## 3. Rà soát cấp senior — các lỗi đã phát hiện & sửa

| Mức | Vấn đề | Đã sửa |
|---|---|---|
| Nghiêm trọng | Gợi ý "mua kèm" gộp dữ liệu mọi cửa hàng (rò rỉ chéo) | Lọc theo `storeId` |
| Nghiêm trọng | `/payments/{id}/confirm` & `/status` không chặn chéo cửa hàng (IDOR) | Thêm `assertSameStore` |
| Cao | Migration để `store_id` nullable sau backfill | Siết `NOT NULL` cho bảng vận hành |
| Cao | Gợi ý tiền đầu ca lấy két cửa hàng khác | Lọc theo cửa hàng |
| Cao | Cửa hàng đóng nhưng nhân viên vẫn đăng nhập | Xác thực kiểm tra trạng thái cửa hàng |
| Cao | Bán khi ca chưa gắn cửa hàng (NPE) | Guard + chốt từ ca |
| Trung bình | `GoodsReceipt.findAll` lọc trong RAM; nhân viên lọc HĐ sau trần 500; idempotency chéo cửa hàng | Lọc tại CSDL; đẩy điều kiện vào truy vấn; `assertSameStore` |
| Cô lập nghiệp vụ | Quản lý cửa hàng sửa/xoá catalog dùng chung của cả chuỗi | Catalog/giá/KM/NCC: **mọi thao tác ghi = ADMIN only** |

## 4. CSDL — toàn vẹn & tối ưu

- **Toàn vẹn:** đủ FK (store_id → stores); `NOT NULL` store cho bảng vận hành; UNIQUE đúng phạm vi
  (mã kệ duy nhất *trong* cửa hàng, Telegram theo `(cửa hàng, chat)`, `invoices.code/idempotency_key`,
  `barcode`, `phone`); `store_config` 1–1 với cửa hàng; CHECK số lượng/giá ≥ 0.
- **Tồn = view suy ra** (không cột tồn dư) → không lệch; `total_amount`/`subtotal` là GENERATED; job nền đối soát
  tồn âm & sổ điểm.
- **Tối ưu:** index `(store_id, status, created_at)` trên `invoices`; `idx_*_store` cho goods_receipts/work_shifts/shelves;
  view tồn gộp sẵn (pre-aggregate).

## 5. Kết quả kiểm thử (hệ thống đang chạy)

- **Unit test:** 27/27 PASS, compile sạch (BE) + build sạch (FE).
- **End-to-end (API thật):**
  - Đăng nhập đúng 3 vai trò + cửa hàng; phân quyền 403/200 đúng kỳ vọng.
  - POS: nhân viên bán → tồn −1 → quản lý hủy → tồn hoàn; nhân viên không hủy được (403).
  - Cô lập: CH2 tồn=0 (không lẫn CH1); quản lý CH1 không đọc được ca CH2 (IDOR 400).
  - Cấu hình riêng từng cửa hàng (CH1 MB Bank ≠ CH2 Vietcombank).
  - Đóng/mở cửa hàng ↔ đăng nhập: 9/9 PASS (đóng → 401, mở → 200, không đụng nhầm cửa hàng khác).
  - Catalog HQ-only: manager xoá product/supplier/promotion/unit → 403; vẫn đọc được để nhập/bán.

## 6. Việc còn lại / hướng mở rộng (chưa critical)

- Cảnh báo khi gán tài khoản vào cửa hàng đã đóng; khi đóng cửa hàng còn ca mở.
- Điều chuyển hàng nội bộ giữa các cửa hàng (kho trung tâm + chứng từ xuất/nhận).
- So sánh hiệu quả các cửa hàng trên dashboard; giá bán theo cửa hàng/vùng.
</content>
