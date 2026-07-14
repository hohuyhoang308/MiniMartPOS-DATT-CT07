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
