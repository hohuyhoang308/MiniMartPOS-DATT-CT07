import { formatMoney } from '../../utils/format'

/** Một dòng đối soát quỹ (nhãn + số tiền), dùng chung ở modal đóng ca POS & Quản lý ca. */
export default function Recon({ label, value, icon, strong }) {
  return (
    <div className="d-flex justify-content-between align-items-center py-1">
      <span className={strong ? 'fw-semibold' : 'text-muted2'}>
        {icon && <i className={`bi ${icon} me-1 text-muted2`}></i>}{label}
      </span>
      <span className={`num ${strong ? 'fw-bold fs-6' : ''}`}>{formatMoney(value)}</span>
    </div>
  )
}
