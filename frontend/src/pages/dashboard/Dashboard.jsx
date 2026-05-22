import { useEffect, useState } from 'react'
import { Card, Col, Row } from 'react-bootstrap'
import {
  Area, AreaChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis,
} from 'recharts'
import PageHeader from '../../components/ui/PageHeader'
import StatCard from '../../components/ui/StatCard'
import EmptyState from '../../components/ui/EmptyState'
import Loading from '../../components/ui/Loading'
import { dashboardApi } from '../../api/misc'
import { useToast } from '../../context/ToastContext'
import { useAuth } from '../../context/AuthContext'
import { errMsg } from '../../api/client'
import { formatMoney } from '../../utils/format'

const RANK_COLORS = ['chip-amber', 'chip-sky', 'chip-violet', 'chip-emerald', 'chip-rose']

export default function Dashboard() {
  const toast = useToast()
  const { user } = useAuth()
  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    dashboardApi.get().then(setData).catch((e) => toast.error(errMsg(e))).finally(() => setLoading(false))
  }, [])

  if (loading) return <Loading />
  if (!data) return null

  const chart = (data.revenueChart || []).map((d) => ({ label: d.day?.slice(5), revenue: Number(d.revenue || 0) }))
  const maxQty = Math.max(1, ...(data.topProducts || []).map((p) => p.quantitySold))

  return (
    <div>
      <PageHeader title={`Xin chào, ${user?.fullName} 👋`} subtitle="Tổng quan hoạt động kinh doanh hôm nay" />

      <Row className="g-3 mb-3 stagger">
        <Col xl={3} md={6}><StatCard icon="bi-cash-coin" chip="emerald" label="Doanh thu hôm nay" value={formatMoney(data.revenueToday)} /></Col>
        <Col xl={3} md={6}><StatCard icon="bi-calendar3" chip="sky" label="Doanh thu tháng này" value={formatMoney(data.revenueMonth)} /></Col>
        <Col xl={3} md={6}><StatCard icon="bi-receipt-cutoff" chip="violet" label="Hóa đơn hôm nay" value={data.invoiceCountToday} /></Col>
        <Col xl={3} md={6}><StatCard icon="bi-exclamation-triangle-fill" chip="amber" label="Mặt hàng tồn thấp" value={data.lowStockCount} hint="Cần nhập thêm hàng" /></Col>
      </Row>

      <Row className="g-3">
        <Col lg={7}>
          <Card className="border-0 h-100 fade-up">
            <Card.Body>
              <div className="d-flex justify-content-between align-items-center mb-1">
                <Card.Title className="fs-6 mb-0">Doanh thu 7 ngày gần nhất</Card.Title>
                <span className="pill pill-success"><i className="bi bi-graph-up"></i>Theo ngày</span>
              </div>
              {chart.length === 0 ? <EmptyState title="Chưa có giao dịch" /> : (
                <ResponsiveContainer width="100%" height={300}>
                  <AreaChart data={chart} margin={{ top: 16, right: 8 }}>
                    <defs>
                      <linearGradient id="dashRev" x1="0" y1="0" x2="0" y2="1">
                        <stop offset="0%" stopColor="#10b981" stopOpacity={0.4} />
                        <stop offset="100%" stopColor="#10b981" stopOpacity={0} />
                      </linearGradient>
                    </defs>
                    <CartesianGrid strokeDasharray="3 3" vertical={false} />
                    <XAxis dataKey="label" />
                    <YAxis tickFormatter={(v) => `${v / 1000}k`} />
                    <Tooltip formatter={(v) => formatMoney(v)} />
                    <Area type="monotone" dataKey="revenue" stroke="#059669" strokeWidth={2.5} fill="url(#dashRev)" />
                  </AreaChart>
                </ResponsiveContainer>
              )}
            </Card.Body>
          </Card>
        </Col>

        <Col lg={5}>
          <Card className="border-0 h-100 fade-up">
            <Card.Body>
              <Card.Title className="fs-6 mb-3">Top sản phẩm bán chạy (tháng)</Card.Title>
              {(data.topProducts || []).length === 0 ? <EmptyState title="Chưa có dữ liệu" /> : (
                <div className="d-flex flex-column gap-3">
                  {data.topProducts.map((p, i) => (
                    <div key={p.productId} className="d-flex align-items-center gap-3">
                      <span className={`stat-chip ${RANK_COLORS[i % RANK_COLORS.length]}`} style={{ width: 38, height: 38, fontSize: '.9rem', fontWeight: 700 }}>
                        {i + 1}
                      </span>
                      <div className="flex-grow-1 min-w-0">
                        <div className="d-flex justify-content-between">
                          <span className="fw-semibold text-truncate" style={{ maxWidth: 160 }}>{p.productName}</span>
                          <span className="num small text-muted2">{p.quantitySold} bán</span>
                        </div>
                        <div className="progress mt-1" style={{ height: 6, background: '#eef2f7' }}>
                          <div className="progress-bar" role="progressbar"
                            style={{ width: `${(p.quantitySold / maxQty) * 100}%`, background: 'linear-gradient(90deg,var(--brand-400),var(--brand-600))' }} />
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
