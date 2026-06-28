# THIẾT KẾ MODULE LƯƠNG & BẢNG CÔNG (Payroll & Timekeeping)

> Mở rộng từ **Quản lý ca làm việc** đã có. Module này biến dữ liệu ca thành **bảng công** rồi
> **tính lương** cho từng nhân viên theo từng chi nhánh, từng tháng — đúng chuẩn một hệ thống POS
> đa chuỗi chuyên nghiệp. Tài liệu này mô tả nghiệp vụ, dữ liệu, công thức và phân quyền; phần
> hiện thực (entity/service/controller/UI) bám đúng thiết kế ở đây.

---

## 1. Vì sao làm được — "base tính công" đã có sẵn

Hệ thống đã có bảng `work_shifts` (Ca làm việc). Mỗi ca ghi:

| Cột | Ý nghĩa dùng cho công |
|-----|----------------------|
| `user_id`   | Nhân viên làm ca |
| `store_id`  | Chi nhánh làm ca (đa chuỗi) |
| `opened_at` | Giờ **vào ca** (check-in) |
| `closed_at` | Giờ **kết ca** (check-out) — NULL khi đang mở |
| `status`    | `OPEN` / `CLOSED` |

→ Với một ca **đã đóng**, `closed_at − opened_at` chính là **số giờ công** của ca đó.
Module lương **không tạo cơ chế chấm công mới**: nó **cộng dồn giờ công từ các ca đã đóng**
theo (nhân viên × chi nhánh × tháng). Ca đang mở (`OPEN`) **không** được tính (chưa chốt giờ ra).

Đây là lý do "có quản lý ca thì có tính lương": ca = chấm công, lương = công × đơn giá.

---

## 2. Mô hình nghiệp vụ

```
                ┌─────────────────────┐
   work_shifts  │  Ca đã đóng (CLOSED)│  ──► nguồn GIỜ CÔNG (không sửa)
                └─────────┬───────────┘
                          │  cộng dồn theo (user, store, tháng)
                          ▼
employee_pay_profiles ─► PAYROLL  ◄─ payslip_adjustments (thưởng/phạt/tạm ứng)
 (đơn giá, loại lương)     │
                          ▼
                payroll_periods (kỳ lương: chi nhánh × tháng)
                          │  1 — N
                          ▼
                     payslips (phiếu lương: 1 / nhân viên / kỳ)
```

### Vòng đời một kỳ lương

```
            tính / tính lại                khóa kỳ              chi lương
  (chưa có) ───────────────►  DRAFT  ──────────────►  LOCKED  ──────────────►  PAID
                              ▲   │ (recompute từ ca)         (số liệu đóng băng)
                              └───┘  thêm/sửa thưởng-phạt
```

- **DRAFT**: được **tính lại** (recompute) bất kỳ lúc nào — đọc lại giờ công từ ca, tính lại
  lương gốc/tăng ca; được **thêm/xóa** dòng thưởng/phạt. Dùng khi tháng chưa chốt.
- **LOCKED**: **đóng băng** toàn bộ số liệu (snapshot). Không recompute, không sửa thưởng/phạt.
  Hủy ca sau này **không** làm lệch phiếu lương đã khóa (giống snapshot đối soát quỹ ở đóng ca).
- **PAID**: đã chi lương. Trạng thái cuối, chỉ để lưu vết.

---

## 3. Cấu trúc dữ liệu (4 bảng mới)

### 3.1 `employee_pay_profiles` — Cấu hình lương / nhân viên
Một dòng cho mỗi nhân viên (cấu hình **hiện hành**). Đổi mức lương được ghi `audit_logs`.

| Cột | Kiểu | Ghi chú |
|-----|------|--------|
| `user_id` (UNIQUE) | BIGINT | Nhân viên |
| `pay_type` | ENUM(`HOURLY`,`MONTHLY`) | Lương theo **giờ** hay theo **tháng** |
| `base_rate` | DECIMAL(12,2) | `HOURLY`: đ/giờ · `MONTHLY`: đ/tháng |
| `standard_monthly_hours` | DECIMAL(6,2) | Công chuẩn/tháng (mặc định **208** = 26 ngày × 8h) |
| `ot_multiplier` | DECIMAL(4,2) | Hệ số tăng ca (mặc định **1.5**) |
| `monthly_allowance` | DECIMAL(12,2) | Phụ cấp cố định/tháng (ăn trưa, xăng xe…) |
| `updated_by`, `updated_at` | | Người & thời điểm sửa gần nhất |

### 3.2 `payroll_periods` — Kỳ lương (chi nhánh × tháng)
UNIQUE `(store_id, period_month)` — mỗi chi nhánh chỉ một kỳ/tháng.

