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
  EXPIRE_INVOICE: { cls: 'pill-muted', icon: 'bi-clock-history', label: 'Hóa đơn QR quá hạn' },
  CHANGE_PRICE: { cls: 'pill-info', icon: 'bi-tag-fill', label: 'Đổi giá' },
  CREATE_STORE: { cls: 'pill-success', icon: 'bi-shop', label: 'Thêm chi nhánh' },
  UPDATE_STORE: { cls: 'pill-info', icon: 'bi-shop', label: 'Sửa chi nhánh' },
  CONFIRM_PAYMENT: { cls: 'pill-success', icon: 'bi-check2-circle', label: 'Xác nhận thanh toán' },
  CREATE_RECEIPT: { cls: 'pill-info', icon: 'bi-box-seam', label: 'Nhập kho' },
  WRITEOFF_STOCK: { cls: 'pill-danger', icon: 'bi-trash3-fill', label: 'Xuất hủy tồn kho' },
  SHELVE: { cls: 'pill-info', icon: 'bi-box-arrow-in-up', label: 'Lên kệ' },
  SHELF_RETURN: { cls: 'pill-warning', icon: 'bi-box-arrow-down', label: 'Lấy về kho' },
  CLOSE_SHIFT: { cls: 'pill-violet', icon: 'bi-door-closed-fill', label: 'Đóng ca' },
  CREATE_USER: { cls: 'pill-success', icon: 'bi-person-plus-fill', label: 'Tạo tài khoản' },
  UPDATE_USER: { cls: 'pill-info', icon: 'bi-person-gear', label: 'Sửa tài khoản' },
  LOCK_USER: { cls: 'pill-danger', icon: 'bi-person-lock', label: 'Khóa tài khoản' },
  RESET_PASSWORD: { cls: 'pill-warning', icon: 'bi-key-fill', label: 'Đặt lại mật khẩu' },
  UPDATE_CONFIG: { cls: 'pill-muted', icon: 'bi-gear-fill', label: 'Đổi cấu hình' },
  OPEN_SHIFT: { cls: 'pill-success', icon: 'bi-door-open-fill', label: 'Mở ca' },

  CREATE_PRODUCT: { cls: 'pill-success', icon: 'bi-plus-square-fill', label: 'Thêm sản phẩm' },
  UPDATE_PRODUCT: { cls: 'pill-info', icon: 'bi-pencil-square', label: 'Sửa sản phẩm' },
  DELETE_PRODUCT: { cls: 'pill-danger', icon: 'bi-trash-fill', label: 'Xóa sản phẩm' },

  CREATE_CATEGORY: { cls: 'pill-success', icon: 'bi-plus-square-fill', label: 'Thêm danh mục' },
  UPDATE_CATEGORY: { cls: 'pill-info', icon: 'bi-pencil-square', label: 'Sửa danh mục' },
  DELETE_CATEGORY: { cls: 'pill-danger', icon: 'bi-trash-fill', label: 'Xóa danh mục' },

  CREATE_UNIT: { cls: 'pill-success', icon: 'bi-plus-square-fill', label: 'Thêm đơn vị tính' },
  UPDATE_UNIT: { cls: 'pill-info', icon: 'bi-pencil-square', label: 'Sửa đơn vị tính' },
  DELETE_UNIT: { cls: 'pill-danger', icon: 'bi-trash-fill', label: 'Xóa đơn vị tính' },

  CREATE_SUPPLIER: { cls: 'pill-success', icon: 'bi-truck', label: 'Thêm nhà cung cấp' },
  UPDATE_SUPPLIER: { cls: 'pill-info', icon: 'bi-truck', label: 'Sửa nhà cung cấp' },
  DELETE_SUPPLIER: { cls: 'pill-danger', icon: 'bi-trash-fill', label: 'Xóa nhà cung cấp' },

  CREATE_PROMOTION: { cls: 'pill-success', icon: 'bi-ticket-perforated-fill', label: 'Thêm khuyến mãi' },
  UPDATE_PROMOTION: { cls: 'pill-info', icon: 'bi-ticket-perforated-fill', label: 'Sửa khuyến mãi' },
  DELETE_PROMOTION: { cls: 'pill-danger', icon: 'bi-trash-fill', label: 'Xóa khuyến mãi' },

  CREATE_CUSTOMER: { cls: 'pill-success', icon: 'bi-person-plus-fill', label: 'Thêm khách hàng' },
  UPDATE_CUSTOMER: { cls: 'pill-info', icon: 'bi-person-lines-fill', label: 'Sửa khách hàng' },
  DELETE_CUSTOMER: { cls: 'pill-danger', icon: 'bi-person-x-fill', label: 'Xóa khách hàng' },

  CREATE_SHELF: { cls: 'pill-success', icon: 'bi-grid-3x3-gap-fill', label: 'Thêm kệ' },
  UPDATE_SHELF: { cls: 'pill-info', icon: 'bi-grid-3x3-gap-fill', label: 'Sửa kệ' },
  DELETE_SHELF: { cls: 'pill-danger', icon: 'bi-trash-fill', label: 'Xóa kệ' },
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
      <PageHeader title="Nhật ký thao tác" subtitle="Lịch sử ai làm gì, lúc nào với những thao tác quan trọng (200 mục mới nhất)" />

      <InfoBanner id="audit" title="Trang này dùng để làm gì?">
        Ghi lại <b>ai làm</b>, <b>vào lúc nào</b> và <b>chi tiết</b> của những thao tác quan trọng:
        <b> hủy hóa đơn / hóa đơn quá hạn</b>, <b>đổi giá</b>, <b>xác nhận thanh toán</b>, <b>nhập kho</b>,
        <b> lên kệ / lấy về kho</b>, <b>đóng ca</b>, <b>tài khoản và mật khẩu</b>, <b>đổi cấu hình</b>.
        Giúp bạn kiểm tra lại và tránh gian lận. Nhật ký này <b>chỉ ghi thêm, không sửa hay xóa được</b>.
      </InfoBanner>

      <Card className="border-0 fill-card">
        <Card.Body className="d-flex align-items-center gap-2 flex-wrap">
          <span className="text-muted2 small">Lọc theo loại thao tác:</span>
          <Form.Select size="sm" style={{ maxWidth: 220 }} value={filter} onChange={(e) => setFilter(e.target.value)}>
            <option value="">— Tất cả ({rows.length}) —</option>
            {actions.map((a) => <option key={a} value={a}>{ACTION[a]?.label || a}</option>)}
          </Form.Select>
        </Card.Body>
        <div className="table-responsive fill-scroll">
          <Table hover className="mb-0 align-middle">
            <thead><tr>
              <th>Thời điểm</th><th>Người làm</th><th className="text-center">Thao tác</th>
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
              {list.length === 0 && <tr><td colSpan={5}><EmptyState icon="bi-shield-check" title="Chưa có thao tác nào được ghi lại" /></td></tr>}
            </tbody>
          </Table>
        </div>
      </Card>
    </div>
  )
}
