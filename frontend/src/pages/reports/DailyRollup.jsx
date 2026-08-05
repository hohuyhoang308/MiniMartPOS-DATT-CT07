import { useEffect, useMemo, useState } from 'react'
import { Button, Card, Col, Form, Row, Table } from 'react-bootstrap'
import {
  CartesianGrid, Legend, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis,
} from 'recharts'
import InfoBanner from '../../components/ui/InfoBanner'
import StatCard from '../../components/ui/StatCard'
import EmptyState from '../../components/ui/EmptyState'
import Loading from '../../components/ui/Loading'
import { reportApi } from '../../api/misc'
import { useToast } from '../../context/ToastContext'
import { errMsg } from '../../api/client'
import { formatMoney, monthRange } from '../../utils/format'
import { REVENUE_STROKE, PROFIT_COLOR } from '../../constants/chartColors'

const dayLabel = (d) => (d ? d.slice(5).split('-').reverse().join('/') : '') // 2026-06-09 → 09/06

/**
 * Báo cáo TỔNG HỢP NGÀY (daily rollup) — số liệu doanh thu/lợi nhuận đã được kết toán sẵn theo ngày,
 * truy vấn rất nhanh (không tính lại từ hóa đơn). Nhúng trong trang Báo cáo.
 */
export default function DailyRollup() {
  const toast = useToast()
  const def = monthRange()
  const [from, setFrom] = useState(def.from)
  const [to, setTo] = useState(def.to)
  const [rows, setRows] = useState([])
  const [loading, setLoading] = useState(true)

  function load(f = from, t = to) {
    setLoading(true)
    reportApi.dailyRollup(f, t).then(setRows).catch((e) => toast.error(errMsg(e))).finally(() => setLoading(false))
  }
  useEffect(() => { load() }, [])

  // Gộp theo ngày (phòng khi nhiều cửa hàng cùng trả về) và sắp xếp tăng dần để biểu đồ liền mạch.
  const chart = useMemo(() => {
    const byDate = new Map()
    for (const r of rows) {
      const cur = byDate.get(r.salesDate) || { label: dayLabel(r.salesDate), salesDate: r.salesDate, revenue: 0, grossProfit: 0 }
      cur.revenue += Number(r.revenue || 0)
      cur.grossProfit += Number(r.grossProfit || 0)
      byDate.set(r.salesDate, cur)
    }
    return [...byDate.values()].sort((a, b) => a.salesDate.localeCompare(b.salesDate))
  }, [rows])

  const totals = useMemo(() => rows.reduce((s, r) => ({
    revenue: s.revenue + Number(r.revenue || 0),
    grossProfit: s.grossProfit + Number(r.grossProfit || 0),
    invoiceCount: s.invoiceCount + Number(r.invoiceCount || 0),
    itemsSold: s.itemsSold + Number(r.itemsSold || 0),
  }), { revenue: 0, grossProfit: 0, invoiceCount: 0, itemsSold: 0 }), [rows])

  return (
    <div>
      <InfoBanner id="daily-rollup" title="Báo cáo tổng hợp ngày">
        Đây là báo cáo <b>tổng hợp nhanh</b>: mỗi ngày bán hàng được <b>kết toán sẵn</b> (doanh thu, lợi nhuận gộp,
        giá vốn, thuế, số hóa đơn, số hàng bán) nên xem lại nhanh hơn nhiều so với tính lại từ từng hóa đơn.
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
            <Col md={3}><StatCard icon="bi-cash-stack" chip="emerald" label="Tổng doanh thu" value={formatMoney(totals.revenue)} /></Col>
            <Col md={3}><StatCard icon="bi-graph-up-arrow" chip="violet" label="Tổng lợi nhuận gộp" value={formatMoney(totals.grossProfit)} /></Col>
            <Col md={3}><StatCard icon="bi-receipt" chip="sky" label="Số hóa đơn" value={totals.invoiceCount} /></Col>
            <Col md={3}><StatCard icon="bi-basket3" chip="amber" label="Số hàng đã bán" value={totals.itemsSold} /></Col>
          </Row>

          <Card className="border-0 mb-3">
            <Card.Body>
              <Card.Title className="fs-6 mb-3">Doanh thu & lợi nhuận gộp theo ngày</Card.Title>
              {chart.length === 0 ? <EmptyState title="Chưa có dữ liệu trong khoảng thời gian đã chọn" /> : (
                <ResponsiveContainer width="100%" height={340}>
                  <LineChart data={chart}>
                    <CartesianGrid strokeDasharray="3 3" vertical={false} />
                    <XAxis dataKey="label" />
                    <YAxis tickFormatter={(v) => `${v / 1000}k`} />
                    <Tooltip cursor={false} formatter={(v, n) => [formatMoney(v), n === 'revenue' ? 'Doanh thu' : 'Lợi nhuận gộp']} />
                    <Legend formatter={(v) => (v === 'revenue' ? 'Doanh thu' : 'Lợi nhuận gộp')} />
                    <Line type="monotone" dataKey="revenue" stroke={REVENUE_STROKE} strokeWidth={2.5} dot={false} />
                    <Line type="monotone" dataKey="grossProfit" stroke={PROFIT_COLOR} strokeWidth={2.5} dot={false} />
                  </LineChart>
                </ResponsiveContainer>
              )}
            </Card.Body>
          </Card>

          <div className="table-wrap">
            <Table hover className="mb-0 align-middle">
              <thead><tr>
                <th>Ngày</th>
                <th className="text-end">Doanh thu</th>
                <th className="text-end">Lợi nhuận gộp</th>
                <th className="text-end">Giá vốn</th>
                <th className="text-end">Thuế</th>
                <th className="text-center">Số hóa đơn</th>
                <th className="text-center">Hàng đã bán</th>
              </tr></thead>
              <tbody>
                {[...rows].sort((a, b) => b.salesDate.localeCompare(a.salesDate)).map((r) => (
                  <tr key={`${r.storeId}-${r.salesDate}`}>
                    <td className="fw-semibold">{dayLabel(r.salesDate)}</td>
                    <td className="text-end num fw-semibold text-success">{formatMoney(r.revenue)}</td>
                    <td className="text-end num fw-semibold text-secondary">{formatMoney(r.grossProfit)}</td>
                    <td className="text-end num text-muted2">{formatMoney(r.cogs)}</td>
                    <td className="text-end num text-muted2">{formatMoney(r.tax)}</td>
                    <td className="text-center num">{r.invoiceCount}</td>
                    <td className="text-center num">{r.itemsSold}</td>
                  </tr>
                ))}
                {rows.length === 0 && <tr><td colSpan={7}><EmptyState icon="bi-calendar-week" title="Chưa có dữ liệu trong khoảng đã chọn" /></td></tr>}
              </tbody>
            </Table>
          </div>
        </>
      )}
    </div>
  )
}
