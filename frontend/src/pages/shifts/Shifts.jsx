import { useEffect, useMemo, useState } from 'react'
import { Button, Card, Col, Row, Table } from 'react-bootstrap'
import PageHeader from '../../components/ui/PageHeader'
import InfoBanner from '../../components/ui/InfoBanner'
import StatCard from '../../components/ui/StatCard'
import StatusPill from '../../components/ui/StatusPill'
import EmptyState from '../../components/ui/EmptyState'
import Loading from '../../components/ui/Loading'
import DiffBadge from '../../components/ui/DiffBadge'
import CloseShiftModal from '../../components/CloseShiftModal'
import { shiftApi } from '../../api/sales'
import { useToast } from '../../context/ToastContext'
import { errMsg } from '../../api/client'
import { formatMoney } from '../../utils/format'

/** Phiên gợi ý theo giờ mở ca (giúp chia ca hợp lý: Sáng/Chiều/Tối). */
function sessionOf(openedAt) {
  const h = openedAt ? new Date(openedAt).getHours() : 0
  if (h < 12) return { label: 'Ca sáng', cls: 'pill-warning', icon: 'bi-sunrise' }
  if (h < 17) return { label: 'Ca chiều', cls: 'pill-info', icon: 'bi-sun' }
  return { label: 'Ca tối', cls: 'pill-violet', icon: 'bi-moon-stars' }
}

function fmtTime(s) {
  if (!s) return '—'
  const d = new Date(s)
  return d.toLocaleString('vi-VN', { day: '2-digit', month: '2-digit', hour: '2-digit', minute: '2-digit' })
}

/** Thời lượng ca: từ lúc mở tới lúc đóng (hoặc hiện tại nếu đang mở). */
function duration(openedAt, closedAt) {
  if (!openedAt) return '—'
  const end = closedAt ? new Date(closedAt) : new Date()
  let mins = Math.max(0, Math.round((end - new Date(openedAt)) / 60000))
  const h = Math.floor(mins / 60)
  mins %= 60
  return h > 0 ? `${h}h ${mins}p` : `${mins}p`
}

/** Ca đang mở "quá hạn": mở từ ngày khác hoặc kéo dài > 16 giờ → nên nhắc đóng. */
function staleOpen(s) {
  if (s.status !== 'OPEN' || !s.openedAt) return false
  const opened = new Date(s.openedAt)
  const now = new Date()
  return opened.toDateString() !== now.toDateString() || (now - opened) > 16 * 3600 * 1000
}

