/** Pill HSD theo số ngày còn lại: quá hạn/đỏ, ≤7n vàng, ≤30n xanh dương, còn lại xám. */
export default function ExpiryPill({ days, date }) {
  if (date == null) return <span className="text-muted2">Không HSD</span>
  const cls = days < 0 ? 'pill-danger' : days <= 7 ? 'pill-warning' : days <= 30 ? 'pill-info' : 'pill-muted'
  return <span className={`pill ${cls}`}>{date} · {days < 0 ? `quá ${-days}n` : `${days}n`}</span>
}
