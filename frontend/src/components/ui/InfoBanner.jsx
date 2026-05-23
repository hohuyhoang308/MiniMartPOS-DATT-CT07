import { useEffect, useState } from 'react'

/**
 * Dải hướng dẫn ngắn gọn cho người dùng trên mỗi màn hình.
 * Có thể đóng lại; trạng thái đóng lưu theo `id` trong localStorage.
 */
export default function InfoBanner({ id, icon = 'bi-info-circle-fill', title, children }) {
  const key = `pos_hint_${id}`
  const [open, setOpen] = useState(true)

  useEffect(() => { setOpen(localStorage.getItem(key) !== '0') }, [key])

  function close() { localStorage.setItem(key, '0'); setOpen(false) }
  if (!open) return null

  return (
    <div className="info-banner fade-up">
      <i className={`bi ${icon} ib-icon`}></i>
      <div className="flex-grow-1">
        {title && <div className="fw-semibold mb-1">{title}</div>}
        <div className="small">{children}</div>
      </div>
      <button type="button" className="btn-close btn-sm" onClick={close} aria-label="Đóng"></button>
    </div>
  )
}