export default function Shifts() {
  const toast = useToast()
  const [shifts, setShifts] = useState([])
  const [loading, setLoading] = useState(true)
  const [closing, setClosing] = useState(null) // ca đang đóng

  function load() {
    setLoading(true)
    shiftApi.list().then(setShifts).catch((e) => toast.error(errMsg(e))).finally(() => setLoading(false))
  }
  useEffect(() => { load() }, [])

  const stats = useMemo(() => {
    const open = shifts.filter((s) => s.status === 'OPEN').length
    const sales = shifts.reduce((sum, s) => sum + Number(s.totalSales || 0), 0)
    const diff = shifts.reduce((sum, s) => sum + Number(s.cashDifference || 0), 0)
    return { open, total: shifts.length, sales, diff }
  }, [shifts])

  if (loading) return <Loading />

  return (
    <div>
      <PageHeader title="Quản lý ca làm việc" subtitle="Theo dõi ca của thu ngân, đối soát quỹ và đóng ca" />

      <InfoBanner id="shifts" title="Cách quản lý ca">
        Mỗi thu ngân <b>mở 1 ca</b> khi bắt đầu (nhập tiền đầu ca) và <b>đóng ca</b> khi hết phiên — gợi ý chia
        <b> Ca sáng / chiều / tối</b> theo giờ. <b>Tiền mặt</b> nằm trong két nên cần đối soát; <b>tiền QR/CK</b> vào
        tài khoản ngân hàng (không tính vào két). Khi đóng ca, hệ thống <b>điền sẵn số tiền dự kiến</b> — nếu khớp chỉ
        cần bấm <b>Khớp quỹ</b>, không phải đếm lại. Quản lý/chủ có thể <b>đóng hộ</b> ca của thu ngân.
      </InfoBanner>

      <Row className="g-3 mb-3 stagger">
        <Col md={3}><StatCard icon="bi-unlock-fill" chip="emerald" label="Ca đang mở" value={stats.open} /></Col>
        <Col md={3}><StatCard icon="bi-clock-history" chip="sky" label="Tổng số ca" value={stats.total} /></Col>
        <Col md={3}><StatCard icon="bi-cash-stack" chip="violet" label="Doanh thu các ca" value={formatMoney(stats.sales)} /></Col>
        <Col md={3}><StatCard icon="bi-scale" chip="amber" label="Chênh lệch quỹ (tổng)" value={formatMoney(stats.diff)} /></Col>
      </Row>

      <Card className="border-0">
        <Card.Body className="pb-0 d-flex justify-content-between align-items-center">
          <Card.Title className="fs-6 mb-0">Danh sách ca</Card.Title>
          <Button size="sm" variant="soft" onClick={load}><i className="bi bi-arrow-clockwise me-1"></i>Làm mới</Button>
        </Card.Body>
        <div className="table-responsive">
          <Table hover className="mb-0 align-middle">
            <thead><tr>
              <th>Ca</th><th>Thu ngân</th><th>Phiên</th>
              <th>Mở · Đóng</th><th className="text-center">Thời lượng</th>
              <th className="text-end">Tiền đầu</th>
              <th className="text-end">Tiền mặt bán</th>
              <th className="text-end">QR/CK</th>
              <th className="text-end">Dự kiến két</th>
              <th className="text-end">Đếm thực</th>
              <th className="text-end">Chênh lệch</th>
              <th className="text-center">Trạng thái</th>
              <th></th>
            </tr></thead>
            <tbody>
              {shifts.map((s) => {
                const ss = sessionOf(s.openedAt)
                return (
                  <tr key={s.id}>
                    <td className="fw-semibold">#{s.id}</td>
                    <td>{s.cashierName}</td>
                    <td><span className={`pill ${ss.cls}`}><i className={`bi ${ss.icon}`}></i>{ss.label}</span></td>
                    <td className="small text-muted2">{fmtTime(s.openedAt)}<br />{s.closedAt
                      ? fmtTime(s.closedAt)
                      : staleOpen(s)
                        ? <span className="text-danger fw-semibold"><i className="bi bi-exclamation-triangle-fill me-1"></i>mở qua ngày — nên đóng</span>
                        : <span className="text-success">đang mở</span>}</td>
                    <td className="text-center num">{duration(s.openedAt, s.closedAt)}</td>
                    <td className="text-end num">{formatMoney(s.openingCash)}</td>
                    <td className="text-end num">{formatMoney(s.cashSales)}</td>
                    <td className="text-end num text-muted2">{formatMoney(s.qrSales)}</td>
                    <td className="text-end num fw-semibold">{formatMoney(s.expectedCash)}</td>
                    <td className="text-end num">{s.closingCash != null ? formatMoney(s.closingCash) : '—'}</td>
                    <td className="text-end"><DiffBadge value={s.cashDifference} /></td>
                    <td className="text-center"><StatusPill value={s.status} /></td>
                    <td className="text-end">
                      {s.status === 'OPEN' && (
                        <Button size="sm" variant="danger" onClick={() => setClosing(s)}>
                          <i className="bi bi-door-closed me-1"></i>Đóng ca
                        </Button>
                      )}
                    </td>
                  </tr>
                )
              })}
              {shifts.length === 0 && <tr><td colSpan={13}><EmptyState icon="bi-clock-history" title="Chưa có ca làm việc" /></td></tr>}
            </tbody>
          </Table>
        </div>
      </Card>

      <CloseShiftModal shift={closing} onHide={() => setClosing(null)} onClosed={() => { setClosing(null); load() }} />
    </div>
  )
}

