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
  RETURN: { cls: 'pill-warning', icon: 'bi-arrow-return-left', label: 'Trả hàng' },
  CHANGE_PRICE: { cls: 'pill-info', icon: 'bi-tag-fill', label: 'Đổi giá' },
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
    <div>
      <PageHeader title="Nhật ký kiểm toán" subtitle="Vết ai làm gì, khi nào — cho các thao tác nhạy cảm (200 vết mới nhất)" />

      <InfoBanner id="audit" title="Nhật ký kiểm toán là gì?">
        Ghi lại <b>người thực hiện</b>, <b>thời điểm</b> và <b>lý do</b> của các thao tác nhạy cảm:
        <b> hủy hóa đơn</b>, <b>trả hàng</b>, <b>đổi giá</b>. Giúp truy vết & chống gian lận (vd void-rồi-thủ-tiền).
        Nhật ký là <b>chỉ thêm</b> (không sửa/xóa được).
      </InfoBanner>

      <Card className="border-0">
        <Card.Body className="d-flex align-items-center gap-2 flex-wrap">
          <span className="text-muted2 small">Lọc theo hành động:</span>
          <Form.Select size="sm" style={{ maxWidth: 220 }} value={filter} onChange={(e) => setFilter(e.target.value)}>
            <option value="">— Tất cả ({rows.length}) —</option>
            {actions.map((a) => <option key={a} value={a}>{ACTION[a]?.label || a}</option>)}
          </Form.Select>
        </Card.Body>
        <div className="table-responsive" style={{ maxHeight: 560, overflowY: 'auto' }}>
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
