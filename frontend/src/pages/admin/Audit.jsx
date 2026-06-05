import { useEffect, useState } from 'react'
import { Card, Form, Table } from 'react-bootstrap'
import PageHeader from '../../components/ui/PageHeader'
import InfoBanner from '../../components/ui/InfoBanner'
import EmptyState from '../../components/ui/EmptyState'
import Loading from '../../components/ui/Loading'
import { auditApi } from '../../api/misc'
import { useToast } from '../../context/ToastContext'
import { errMsg } from '../../api/client'

const ACTION = {
  CANCEL_INVOICE: { cls: 'pill-danger', icon: 'bi-x-circle-fill', label: 'Hủy hóa đơn' },
  EXPIRE_INVOICE: { cls: 'pill-muted', icon: 'bi-clock-history', label: 'HĐ QR quá hạn' },
  RETURN: { cls: 'pill-warning', icon: 'bi-arrow-return-left', label: 'Trả hàng' },
  CHANGE_PRICE: { cls: 'pill-info', icon: 'bi-tag-fill', label: 'Đổi giá' },
  CONFIRM_PAYMENT: { cls: 'pill-success', icon: 'bi-check2-circle', label: 'Xác nhận thanh toán' },
  CREATE_RECEIPT: { cls: 'pill-info', icon: 'bi-box-seam', label: 'Nhập kho' },
  SHELVE: { cls: 'pill-info', icon: 'bi-box-arrow-in-up', label: 'Lên kệ' },
  SHELF_RETURN: { cls: 'pill-warning', icon: 'bi-box-arrow-down', label: 'Lấy về kho' },
  CLOSE_SHIFT: { cls: 'pill-violet', icon: 'bi-door-closed-fill', label: 'Đóng ca' },
  CREATE_USER: { cls: 'pill-success', icon: 'bi-person-plus-fill', label: 'Tạo tài khoản' },
  UPDATE_USER: { cls: 'pill-info', icon: 'bi-person-gear', label: 'Sửa tài khoản' },
  LOCK_USER: { cls: 'pill-danger', icon: 'bi-person-lock', label: 'Khóa tài khoản' },
  RESET_PASSWORD: { cls: 'pill-warning', icon: 'bi-key-fill', label: 'Đặt lại mật khẩu' },
  UPDATE_CONFIG: { cls: 'pill-muted', icon: 'bi-gear-fill', label: 'Đổi cấu hình' },
}

function fmtTime(t) {
  if (!t) return '—'
  const d = new Date(t)
  return d.toLocaleString('vi-VN', { hour: '2-digit', minute: '2-digit', day: '2-digit', month: '2-digit', year: 'numeric' })
}

export default function Audit() {
  const toast = useToast()
  const [rows, setRows] = useState([])
  const [loading, setLoading] = useState(true)
  const [filter, setFilter] = useState('')

  useEffect(() => {
    auditApi.recent().then(setRows).catch((e) => toast.error(errMsg(e))).finally(() => setLoading(false))
  }, [])

  if (loading) return <Loading />

  const list = filter ? rows.filter((r) => r.action === filter) : rows
  const actions = [...new Set(rows.map((r) => r.action))]

  return (
    <div className="page-fill">
      <PageHeader title="Nhật ký kiểm toán" subtitle="Vết ai làm gì, khi nào — cho các thao tác nhạy cảm (200 vết mới nhất)" />

      <InfoBanner id="audit" title="Nhật ký kiểm toán là gì?">
        Ghi lại <b>người thực hiện</b>, <b>thời điểm</b> và <b>chi tiết</b> của các thao tác nhạy cảm:
        <b> hủy/quá hạn hóa đơn</b>, <b>trả hàng</b>, <b>đổi giá</b>, <b>xác nhận thanh toán</b>, <b>nhập kho</b>,
        <b> lên kệ / lấy về kho</b>, <b>đóng ca</b>, <b>tài khoản & mật khẩu</b>, <b>đổi cấu hình</b>.
        Giúp truy vết & chống gian lận (vd void-rồi-thủ-tiền). Nhật ký là <b>chỉ thêm</b> (không sửa/xóa được).
      </InfoBanner>

      <Card className="border-0 fill-card">
        <Card.Body className="d-flex align-items-center gap-2 flex-wrap">
          <span className="text-muted2 small">Lọc theo hành động:</span>
          <Form.Select size="sm" style={{ maxWidth: 220 }} value={filter} onChange={(e) => setFilter(e.target.value)}>
            <option value="">— Tất cả ({rows.length}) —</option>
            {actions.map((a) => <option key={a} value={a}>{ACTION[a]?.label || a}</option>)}
          </Form.Select>
        </Card.Body>
        <div className="table-responsive fill-scroll">
          <Table hover className="mb-0 align-middle">
            <thead><tr>
              <th>Thời điểm</th><th>Người thực hiện</th><th className="text-center">Hành động</th>
              <th>Đối tượng</th><th>Chi tiết</th>
            </tr></thead>
            <tbody>
              {list.map((r) => {
                const a = ACTION[r.action] || { cls: 'pill-muted', icon: 'bi-dot', label: r.action }
                return (
                  <tr key={r.id}>
                    <td className="num text-muted2 small" style={{ whiteSpace: 'nowrap' }}>{fmtTime(r.createdAt)}</td>
                    <td className="fw-semibold">{r.actorUsername || '—'}</td>
                    <td className="text-center"><span className={`pill ${a.cls}`}><i className={`bi ${a.icon}`}></i>{a.label}</span></td>
                    <td className="text-muted2 small">{r.targetType}{r.targetId ? ` #${r.targetId}` : ''}</td>
                    <td className="small">{r.detail}</td>
                  </tr>
                )
              })}
              {list.length === 0 && <tr><td colSpan={5}><EmptyState icon="bi-shield-check" title="Chưa có vết kiểm toán nào" /></td></tr>}
            </tbody>
          </Table>
        </div>
      </Card>
    </div>
  )
}
