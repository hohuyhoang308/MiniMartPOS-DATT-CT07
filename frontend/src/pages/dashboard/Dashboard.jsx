import { useEffect, useState } from 'react'
import { Card, Col, Row, Table } from 'react-bootstrap'
import { useNavigate } from 'react-router-dom'
import {
  Area, AreaChart, Bar, BarChart, Cell, CartesianGrid, Legend, Pie, PieChart,
  ResponsiveContainer, Tooltip, XAxis, YAxis,
} from 'recharts'
import PageHeader from '../../components/ui/PageHeader'
import StatusPill from '../../components/ui/StatusPill'
import EmptyState from '../../components/ui/EmptyState'
import Loading from '../../components/ui/Loading'
import { dashboardApi } from '../../api/misc'
import { useToast } from '../../context/ToastContext'
import { useAuth } from '../../context/AuthContext'
import { errMsg } from '../../api/client'
import { formatMoney } from '../../utils/format'

const PAY_COLOR = { CASH: '#0ea5e9', QR: '#8b5cf6' }
const CAT_COLORS = ['#10b981', '#0ea5e9', '#f59e0b', '#8b5cf6', '#f43f5e', '#14b8a6']

/**
 * Dựng đủ chuỗi 7 ngày gần nhất cho biểu đồ. Backend (revenueByDay) chỉ trả về
 * những ngày CÓ hóa đơn COMPLETED, nên nếu chỉ 1-2 ngày phát sinh giao dịch thì
 * AreaChart chỉ vẽ ra 1 chấm. Ở đây ta map dữ liệu theo ngày rồi điền 0 cho các
 * ngày trống → luôn có 7 điểm để vẽ thành đường liền mạch.
 */
function build7DayChart(rows) {
  const byDay = new Map((rows || []).map((r) => [String(r.day || '').slice(0, 10), Number(r.revenue || 0)]))
  const out = []
  const today = new Date()
  for (let i = 6; i >= 0; i--) {
    const dt = new Date(today)
    dt.setDate(today.getDate() - i)
    const y = dt.getFullYear()
    const m = String(dt.getMonth() + 1).padStart(2, '0')
    const day = String(dt.getDate()).padStart(2, '0')
    const key = `${y}-${m}-${day}`
    out.push({ label: `${day}/${m}`, revenue: byDay.get(key) || 0 })
  }
  return out
}

function trend(today, yesterday) {
  const t = Number(today || 0), y = Number(yesterday || 0)
  if (y === 0) return t > 0 ? { pct: 100, up: true } : { pct: 0, up: true }
  const pct = Math.round(((t - y) / y) * 100)
  return { pct: Math.abs(pct), up: pct >= 0 }
}

function KpiCard({ icon, chip, label, value, sub }) {
  return (
    <Card className="stat-card border-0 h-100">
      <Card.Body>
        <div className="d-flex justify-content-between align-items-start">
          <div className={`stat-chip chip-${chip}`}><i className={`bi ${icon}`}></i></div>
          {sub}
        </div>
        <div className="stat-label mt-3">{label}</div>
        <div className="stat-value">{value}</div>
      </Card.Body>
    </Card>
  )
}

function AlertCard({ icon, color, count, label, onClick }) {
  return (
    <div className="soft-card p-3 d-flex align-items-center gap-3 cursor-pointer h-100" onClick={onClick}
         style={{ borderLeft: `4px solid ${color}` }}>
      <i className={`bi ${icon}`} style={{ fontSize: '1.6rem', color }}></i>
      <div>
        <div className="num fw-bold fs-5" style={{ lineHeight: 1 }}>{count}</div>
        <div className="small text-muted2">{label}</div>
      </div>
      <i className="bi bi-chevron-right text-muted2 ms-auto"></i>
    </div>
  )
}

