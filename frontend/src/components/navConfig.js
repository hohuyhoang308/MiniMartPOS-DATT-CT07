/** Cấu hình điều hướng dùng chung cho Sidebar + breadcrumb Topbar. */
export const NAV_SECTIONS = [
  {
    title: 'Bán hàng',
    items: [
      { to: '/dashboard', icon: 'bi-grid-1x2-fill', label: 'Tổng quan', roles: ['ADMIN', 'MANAGER'] },
      { to: '/pos', icon: 'bi-bag-check-fill', label: 'Bán hàng (POS)', roles: ['ADMIN', 'MANAGER', 'CASHIER'] },
      { to: '/shelf', icon: 'bi-arrow-up-square-fill', label: 'Lên kệ', roles: ['ADMIN', 'MANAGER', 'CASHIER'] },
      { to: '/invoices', icon: 'bi-receipt', label: 'Hóa đơn', roles: ['ADMIN', 'MANAGER', 'CASHIER'] },
      { to: '/shifts', icon: 'bi-clock-history', label: 'Quản lý ca', roles: ['ADMIN', 'MANAGER'] },
    ],
  },
  {
    title: 'Kho hàng',
    items: [
      { to: '/products', icon: 'bi-box-seam-fill', label: 'Sản phẩm', roles: ['ADMIN', 'MANAGER'] },
      { to: '/catalog', icon: 'bi-tags-fill', label: 'Danh mục & Đơn vị', roles: ['ADMIN', 'MANAGER'] },
      { to: '/suppliers', icon: 'bi-truck', label: 'Nhà cung cấp', roles: ['ADMIN', 'MANAGER'] },
      { to: '/receipts', icon: 'bi-box-arrow-in-down', label: 'Nhập kho', roles: ['ADMIN', 'MANAGER'] },
      { to: '/inventory', icon: 'bi-clipboard2-pulse-fill', label: 'Tồn kho & cảnh báo', roles: ['ADMIN', 'MANAGER'] },
      { to: '/shelves', icon: 'bi-grid-3x3-gap-fill', label: 'Quản lý kệ', roles: ['ADMIN', 'MANAGER'] },
    ],
  },
  {
    title: 'Kinh doanh',
    items: [
      { to: '/customers', icon: 'bi-people-fill', label: 'Khách hàng', roles: ['ADMIN', 'MANAGER', 'CASHIER'] },
      { to: '/promotions', icon: 'bi-percent', label: 'Khuyến mãi', roles: ['ADMIN', 'MANAGER'] },
      { to: '/reports', icon: 'bi-bar-chart-line-fill', label: 'Báo cáo', roles: ['ADMIN', 'MANAGER'] },
    ],
  },
  {
    title: 'Hệ thống',
    items: [
      { to: '/users', icon: 'bi-person-badge-fill', label: 'Tài khoản', roles: ['ADMIN'] },
      { to: '/settings', icon: 'bi-gear-fill', label: 'Cấu hình', roles: ['ADMIN'] },
    ],
  },
]

export const ALL_NAV = NAV_SECTIONS.flatMap((s) => s.items)

export function findNav(pathname) {
  return ALL_NAV.find((i) => pathname.startsWith(i.to))
}
