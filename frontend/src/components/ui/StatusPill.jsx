/** Hiển thị trạng thái dạng pill với màu + nhãn tiếng Việt. */
const MAP = {
  ACTIVE: { cls: 'pill-success', icon: 'bi-check-circle-fill', label: 'Đang hoạt động' },
  INACTIVE: { cls: 'pill-muted', icon: 'bi-slash-circle', label: 'Ngừng' },
  LOCKED: { cls: 'pill-danger', icon: 'bi-lock-fill', label: 'Đã khóa' },
  OPEN: { cls: 'pill-success', icon: 'bi-unlock-fill', label: 'Đang mở' },
  CLOSED: { cls: 'pill-muted', icon: 'bi-lock-fill', label: 'Đã đóng' },
  COMPLETED: { cls: 'pill-success', icon: 'bi-check-circle-fill', label: 'Hoàn tất' },
  CANCELLED: { cls: 'pill-danger', icon: 'bi-x-circle-fill', label: 'Đã hủy' },
  PENDING_PAYMENT: { cls: 'pill-warning', icon: 'bi-hourglass-split', label: 'Chờ thanh toán' },
  PENDING: { cls: 'pill-warning', icon: 'bi-hourglass-split', label: 'Chờ thanh toán' },
  PAID: { cls: 'pill-success', icon: 'bi-check-circle-fill', label: 'Đã thanh toán' },
  EXPIRED: { cls: 'pill-muted', icon: 'bi-clock-history', label: 'Hết hạn' },
  FAILED: { cls: 'pill-danger', icon: 'bi-x-circle-fill', label: 'Thất bại' },
  CASH: { cls: 'pill-info', icon: 'bi-cash-stack', label: 'Tiền mặt' },
  QR: { cls: 'pill-violet', icon: 'bi-qr-code', label: 'QR/CK' },
  ADMIN: { cls: 'pill-violet', icon: 'bi-shield-lock-fill', label: 'Chủ cửa hàng' },
  MANAGER: { cls: 'pill-info', icon: 'bi-person-gear', label: 'Quản lý' },
  CASHIER: { cls: 'pill-success', icon: 'bi-person-fill', label: 'Thu ngân' },
}

export default function StatusPill({ value }) {
  const s = MAP[value] || { cls: 'pill-muted', icon: 'bi-dot', label: value }
  return <span className={`pill ${s.cls}`}><i className={`bi ${s.icon}`}></i>{s.label}</span>
}
