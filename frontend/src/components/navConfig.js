/**
 * Cấu hình điều hướng (Sidebar + breadcrumb Topbar).
 * Sắp xếp theo NGHIỆP VỤ — mỗi nhóm một mục đích rõ ràng, thứ tự từ dùng nhiều → cấu hình:
 *   1) Tổng quan   — phân tích/báo cáo (quản lý xem)
 *   2) Bán hàng    — thao tác hằng ngày tại quầy (thu ngân)
 *   3) Hàng hóa    — danh mục sản phẩm & giá/khuyến mãi
 *   4) Kho & nhập  — nhà cung cấp, nhập kho, tồn kho, kệ
 *   5) Hệ thống    — kiểm toán, tài khoản, cấu hình (chỉ chủ cửa hàng)
 */
export const NAV_SECTIONS = [
  {
    title: 'Tổng quan',
    items: [
      { to: '/chain', icon: 'bi-diagram-3-fill', label: 'So sánh chi nhánh', roles: ['ADMIN'] },
      { to: '/dashboard', icon: 'bi-grid-1x2-fill', label: 'Tổng quan', roles: ['ADMIN', 'MANAGER'] },
      { to: '/reports', icon: 'bi-bar-chart-line-fill', label: 'Báo cáo & phân tích', roles: ['ADMIN', 'MANAGER'] },
    ],
  },
  {
    title: 'Bán hàng',
    items: [
      { to: '/pos', icon: 'bi-bag-check-fill', label: 'Bán hàng (POS)', roles: ['MANAGER', 'STAFF'] },
      { to: '/invoices', icon: 'bi-receipt', label: 'Hóa đơn', roles: ['ADMIN', 'MANAGER', 'STAFF'] },
      { to: '/shelf', icon: 'bi-arrow-up-square-fill', label: 'Lên kệ / lấy về kho', roles: ['MANAGER', 'STAFF'] },
      { to: '/customers', icon: 'bi-people-fill', label: 'Khách hàng', roles: ['ADMIN', 'MANAGER', 'STAFF'] },
      { to: '/shifts', icon: 'bi-clock-history', label: 'Ca làm việc', roles: ['ADMIN', 'MANAGER'] },
    ],
  },
  {
    title: 'Hàng hóa & giá (HQ — toàn chuỗi)',
    items: [
      { to: '/products', icon: 'bi-box-seam-fill', label: 'Sản phẩm', roles: ['ADMIN'] },
      { to: '/catalog', icon: 'bi-tags-fill', label: 'Danh mục & Đơn vị', roles: ['ADMIN'] },
      { to: '/promotions', icon: 'bi-percent', label: 'Khuyến mãi', roles: ['ADMIN'] },
      { to: '/suppliers', icon: 'bi-truck', label: 'Nhà cung cấp', roles: ['ADMIN'] },
    ],
  },
  {
    title: 'Kho & nhập hàng',
    items: [
      { to: '/receipts', icon: 'bi-box-arrow-in-down', label: 'Nhập kho', roles: ['ADMIN', 'MANAGER'] },
      { to: '/inventory', icon: 'bi-clipboard2-pulse-fill', label: 'Tồn kho & xuất hủy', roles: ['ADMIN', 'MANAGER'] },
      { to: '/shelves', icon: 'bi-grid-3x3-gap-fill', label: 'Cấu hình kệ', roles: ['MANAGER'] },
    ],
  },
  {
    title: 'Hệ thống',
    items: [
      { to: '/stores', icon: 'bi-shop', label: 'Cửa hàng (chuỗi)', roles: ['ADMIN'] },
      { to: '/users', icon: 'bi-person-badge-fill', label: 'Tài khoản', roles: ['ADMIN', 'MANAGER'] },
      { to: '/audit', icon: 'bi-shield-check', label: 'Nhật ký thao tác', roles: ['ADMIN', 'MANAGER'] },
      { to: '/settings', icon: 'bi-gear-fill', label: 'Cấu hình', roles: ['ADMIN'] },
    ],
  },
]

export const ALL_NAV = NAV_SECTIONS.flatMap((s) => s.items)

export function findNav(pathname) {
  // Chọn prefix KHỚP DÀI NHẤT để route con (vd /reports/employees) không bị /reports "nuốt".
  return ALL_NAV.filter((i) => pathname.startsWith(i.to))
    .sort((a, b) => b.to.length - a.to.length)[0]
}
