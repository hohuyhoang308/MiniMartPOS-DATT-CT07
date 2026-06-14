import { useEffect, useMemo, useState } from 'react'
import { Button, Card, Col, Form, Row, Table } from 'react-bootstrap'
import PageHeader from '../../components/ui/PageHeader'
import InfoBanner from '../../components/ui/InfoBanner'
import StatCard from '../../components/ui/StatCard'
import EmptyState from '../../components/ui/EmptyState'
import Loading from '../../components/ui/Loading'
import DiffBadge from '../../components/ui/DiffBadge'
import { reportApi } from '../../api/misc'
import { useToast } from '../../context/ToastContext'
import { errMsg } from '../../api/client'
import { formatMoney } from '../../utils/format'

const iso = (d) => d.toISOString().slice(0, 10)
function monthRange() {
  const now = new Date()
  return { from: iso(new Date(now.getFullYear(), now.getMonth(), 1)), to: iso(now) }
}

export default function EmployeeReport({ embedded = false }) {
  const toast = useToast()
  const def = monthRange()
  const [from, setFrom] = useState(def.from)
  const [to, setTo] = useState(def.to)
  const [rows, setRows] = useState([])
  const [loading, setLoading] = useState(true)

  function load(f = from, t = to) {
    setLoading(true)
    reportApi.employees(f, t).then(setRows).catch((e) => toast.error(errMsg(e))).finally(() => setLoading(false))
  }
  useEffect(() => { load() }, [])

  const stats = useMemo(() => {
    const revenue = rows.reduce((s, r) => s + Number(r.revenue || 0), 0)
    const variance = rows.reduce((s, r) => s + Number(r.cashVariance || 0), 0)
    return { staff: rows.length, revenue, variance }
  }, [rows])

  return (
    <div>
      {!embedded && <PageHeader title="Hiệu suất nhân viên" subtitle="Doanh số bán và trách nhiệm quỹ tiền của từng thu ngân" />}

      <InfoBanner id="emp-report" title="Trang này dùng để làm gì?">
        Theo dõi <b>từng thu ngân</b>: bán được bao nhiêu (doanh thu, số hóa đơn, số hàng), trung bình mỗi hóa đơn,
        làm bao nhiêu ca, và <b>tổng tiền lệch quỹ</b> qua các ca đã đóng (âm = hay thiếu quỹ → cần lưu ý).
        Chọn khoảng thời gian rồi bấm "Xem".
      </InfoBanner>

      <Card className="border-0 mb-3">
        <Card.Body>
          <Row className="g-2 align-items-end">
            <Col md={3}><Form.Label>Từ ngày</Form.Label><Form.Control type="date" value={from} onChange={(e) => setFrom(e.target.value)} /></Col>
            <Col md={3}><Form.Label>Đến ngày</Form.Label><Form.Control type="date" value={to} onChange={(e) => setTo(e.target.value)} /></Col>
            <Col md="auto"><Button onClick={() => load()}><i className="bi bi-funnel me-1"></i>Xem</Button></Col>
          </Row>
        </Card.Body>
      </Card>

      {loading ? <Loading /> : (
        <>
          <Row className="g-3 mb-3 stagger">
            <Col md={4}><StatCard icon="bi-people-fill" chip="sky" label="Số nhân viên bán hàng" value={stats.staff} /></Col>
            <Col md={4}><StatCard icon="bi-cash-stack" chip="emerald" label="Tổng doanh thu" value={formatMoney(stats.revenue)} /></Col>
            <Col md={4}><StatCard icon="bi-scale" chip="amber" label="Tổng lệch quỹ" value={formatMoney(stats.variance)} /></Col>
          </Row>

          <div className="table-wrap">
            <Table hover className="mb-0 align-middle">
              <thead><tr>
                <th>Thu ngân</th>
                <th className="text-center">Số hóa đơn</th>
                <th className="text-end">Doanh thu</th>
                <th className="text-center">Hàng đã bán</th>
                <th className="text-end">TB / hóa đơn</th>
                <th className="text-center" title="Số ca đã đóng / tổng số ca">Ca (đóng/tổng)</th>
                <th className="text-end" title="Tổng chênh lệch quỹ các ca đã đóng">Tổng lệch quỹ</th>
              </tr></thead>
              <tbody>
                {rows.map((r) => (
                  <tr key={r.userId}>
                    <td className="fw-semibold">{r.cashierName || `#${r.userId}`}</td>
                    <td className="text-center num">{r.invoiceCount}</td>
                    <td className="text-end num fw-semibold text-success">{formatMoney(r.revenue)}</td>
                    <td className="text-center num">{r.itemsSold}</td>
                    <td className="text-end num text-muted2">{formatMoney(r.avgOrderValue)}</td>
                    <td className="text-center num text-muted2">{r.closedShifts}/{r.totalShifts}</td>
                    <td className="text-end">{r.closedShifts > 0 ? <DiffBadge value={r.cashVariance} /> : '—'}</td>
                  </tr>
                ))}
                {rows.length === 0 && <tr><td colSpan={7}><EmptyState icon="bi-people" title="Chưa có dữ liệu bán hàng trong khoảng đã chọn" /></td></tr>}
              </tbody>
            </Table>
          </div>
        </>
      )}
    </div>
  )
}
