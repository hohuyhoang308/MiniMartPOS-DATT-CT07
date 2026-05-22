/** Tiêu đề trang chuẩn: title + mô tả + vùng hành động bên phải. */
export default function PageHeader({ title, subtitle, children }) {
  return (
    <div className="page-header fade-up">
      <div>
        <h1 className="ph-title">{title}</h1>
        {subtitle && <p className="ph-sub">{subtitle}</p>}
      </div>
      {children && <div className="d-flex gap-2 flex-wrap">{children}</div>}
    </div>
  )
}
