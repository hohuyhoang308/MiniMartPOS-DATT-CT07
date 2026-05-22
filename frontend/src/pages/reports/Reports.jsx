import { useEffect, useState } from 'react'
import { Button, Card, Col, Form, Row, Table } from 'react-bootstrap'
import {
  Area, AreaChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis,
} from 'recharts'
import PageHeader from '../../components/ui/PageHeader'
import StatCard from '../../components/ui/StatCard'
import StatusPill from '../../components/ui/StatusPill'
import EmptyState from '../../components/ui/EmptyState'
import Loading from '../../components/ui/Loading'
import { reportApi } from '../../api/misc'
import client from '../../api/client'
import { useToast } from '../../context/ToastContext'
import { errMsg } from '../../api/client'
import { formatMoney } from '../../utils/format'

function monthRange() {
  const now = new Date()
  const first = new Date(now.getFullYear(), now.getMonth(), 1)
  const iso = (d) => d.toISOString().slice(0, 10)
  return { from: iso(first), to: iso(now) }
}

export default function Reports() {
  const toast = useToast()
  const def = monthRange()
  const [from, setFrom] = useState(def.from)
  const [to, setTo] = useState(def.to)
  const [data, setData] = useState(null)
  const [shifts, setShifts] = useState([])
  const [loading, setLoading] = useState(true)
  const [exporting, setExporting] = useState(false)

  async function load() {
    setLoading(true)
    try {
      const [rev, sh] = await Promise.all([reportApi.revenue(from, to), reportApi.shifts()])
      setData(rev); setShifts(sh)
    } catch (e) { toast.error(errMsg(e)) } finally { setLoading(false) }
  }
  useEffect(() => { load() }, [])

  async function exportExcel() {
    setExporting(true)
    try {
      const res = await client.get(`/reports/export?type=excel&from=${from}&to=${to}`, { responseType: 'blob' })
      const url = URL.createObjectURL(res.data)
      const a = document.createElement('a')
      a.href = url; a.download = `bao-cao-doanh-thu-${from}_${to}.xlsx`; a.click()
      URL.revokeObjectURL(url)
      toast.success('Đã xuất file Excel')
    } catch (e) { toast.error(errMsg(e)) } finally { setExporting(false) }
  }

  const chart = (data?.days || []).map((d) => ({ label: d.day?.slice(5), revenue: Number(d.revenue || 0) }))
  const avg = data && data.totalInvoices > 0 ? data.totalRevenue / data.totalInvoices : 0

  return (
    <div>
      <PageHeader title="Báo cáo doanh thu" subtitle="Phân tích doanh thu theo thời gian & ca làm việc">
        <Button variant="soft" onClick={exportExcel} disabled={exporting}>
          <i className="bi bi-file-earmark-excel me-1"></i>{exporting ? 'Đang xuất…' : 'Xuất Excel'}
        </Button>
      </PageHeader>

      <Card className="border-0 mb-3">
        <Card.Body>
          <Row className="g-2 align-items-end">
            <Col md={3}><Form.Label>Từ ngày</Form.Label><Form.Control type="date" value={from} onChange={(e) => setFrom(e.target.value)} /></Col>
            <Col md={3}><Form.Label>Đến ngày</Form.Label><Form.Control type="date" value={to} onChange={(e) => setTo(e.target.value)} /></Col>
            <Col md="auto"><Button onClick={load}><i className="bi bi-funnel me-1"></i>Xem báo cáo</Button></Col>
          </Row>
        </Card.Body>
      </Card>

      {loading ? <Loading /> : (
        <>
          <Row className="g-3 mb-3 stagger">
            <Col md={4}><StatCard icon="bi-cash-stack" chip="emerald" label="Tổng doanh thu" value={formatMoney(data?.totalRevenue)} /></Col>
            <Col md={4}><StatCard icon="bi-receipt" chip="sky" label="Số hóa đơn" value={data?.totalInvoices} /></Col>
            <Col md={4}><StatCard icon="bi-graph-up-arrow" chip="violet" label="Trung bình / hóa đơn" value={formatMoney(avg)} /></Col>
          </Row>

          <Card className="border-0 mb-3">
            <Card.Body>
              <Card.Title className="fs-6 mb-3">Diễn biến doanh thu theo ngày</Card.Title>
              {chart.length === 0 ? <EmptyState title="Không có dữ liệu trong khoảng đã chọn" /> : (
                <ResponsiveContainer width="100%" height={320}>
                  <AreaChart data={chart}>
                    <defs>
                      <linearGradient id="revFill" x1="0" y1="0" x2="0" y2="1">
                        <stop offset="0%" stopColor="#10b981" stopOpacity={0.35} />
                        <stop offset="100%" stopColor="#10b981" stopOpacity={0} />
                      </linearGradient>
                    </defs>
                    <CartesianGrid strokeDasharray="3 3" vertical={false} />
                    <XAxis dataKey="label" />
                    <YAxis tickFormatter={(v) => `${v / 1000}k`} />
                    <Tooltip formatter={(v) => formatMoney(v)} />
                    <Area type="monotone" dataKey="revenue" stroke="#059669" strokeWidth={2.5} fill="url(#revFill)" />
                  </AreaChart>
                </ResponsiveContainer>
              )}
            </Card.Body>
          </Card>

          <Card className="border-0">
            <Card.Body className="pb-0"><Card.Title className="fs-6">Báo cáo theo ca / thu ngân</Card.Title></Card.Body>
            <Table hover className="mb-0">
              <thead><tr><th>Ca</th><th>Thu ngân</th><th className="text-end">Tiền đầu ca</th><th className="text-end">Doanh thu</th><th className="text-center">Số HĐ</th><th>Trạng thái</th></tr></thead>
              <tbody>
                {shifts.map((s) => (
                  <tr key={s.shiftId}>
                    <td className="fw-semibold">#{s.shiftId}</td>
                    <td>{s.cashierName}</td>
                    <td className="text-end num">{formatMoney(s.openingCash)}</td>
                    <td className="text-end num fw-semibold text-success">{formatMoney(s.totalSales)}</td>
                    <td className="text-center num">{s.invoiceCount}</td>
                    <td><StatusPill value={s.status} /></td>
                  </tr>
                ))}
                {shifts.length === 0 && <tr><td colSpan={6}><EmptyState icon="bi-clock-history" title="Chưa có ca làm việc" /></td></tr>}
              </tbody>
            </Table>
          </Card>
        </>
      )}
    </div>
  )
}
