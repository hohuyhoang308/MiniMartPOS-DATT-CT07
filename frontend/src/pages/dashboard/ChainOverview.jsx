import { useMemo } from 'react'
import { Card, Col, Row, Table } from 'react-bootstrap'
import { useNavigate } from 'react-router-dom'
import {
  Bar, BarChart, CartesianGrid, Cell, ResponsiveContainer, Tooltip, XAxis, YAxis,
} from 'recharts'
import PageHeader from '../../components/ui/PageHeader'
import InfoBanner from '../../components/ui/InfoBanner'
import StatCard from '../../components/ui/StatCard'
import StatusPill from '../../components/ui/StatusPill'
import EmptyState from '../../components/ui/EmptyState'
import Loading from '../../components/ui/Loading'
import { dashboardApi } from '../../api/misc'
import { useStoreScope } from '../../context/StoreScopeContext'
import { formatMoney } from '../../utils/format'
import { useList } from '../../hooks/useList'

const BAR_COLORS = ['#10b981', '#0ea5e9', '#8b5cf6', '#f59e0b', '#f43f5e', '#14b8a6', '#6366f1']

/**
 * SO SÁNH CHI NHÁNH (toàn chuỗi) — chỉ quản trị viên toàn chuỗi (ADMIN).
 * Gộp KPI toàn chuỗi + xếp hạng từng cửa hàng theo doanh thu tháng, kèm cảnh báo tồn kho.
 * Bấm 1 cửa hàng → chọn phạm vi cửa hàng đó và mở Tổng quan để đi sâu.
 */
