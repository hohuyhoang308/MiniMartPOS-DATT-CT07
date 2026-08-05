/** Định dạng tiền VND. */
export function formatMoney(value) {
  const n = Number(value || 0)
  return n.toLocaleString('vi-VN') + 'đ'
}

/** Định dạng ngày giờ ISO theo locale vi-VN (24h), ví dụ "20:30:45 14/7/2026". */
export function formatDateTime(iso) {
  if (!iso) return ''
  const d = new Date(iso)
  return d.toLocaleString('vi-VN', { hour12: false })
}

/** Ngày dạng 'YYYY-MM-DD' từ một Date. */
export const iso = (d) => d.toISOString().slice(0, 10)

/** Khoảng thời gian mặc định cho báo cáo: từ đầu tháng này tới hôm nay. */
export function monthRange() {
  const now = new Date()
  return { from: iso(new Date(now.getFullYear(), now.getMonth(), 1)), to: iso(now) }
}

/** Số giờ công theo locale vi-VN, ví dụ "7,5h". */
export const formatHours = (h) => `${Number(h || 0).toLocaleString('vi-VN', { maximumFractionDigits: 2 })}h`

/** Tháng hiện tại dạng 'YYYY-MM' (theo giờ máy). */
export function currentMonth() {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`
}
