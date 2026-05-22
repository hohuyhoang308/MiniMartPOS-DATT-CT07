export default function EmptyState({ icon = 'bi-inbox', title = 'Chưa có dữ liệu', hint }) {
  return (
    <div className="empty-state">
      <i className={`bi ${icon}`}></i>
      <div className="mt-2 fw-semibold">{title}</div>
      {hint && <div className="small">{hint}</div>}
    </div>
  )
}