export default function ChainOverview() {
  const navigate = useNavigate()
  const { setScopeStoreId } = useStoreScope()
  const { data: rows, loading } = useList(dashboardApi.storeComparison)

  const totals = useMemo(() => rows.reduce((a, r) => ({
    revenueToday: a.revenueToday + Number(r.revenueToday || 0),
    revenueMonth: a.revenueMonth + Number(r.revenueMonth || 0),
    profitMonth: a.profitMonth + Number(r.profitMonth || 0),
    invoicesToday: a.invoicesToday + Number(r.invoicesToday || 0),
    lowStock: a.lowStock + Number(r.lowStock || 0),
    outOfStock: a.outOfStock + Number(r.outOfStock || 0),
  }), { revenueToday: 0, revenueMonth: 0, profitMonth: 0, invoicesToday: 0, lowStock: 0, outOfStock: 0 }), [rows])

  const chart = rows.map((r) => ({ name: r.code, revenue: Number(r.revenueMonth || 0) }))
  const maxRev = Math.max(1, ...rows.map((r) => Number(r.revenueMonth || 0)))

  /** Đi sâu vào 1 cửa hàng: đặt phạm vi rồi mở Tổng quan của cửa hàng đó. */
  function drillInto(storeId) {
    setScopeStoreId(String(storeId))
    navigate('/dashboard')
  }

  if (loading) return <Loading />

  return (
    <div>
      <PageHeader title="So sánh chi nhánh" subtitle="Xếp hạng và đối chiếu hiệu suất các cửa hàng trong chuỗi" />

      <InfoBanner id="chain-overview" title="Cách dùng">
        Trang này gộp số liệu <b>toàn chuỗi</b> và so sánh từng cửa hàng theo doanh thu, lợi nhuận và cảnh báo
        tồn kho. Bấm vào một <b>cửa hàng</b> để chuyển phạm vi xem sang cửa hàng đó và mở trang Tổng quan chi tiết.
        Bạn cũng có thể đổi phạm vi bất cứ lúc nào ở phù hiệu góc trên bên phải.
      </InfoBanner>

      <Row className="g-3 mb-3 stagger">
        <Col xl={3} md={6}><StatCard icon="bi-shop" chip="sky" label="Số chi nhánh" value={rows.length} /></Col>
        <Col xl={3} md={6}><StatCard icon="bi-cash-coin" chip="emerald" label="Doanh thu hôm nay (chuỗi)" value={formatMoney(totals.revenueToday)} /></Col>
        <Col xl={3} md={6}><StatCard icon="bi-graph-up-arrow" chip="violet" label="Lợi nhuận tháng (chuỗi)" value={formatMoney(totals.profitMonth)} /></Col>
        <Col xl={3} md={6}><StatCard icon="bi-receipt" chip="amber" label="Hóa đơn hôm nay (chuỗi)" value={totals.invoicesToday} /></Col>
      </Row>

      <Row className="g-3 mb-3">
        <Col lg={5}>
          <Card className="border-0 h-100">
            <Card.Body>
              <Card.Title className="fs-6 mb-3">Doanh thu tháng theo chi nhánh</Card.Title>
              {chart.length === 0 ? <EmptyState title="Chưa có chi nhánh nào" /> : (
                <ResponsiveContainer width="100%" height={300}>
                  <BarChart data={chart} layout="vertical" margin={{ left: 10 }}>
                    <CartesianGrid strokeDasharray="3 3" horizontal={false} />
                    <XAxis type="number" tickFormatter={(v) => `${v / 1000}k`} />
                    <YAxis type="category" dataKey="name" width={70} tick={{ fontSize: 12 }} />
                    <Tooltip cursor={false} formatter={(v) => formatMoney(v)} />
                    <Bar dataKey="revenue" radius={[0, 5, 5, 0]} barSize={22} activeBar={false}>
                      {chart.map((c, i) => <Cell key={i} fill={BAR_COLORS[i % BAR_COLORS.length]} />)}
                    </Bar>
                  </BarChart>
                </ResponsiveContainer>
              )}
            </Card.Body>
          </Card>
        </Col>
        <Col lg={7}>
          <Card className="border-0 h-100">
            <Card.Body className="pb-0"><Card.Title className="fs-6">Bảng so sánh chi nhánh</Card.Title></Card.Body>
            <div className="table-responsive">
              <Table hover className="mb-0 align-middle">
                <thead><tr>
                  <th>Chi nhánh</th>
                  <th className="text-end">DT hôm nay</th>
                  <th className="text-end">DT tháng</th>
                  <th className="text-end">LN tháng</th>
                  <th className="text-center">HĐ hôm nay</th>
                  <th className="text-center" title="Số mặt hàng tồn thấp / đã hết">Cảnh báo kho</th>
                  <th></th>
                </tr></thead>
                <tbody>
                  {rows.map((r, i) => (
                    <tr key={r.storeId} className="cursor-pointer" onClick={() => drillInto(r.storeId)} title="Xem chi tiết cửa hàng">
                      <td>
                        <div className="d-flex align-items-center gap-2">
                          <span style={{ width: 10, height: 10, borderRadius: '50%', flex: '0 0 auto', background: BAR_COLORS[i % BAR_COLORS.length] }}></span>
                          <div>
                            <div className="fw-semibold">{r.code} — {r.name}</div>
                            {r.status === 'INACTIVE' && <StatusPill value="INACTIVE" />}
                          </div>
                        </div>
                      </td>
                      <td className="text-end num">{formatMoney(r.revenueToday)}</td>
                      <td className="text-end num fw-semibold">
                        {formatMoney(r.revenueMonth)}
                        <div className="progress mt-1" style={{ height: 4 }}>
                          <div className="progress-bar bg-success" style={{ width: `${(Number(r.revenueMonth || 0) / maxRev) * 100}%` }} />
                        </div>
                      </td>
                      <td className="text-end num text-success">{formatMoney(r.profitMonth)}</td>
                      <td className="text-center num">{r.invoicesToday}</td>
                      <td className="text-center">
                        {r.outOfStock > 0 && <span className="pill pill-danger me-1" title="Mặt hàng đã hết"><i className="bi bi-x-octagon-fill"></i>{r.outOfStock}</span>}
                        {r.lowStock > 0 && <span className="pill pill-warning" title="Mặt hàng tồn thấp"><i className="bi bi-exclamation-triangle-fill"></i>{r.lowStock}</span>}
                        {r.outOfStock === 0 && r.lowStock === 0 && <span className="pill pill-success"><i className="bi bi-check-circle-fill"></i></span>}
                      </td>
                      <td className="text-end"><i className="bi bi-chevron-right text-muted2"></i></td>
                    </tr>
                  ))}
                  {rows.length === 0 && <tr><td colSpan={7}><EmptyState icon="bi-shop" title="Chưa có chi nhánh nào" /></td></tr>}
                </tbody>
              </Table>
            </div>
          </Card>
        </Col>
      </Row>
    </div>
  )
}
