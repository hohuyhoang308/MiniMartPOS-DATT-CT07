# Frontend — MiniMart POS (React + Vite + Bun)

Giao diện SPA cho hệ thống POS cửa hàng tiện lợi, gọi REST API của backend Spring Boot.

## Công nghệ
- **React 18** + **Vite 5**, chạy bằng **Bun** (HMR / hot-reload sẵn)
- **React Router 6**, **Axios** (interceptor đính JWT + xử lý 401)
- **React-Bootstrap** + design system tùy biến (`src/index.css`)
- **Recharts** (biểu đồ doanh thu, tồn kho)
- Fonts: **Plus Jakarta Sans** (display) + **Be Vietnam Pro** (body) — hỗ trợ tiếng Việt

## Chạy
```bash
bun install
bun run dev        # http://localhost:5173 (proxy /api → http://localhost:8080)
bun run build      # đóng gói tĩnh vào dist/
```
> Cần backend chạy ở cổng 8080. Đăng nhập demo: `admin` / `manager` / `cashier` — mật khẩu `123456`.

## Cấu trúc
```
src/
├── api/            # client axios + module gọi API (auth, catalog, sales, misc)
├── context/        # AuthContext, CartContext, ToastContext
├── routes/         # PrivateRoute (phân quyền theo vai trò)
├── components/     # Layout, Sidebar, navConfig + ui/ (PageHeader, StatCard, StatusPill,
│                   #   ConfirmModal, EmptyState, Loading…)
├── pages/
│   ├── auth/       # Login
│   ├── dashboard/  # Tổng quan (stat + area chart + top SP)
│   ├── pos/        # Bán hàng POS (quét mã, giỏ, thanh toán CASH/QR, in PDF)
│   ├── catalog/    # Products, Categories, Units, Suppliers
│   ├── inventory/  # Inventory (chart cảnh báo), GoodsReceipts (nhập kho)
│   ├── crm/        # Customers (+lịch sử), Promotions
│   ├── invoices/   # Hóa đơn (lọc, chi tiết, hủy, PDF)
│   ├── reports/    # Báo cáo doanh thu + theo ca + xuất Excel
│   └── admin/      # Users, Settings (cửa hàng + VietQR + WEB2M + Telegram)
└── utils/          # format tiền/ngày
```

## Thiết kế
Hướng "Fresh Retail Intelligence": nền sáng sạch, sidebar tối (ink) sang trọng, nhấn **emerald**,
thẻ bo tròn mềm, biểu đồ gradient, micro-interaction tinh tế. Toàn bộ phân quyền FE đồng bộ với
`@PreAuthorize` ở backend (menu & route lọc theo vai trò).

## Ảnh chụp giao diện
Thư mục `shots/` chứa ảnh chụp các màn hình (login, dashboard, sản phẩm, tồn kho, POS, hóa đơn,
báo cáo, khuyến mãi, cấu hình) — tạo lại bằng `node shoot.mjs` (cần backend + dev server đang chạy).
Có thể dùng các ảnh này để chèn vào báo cáo đồ án.
