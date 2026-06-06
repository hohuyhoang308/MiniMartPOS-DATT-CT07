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
      { to: '/dashboard', icon: 'bi-grid-1x2-fill', label: 'Tổng quan', roles: ['ADMIN', 'MANAGER'] },
      { to: '/reports', icon: 'bi-bar-chart-line-fill', label: 'Báo cáo doanh thu', roles: ['ADMIN', 'MANAGER'] },
      { to: '/abc-xyz', icon: 'bi-bar-chart-steps', label: 'Phân loại mặt hàng (ABC/XYZ)', roles: ['ADMIN', 'MANAGER'] },
    ],
  },
  {
    title: 'Bán hàng',
    items: [
      { to: '/pos', icon: 'bi-bag-check-fill', label: 'Bán hàng (POS)', roles: ['ADMIN', 'MANAGER', 'CASHIER'] },
      { to: '/invoices', icon: 'bi-receipt', label: 'Hóa đơn', roles: ['ADMIN', 'MANAGER', 'CASHIER'] },
      { to: '/shelf', icon: 'bi-arrow-up-square-fill', label: 'Lên kệ / lấy về kho', roles: ['ADMIN', 'MANAGER', 'CASHIER'] },
      { to: '/customers', icon: 'bi-people-fill', label: 'Khách hàng', roles: ['ADMIN', 'MANAGER', 'CASHIER'] },
      { to: '/shifts', icon: 'bi-clock-history', label: 'Ca làm việc', roles: ['ADMIN', 'MANAGER'] },
    ],
  },
  {
    title: 'Hàng hóa & giá',
    items: [
      { to: '/products', icon: 'bi-box-seam-fill', label: 'Sản phẩm', roles: ['ADMIN', 'MANAGER'] },
      { to: '/catalog', icon: 'bi-tags-fill', label: 'Danh mục & Đơn vị', roles: ['ADMIN', 'MANAGER'] },
      { to: '/promotions', icon: 'bi-percent', label: 'Khuyến mãi', roles: ['ADMIN', 'MANAGER'] },
    ],
  },
  {
    title: 'Kho & nhập hàng',
    items: [
      { to: '/suppliers', icon: 'bi-truck', label: 'Nhà cung cấp', roles: ['ADMIN', 'MANAGER'] },
      { to: '/receipts', icon: 'bi-box-arrow-in-down', label: 'Nhập kho', roles: ['ADMIN', 'MANAGER'] },
      { to: '/inventory', icon: 'bi-clipboard2-pulse-fill', label: 'Tồn kho & hàng sắp hết', roles: ['ADMIN', 'MANAGER'] },
      { to: '/shelves', icon: 'bi-grid-3x3-gap-fill', label: 'Cấu hình kệ', roles: ['ADMIN', 'MANAGER'] },
    ],
  },
  {
    title: 'Hệ thống',
    items: [
      { to: '/audit', icon: 'bi-shield-check', label: 'Nhật ký thao tác', roles: ['ADMIN'] },
      { to: '/users', icon: 'bi-person-badge-fill', label: 'Tài khoản', roles: ['ADMIN'] },
      { to: '/settings', icon: 'bi-gear-fill', label: 'Cấu hình', roles: ['ADMIN'] },
    ],
  },
]

export const ALL_NAV = NAV_SECTIONS.flatMap((s) => s.items)

export function findNav(pathname) {
  return ALL_NAV.find((i) => pathname.startsWith(i.to))
}