export default function Dashboard() {
  const toast = useToast()
  const { user } = useAuth()
  const navigate = useNavigate()
  const [d, setD] = useState(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    dashboardApi.get().then(setD).catch((e) => toast.error(errMsg(e))).finally(() => setLoading(false))
  }, [])

  if (loading) return <Loading />
  if (!d) return null

  const tRev = trend(d.revenueToday, d.revenueYesterday)
  const hasRevenue = (d.revenueChart || []).length > 0
  const chart = build7DayChart(d.revenueChart)
  const payData = (d.paymentBreakdown || []).map((p) => ({ name: p.method === 'CASH' ? 'Tiền mặt' : 'QR/CK', method: p.method, value: Number(p.amount || 0) }))
  const hourData = (d.hourlySales || []).map((h) => ({ label: `${String(h.hour).padStart(2, '0')}h`, revenue: Number(h.revenue || 0) }))
  const catData = (d.categorySales || []).map((c) => ({ name: c.categoryName, value: Number(c.revenue || 0) }))
  const maxQty = Math.max(1, ...(d.topProducts || []).map((p) => p.quantitySold))

  return (
    <div>
      <PageHeader title={`Xin chào, ${user?.fullName} 👋`} subtitle="Bức tranh kinh doanh của cửa hàng hôm nay" />

      {/* KPI chính */}
      <Row className="g-3 mb-3 stagger">
        <Col xl={3} md={6}>
          <KpiCard icon="bi-cash-coin" chip="emerald" label="Doanh thu hôm nay" value={formatMoney(d.revenueToday)}
            sub={<span className={`pill ${tRev.up ? 'pill-success' : 'pill-danger'}`}>
              <i className={`bi ${tRev.up ? 'bi-arrow-up-right' : 'bi-arrow-down-right'}`}></i>{tRev.pct}% vs hôm qua</span>} />
        </Col>
        <Col xl={3} md={6}>
          <KpiCard icon="bi-graph-up-arrow" chip="violet" label="Lợi nhuận hôm nay" value={formatMoney(d.profitToday)}
            sub={<span className="pill pill-muted">Tháng: {formatMoney(d.profitMonth)}</span>} />
        </Col>
        <Col xl={3} md={6}>
          <KpiCard icon="bi-receipt-cutoff" chip="sky" label="Hóa đơn hôm nay" value={d.invoiceCountToday}
            sub={<span className="pill pill-muted">{d.itemsSoldToday} sản phẩm</span>} />
        </Col>
        <Col xl={3} md={6}>
          <KpiCard icon="bi-basket3" chip="amber" label="Trung bình / hóa đơn" value={formatMoney(d.avgOrderValue)}
            sub={<span className="pill pill-muted">{d.customersToday} khách TT</span>} />
        </Col>
      </Row>

      {/* Cảnh báo nhanh */}
      <Row className="g-3 mb-3 stagger">
        <Col md={4}><AlertCard icon="bi-exclamation-triangle-fill" color="#f59e0b" count={d.lowStockCount} label="Mặt hàng tồn thấp" onClick={() => navigate('/inventory')} /></Col>
        <Col md={4}><AlertCard icon="bi-x-octagon-fill" color="#f43f5e" count={d.outOfStockCount} label="Mặt hàng hết hàng" onClick={() => navigate('/inventory')} /></Col>
        <Col md={4}><AlertCard icon="bi-calendar-x-fill" color="#8b5cf6" count={d.expiringCount} label="Lô cận/quá hạn sử dụng" onClick={() => navigate('/inventory')} /></Col>
      </Row>

      {/* Doanh thu + cơ cấu thanh toán */}
      <Row className="g-3 mb-3">
        <Col lg={8}>
          <Card className="border-0 h-100">
            <Card.Body>
              <Card.Title className="fs-6 mb-3">Doanh thu 7 ngày gần nhất</Card.Title>
              {!hasRevenue ? <EmptyState title="Chưa có giao dịch" /> : (
                <ResponsiveContainer width="100%" height={280}>
                  <AreaChart data={chart} margin={{ top: 10, right: 8 }}>
                    <defs><linearGradient id="dRev" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="0%" stopColor="#10b981" stopOpacity={0.4} /><stop offset="100%" stopColor="#10b981" stopOpacity={0} />
                    </linearGradient></defs>
                    <CartesianGrid strokeDasharray="3 3" vertical={false} />
                    <XAxis dataKey="label" /><YAxis tickFormatter={(v) => `${v / 1000}k`} />
                    <Tooltip formatter={(v) => formatMoney(v)} />
                    <Area type="monotone" dataKey="revenue" stroke="#059669" strokeWidth={2.5} fill="url(#dRev)" />
                  </AreaChart>
                </ResponsiveContainer>
              )}
            </Card.Body>
          </Card>
        </Col>
        <Col lg={4}>
          <Card className="border-0 h-100">
            <Card.Body>
              <Card.Title className="fs-6 mb-3">Cơ cấu thanh toán hôm nay</Card.Title>
              {payData.length === 0 ? <EmptyState title="Chưa có" /> : (
                <ResponsiveContainer width="100%" height={280}>
                  <PieChart>
                    <Pie data={payData} dataKey="value" nameKey="name" innerRadius={55} outerRadius={90} paddingAngle={3}>
                      {payData.map((p, i) => <Cell key={i} fill={PAY_COLOR[p.method] || CAT_COLORS[i]} />)}
                    </Pie>
                    <Tooltip formatter={(v) => formatMoney(v)} /><Legend />
                  </PieChart>
                </ResponsiveContainer>
              )}
            </Card.Body>
          </Card>
        </Col>
      </Row>

      {/* Giờ cao điểm + danh mục */}
      <Row className="g-3 mb-3">
        <Col lg={6}>
          <Card className="border-0 h-100">
            <Card.Body>
              <Card.Title className="fs-6 mb-3">Doanh thu theo giờ (hôm nay)</Card.Title>
              {hourData.length === 0 ? <EmptyState title="Chưa có giao dịch hôm nay" /> : (
                <ResponsiveContainer width="100%" height={240}>
                  <BarChart data={hourData}>
                    <CartesianGrid strokeDasharray="3 3" vertical={false} />
                    <XAxis dataKey="label" /><YAxis tickFormatter={(v) => `${v / 1000}k`} />
                    <Tooltip formatter={(v) => formatMoney(v)} />
                    <Bar dataKey="revenue" fill="#0ea5e9" radius={[5, 5, 0, 0]} />
                  </BarChart>
                </ResponsiveContainer>
              )}
            </Card.Body>
          </Card>
        </Col>
        <Col lg={6}>
          <Card className="border-0 h-100">
            <Card.Body>
              <Card.Title className="fs-6 mb-3">Doanh thu theo danh mục (tháng)</Card.Title>
              {catData.length === 0 ? <EmptyState title="Chưa có dữ liệu" /> : (
                <ResponsiveContainer width="100%" height={240}>
                  <BarChart data={catData} layout="vertical" margin={{ left: 10 }}>
                    <CartesianGrid strokeDasharray="3 3" horizontal={false} />
                    <XAxis type="number" tickFormatter={(v) => `${v / 1000}k`} />
                    <YAxis type="category" dataKey="name" width={110} tick={{ fontSize: 11 }} />
                    <Tooltip formatter={(v) => formatMoney(v)} />
                    <Bar dataKey="value" radius={[0, 5, 5, 0]} barSize={18}>
                      {catData.map((c, i) => <Cell key={i} fill={CAT_COLORS[i % CAT_COLORS.length]} />)}
                    </Bar>
                  </BarChart>
                </ResponsiveContainer>
              )}
            </Card.Body>
          </Card>
        </Col>
      </Row>

      {/* Giao dịch gần đây + top sản phẩm */}
      <Row className="g-3">
        <Col lg={7}>
          <Card className="border-0 h-100">
            <Card.Body className="pb-0"><Card.Title className="fs-6">Giao dịch gần đây</Card.Title></Card.Body>
            <Table hover className="mb-0">
              <thead><tr><th>Mã HĐ</th><th>Thu ngân</th><th>TT</th><th className="text-end">Tổng</th><th>Giờ</th><th>Trạng thái</th></tr></thead>
              <tbody>
                {(d.recentInvoices || []).map((r, i) => (
                  <tr key={i}>
                    <td className="fw-semibold">{r.code}</td>
                    <td className="text-muted2 small">{r.cashierName}</td>
                    <td><StatusPill value={r.paymentMethod} /></td>
                    <td className="text-end num fw-semibold">{formatMoney(r.totalAmount)}</td>
                    <td className="text-muted2 small">{r.createdAt}</td>
                    <td><StatusPill value={r.status} /></td>
                  </tr>
                ))}
                {(d.recentInvoices || []).length === 0 && <tr><td colSpan={6}><EmptyState icon="bi-receipt" title="Chưa có giao dịch" /></td></tr>}
              </tbody>
            </Table>
          </Card>
        </Col>
        <Col lg={5}>
          <Card className="border-0 h-100">
            <Card.Body>
              <Card.Title className="fs-6 mb-3">Top sản phẩm bán chạy (tháng)</Card.Title>
              {(d.topProducts || []).length === 0 ? <EmptyState title="Chưa có dữ liệu" /> : (
                <div className="d-flex flex-column gap-3">
                  {d.topProducts.map((p, i) => (
                    <div key={p.productId} className="d-flex align-items-center gap-3">
                      <span className={`stat-chip chip-${['amber', 'sky', 'violet', 'emerald', 'rose'][i % 5]}`} style={{ width: 34, height: 34, fontSize: '.85rem', fontWeight: 700 }}>{i + 1}</span>
                      <div className="flex-grow-1 min-w-0">
                        <div className="d-flex justify-content-between"><span className="fw-semibold text-truncate" style={{ maxWidth: 150 }}>{p.productName}</span><span className="num small text-muted2">{p.quantitySold}</span></div>
                        <div className="progress mt-1" style={{ height: 6, background: '#eef2f7' }}>
                          <div className="progress-bar" style={{ width: `${(p.quantitySold / maxQty) * 100}%`, background: 'linear-gradient(90deg,var(--brand-400),var(--brand-600))' }} />
                        </div>
                      </div>
                      <span className="num small fw-semibold text-success">{formatMoney(p.revenue)}</span>
                    </div>
                  ))}
                </div>
              )}
            </Card.Body>
          </Card>
        </Col>
      </Row>
    </div>
  )
}