| Cột | Kiểu | Ghi chú |
|-----|------|--------|
| `store_id` | BIGINT | Chi nhánh (đa chuỗi) |
| `period_month` | CHAR(7) | `'YYYY-MM'` |
| `status` | ENUM(`DRAFT`,`LOCKED`,`PAID`) | Vòng đời ở §2 |
| `created_by`, `created_at`, `locked_at`, `paid_at` | | Lưu vết |

### 3.3 `payslips` — Phiếu lương (1 / nhân viên / kỳ)
UNIQUE `(period_id, user_id)`. **Snapshot** mọi số liệu để khóa kỳ là cố định.

| Nhóm | Cột |
|------|-----|
| Tham chiếu | `period_id`, `user_id` |
| Snapshot cấu hình | `pay_type`, `base_rate`, `standard_hours` |
| Công | `worked_hours`, `regular_hours`, `ot_hours`, `shift_count` |
| Tiền | `regular_pay`, `ot_pay`, `allowance`, `gross_pay`, `total_bonus`, `total_deduction`, `net_pay` |

### 3.4 `payslip_adjustments` — Thưởng / phạt / tạm ứng (cộng/trừ net)

| Cột | Kiểu | Ghi chú |
|-----|------|--------|
| `payslip_id` | BIGINT | Phiếu lương |
| `type` | ENUM(`BONUS`,`DEDUCTION`) | Cộng / trừ |
| `amount` | DECIMAL(14,2) | > 0 |
| `reason` | VARCHAR(255) | Bắt buộc (vd "Thưởng doanh số", "Tạm ứng") |
| `created_by`, `created_at` | | Lưu vết |

---

## 4. Công thức tính lương

Cho một nhân viên trong kỳ (chi nhánh × tháng):

```
worked_hours  = Σ (closed_at − opened_at) các ca CLOSED có opened_at thuộc tháng & đúng chi nhánh
standard      = standard_monthly_hours (từ pay_profile)
regular_hours = min(worked_hours, standard)
ot_hours      = max(0, worked_hours − standard)
```

**Lương gốc & tăng ca** theo loại lương:

- `HOURLY` (lương giờ):
  ```
  regular_pay = regular_hours × base_rate
  ot_pay      = ot_hours × base_rate × ot_multiplier
  ```

- `MONTHLY` (lương tháng — trả **theo công** đã làm):
  ```
  hourly_equiv = base_rate / standard            (đơn giá giờ quy đổi)
  regular_pay  = base_rate × (regular_hours / standard)   ← đi thiếu công thì trừ theo tỷ lệ
  ot_pay       = ot_hours × hourly_equiv × ot_multiplier
  ```
  → Làm **đủ** công: `regular_pay = base_rate` (đủ lương tháng). Làm **thiếu**: bị trừ theo tỷ lệ
  công. Làm **dư** (vượt công chuẩn): phần dư tính tăng ca.

**Tổng hợp** (cho cả hai loại):
```
gross_pay       = regular_pay + ot_pay + allowance
total_bonus     = Σ amount  (type = BONUS)
total_deduction = Σ amount  (type = DEDUCTION)
net_pay         = gross_pay + total_bonus − total_deduction        (thực lĩnh)
```

Mọi tiền làm tròn đến **đồng** (HALF_UP) khi snapshot.

---

## 5. Ai được tạo phiếu lương trong một kỳ?

Khi **tính/tính lại** kỳ của một chi nhánh+tháng: hệ thống lấy **tập nhân viên có ≥1 ca CLOSED**
trong chi nhánh+tháng đó (đúng người đã đi làm — đã chấm công). Mỗi người sinh đúng một phiếu.
Nhân viên chưa có `pay_profile` ⇒ coi như `base_rate = 0` (phiếu vẫn hiện đủ giờ công, lương 0 để
quản lý thấy mà cấu hình mức lương). Recompute **giữ nguyên** các dòng thưởng/phạt đã nhập, chỉ
tính lại phần lương gốc/tăng ca và cộng lại `net`.

---

## 6. Phân quyền (đa chuỗi)

| Vai trò | Quyền |
|---------|------|
| **ADMIN** (toàn chuỗi) | Xem/tạo/khóa/chi mọi kỳ lương của **mọi** chi nhánh; cấu hình lương **mọi** nhân viên. Theo chi nhánh đang chọn (`X-Store-Id`); không chọn = xem toàn chuỗi. |
| **MANAGER** (một cửa hàng) | Chỉ kỳ lương & nhân viên **STAFF** thuộc **chính cửa hàng mình** (BE tự lọc, chặn chéo chi nhánh — giống `UserService`). |
| **STAFF** | Chỉ xem **phiếu lương của chính mình** ở các kỳ đã `LOCKED`/`PAID` ("Phiếu lương của tôi"). |

Thao tác **ghi** (tính/khóa/chi/sửa cấu hình) bắt buộc đã chọn chi nhánh (`StoreContext.requireStoreId`).
Mọi thao tác trạng thái kỳ và đổi mức lương đều ghi `audit_logs`.

