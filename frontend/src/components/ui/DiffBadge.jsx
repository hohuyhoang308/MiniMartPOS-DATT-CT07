import { formatMoney } from '../../utils/format'

/** Hiển thị chênh lệch quỹ: null = —, 0 = "Khớp", dương = thừa (xanh), âm = thiếu (đỏ). */
export default function DiffBadge({ value }) {
  if (value == null) return <span className="text-muted2">—</span>
  const v = Number(value)
  if (v === 0) return <span className="pill pill-success">Khớp</span>
  return (
    <span className={`fw-semibold ${v > 0 ? 'text-success' : 'text-danger'}`}>
      {v > 0 ? '+' : ''}{formatMoney(v)}
    </span>
  )
}