---

## 7. API

| Method & path | Quyền | Mục đích |
|---------------|-------|---------|
| `GET  /api/payroll/pay-profiles` | ADMIN, MANAGER | Danh sách cấu hình lương nhân viên (theo chi nhánh) |
| `PUT  /api/payroll/pay-profiles/{userId}` | ADMIN, MANAGER | Đặt/cập nhật cấu hình lương một nhân viên |
| `GET  /api/payroll/periods` | ADMIN, MANAGER | Danh sách kỳ lương (theo chi nhánh) |
| `POST /api/payroll/periods/compute` | ADMIN, MANAGER | Tạo **hoặc** tính lại kỳ `{month}` (DRAFT) từ ca |
| `GET  /api/payroll/periods/{id}` | ADMIN, MANAGER | Chi tiết kỳ + danh sách phiếu lương |
| `POST /api/payroll/periods/{id}/lock` | ADMIN, MANAGER | Khóa kỳ (đóng băng) |
| `POST /api/payroll/periods/{id}/pay` | ADMIN, MANAGER | Đánh dấu đã chi lương |
| `POST /api/payroll/payslips/{id}/adjustments` | ADMIN, MANAGER | Thêm dòng thưởng/phạt |
| `DELETE /api/payroll/adjustments/{id}` | ADMIN, MANAGER | Xóa dòng thưởng/phạt |
| `GET  /api/payroll/my-payslips` | mọi vai trò | Phiếu lương của chính mình (kỳ LOCKED/PAID) |

---

## 8. Giao diện

- **Trang "Lương & công"** (ADMIN/MANAGER) — hub 2 tab:
  - *Kỳ lương*: chọn tháng → tính/tính lại; bảng phiếu lương (công, giờ thường/tăng ca, lương gốc,
    tăng ca, phụ cấp, thưởng/phạt, **thực lĩnh**); khóa kỳ & chi lương; thêm thưởng/phạt cho từng phiếu.
  - *Cấu hình lương*: bảng nhân viên + đặt loại lương/đơn giá/phụ cấp.
- **Trang "Phiếu lương của tôi"** (STAFF) — danh sách phiếu lương đã khóa của bản thân, có giải thích
  công + cách tính.

---

## 8b. Chấm công thủ công & nghỉ phép (bảng `attendance_entries`)

Công **không chỉ** đến từ ca thu ngân. Một số nhân viên (kho, bảo vệ) không mở ca tiền; đôi khi cần
**sửa/bổ sung công** hoặc ghi **nghỉ phép**. Bảng `attendance_entries` bổ sung công NGOÀI ca:

| Cột | Ý nghĩa |
|-----|--------|
| `user_id`, `store_id`, `work_date` | Nhân viên · chi nhánh · ngày |
| `type` | `WORK` (giờ làm ngoài ca) · `LEAVE_PAID` (nghỉ phép có lương) · `LEAVE_UNPAID` (nghỉ không lương) |
| `hours` | Số giờ (>0, ≤24) |
| `reason` | Lý do |

**Tích hợp payroll:** khi tính kỳ, giờ công mỗi nhân viên =
`Σ giờ ca đã đóng + Σ giờ (WORK + LEAVE_PAID)`. `LEAVE_UNPAID` chỉ ghi nhận, không tính lương.
Người **chỉ** có chấm công (không ca) vẫn sinh phiếu lương. Quản lý nhập ở tab **Bảng công**.
API: `GET/POST /api/payroll/attendance`, `DELETE /api/payroll/attendance/{id}` (ADMIN/MANAGER, store-scoped).

## 8c. Xuất Excel & in PDF

- **Bảng lương cả kỳ → Excel**: `GET /api/payroll/periods/{id}/export` (Apache POI) — 1 dòng/nhân viên +
  dòng tổng. ADMIN/MANAGER, chống IDOR theo chi nhánh.
- **Phiếu lương cá nhân → PDF**: `GET /api/payroll/payslips/{id}/pdf` (OpenPDF, khổ A5, font Unicode VN).
  Mọi vai trò gọi được nhưng **STAFF chỉ in phiếu ĐÃ CHỐT của chính mình** (service tự chốt phạm vi).

## 9. Đồng bộ & toàn vẹn

- Hai file schema phải khớp: `backend/src/main/resources/db/schema.sql` (app tự dựng) **và**
  `sql/schema.sql` (bản cài tay/tài liệu).
- Snapshot trên `payslips` đảm bảo kỳ đã khóa **không đổi** dù ca/hủy hóa đơn thay đổi về sau.
- Ràng buộc DB: `UNIQUE(user_id)` cho profile, `UNIQUE(store_id, period_month)` cho kỳ,
  `UNIQUE(period_id, user_id)` cho phiếu, `CHECK(amount > 0)` cho thưởng/phạt.
</content>
</invoke>
