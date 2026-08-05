import { useEffect, useState } from 'react'
import { Button, ButtonGroup, Card, Col, Form, Row, Table } from 'react-bootstrap'
import {
  Area, CartesianGrid, ComposedChart, Legend, Line, ResponsiveContainer, Tooltip, XAxis, YAxis,
} from 'recharts'
import InfoBanner from '../../components/ui/InfoBanner'
import StatCard from '../../components/ui/StatCard'
import StatusPill from '../../components/ui/StatusPill'
import EmptyState from '../../components/ui/EmptyState'
import Loading from '../../components/ui/Loading'
import DiffBadge from '../../components/ui/DiffBadge'
import { reportApi } from '../../api/misc'
import { useToast } from '../../context/ToastContext'
import { errMsg } from '../../api/client'
import { formatMoney, iso, monthRange } from '../../utils/format'
import { downloadBlob } from '../../utils/download'
import { REVENUE_FILL, REVENUE_STROKE, PROFIT_COLOR } from '../../constants/chartColors'

const GROUPS = [
  { key: 'DAY', label: 'Ngày' },
  { key: 'WEEK', label: 'Tuần' },
  { key: 'MONTH', label: 'Tháng' },
  { key: 'YEAR', label: 'Năm' },
]

/** Rút gọn nhãn kỳ từ chuỗi bucket backend trả về theo từng cách gộp. */
function formatLabel(groupBy, bucket) {
  if (!bucket) return ''
  switch (groupBy) {
    case 'DAY': return bucket.slice(5).split('-').reverse().join('/') // 2026-06-09 → 09/06
    case 'WEEK': return 'T' + (bucket.split('-W')[1] || bucket)        // 2026-W23 → T23
    case 'MONTH': { const [y, m] = bucket.split('-'); return `${m}/${y}` }
    default: return bucket                                            // YEAR: 2026
  }
}

/** Tuần ISO-8601 (khớp DATE_FORMAT '%x-W%v' của MySQL): {năm ISO, tuần}. */
function isoWeek(date) {
  const d = new Date(Date.UTC(date.getFullYear(), date.getMonth(), date.getDate()))
  const dayNum = (d.getUTCDay() + 6) % 7         // Thứ 2 = 0 … Chủ nhật = 6
  d.setUTCDate(d.getUTCDate() - dayNum + 3)      // về Thứ 5 cùng tuần
  const isoYear = d.getUTCFullYear()
  const firstThu = new Date(Date.UTC(isoYear, 0, 4))
  const firstDayNum = (firstThu.getUTCDay() + 6) % 7
  firstThu.setUTCDate(firstThu.getUTCDate() - firstDayNum + 3)
  const week = 1 + Math.round((d - firstThu) / (7 * 86400000))
  return { isoYear, week }
}

/** Khoá kỳ cho 1 ngày, đúng định dạng bucket backend trả về theo cách gộp. */
function bucketKey(groupBy, d) {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  switch (groupBy) {
    case 'MONTH': return `${y}-${m}`
    case 'YEAR': return `${y}`
    case 'WEEK': { const w = isoWeek(d); return `${w.isoYear}-W${String(w.week).padStart(2, '0')}` }
    default: return `${y}-${m}-${day}`
  }
}

/**
 * Dựng ĐỦ chuỗi kỳ giữa [from, to] theo cách gộp, điền 0 cho kỳ không có giao dịch.
 * Backend chỉ trả các kỳ CÓ hóa đơn → nếu không điền, biểu đồ chỉ là vài chấm rời.
 */
function buildSeries(from, to, groupBy, points) {
  if (!from || !to) return []
  const byKey = new Map((points || []).map((p) => [p.label, p]))
  const out = []
  const seen = new Set()
  const cur = new Date(`${from}T00:00:00`)
  const end = new Date(`${to}T00:00:00`)
  let guard = 0
  while (cur <= end && guard++ < 6000) {
    const key = bucketKey(groupBy, cur)
    if (!seen.has(key)) {
      seen.add(key)
      const p = byKey.get(key)
      out.push({ label: formatLabel(groupBy, key), revenue: Number(p?.revenue || 0), profit: Number(p?.profit || 0) })
    }
    cur.setDate(cur.getDate() + 1)
  }
  return out
}

/** Báo cáo doanh thu/lợi nhuận + đối soát quỹ theo ca (nhúng trong trang Báo cáo). */
export default function RevenueReport() {
  const toast = useToast()
  const def = monthRange()
  const [from, setFrom] = useState(def.from)
  const [to, setTo] = useState(def.to)
  const [groupBy, setGroupBy] = useState('DAY')
  const [data, setData] = useState(null)
  const [applied, setApplied] = useState({ from: def.from, to: def.to, groupBy: 'DAY' })
  const [shifts, setShifts] = useState([])
  const [loading, setLoading] = useState(true)
  const [exporting, setExporting] = useState(false)

  async function load(overrides = {}) {
    const f = overrides.from ?? from
    const t = overrides.to ?? to
    const g = overrides.groupBy ?? groupBy
    setLoading(true)
    try {
      const [rev, sh] = await Promise.all([reportApi.revenue(f, t, g), reportApi.shifts()])
      setData(rev); setShifts(sh); setApplied({ from: f, to: t, groupBy: g })
    } catch (e) { toast.error(errMsg(e)) } finally { setLoading(false) }
  }
  useEffect(() => { load() }, [])

  /** Khoảng nhanh: đặt from/to + cách gộp hợp lý rồi tải lại. */
  function quickRange(kind) {
    const now = new Date()
    let f, t = iso(now), g = groupBy
    if (kind === '7d') { f = iso(new Date(now.getFullYear(), now.getMonth(), now.getDate() - 6)); g = 'DAY' }
    else if (kind === '30d') { f = iso(new Date(now.getFullYear(), now.getMonth(), now.getDate() - 29)); g = 'DAY' }
    else if (kind === 'month') { f = iso(new Date(now.getFullYear(), now.getMonth(), 1)); g = 'DAY' }
    else if (kind === 'year') { f = iso(new Date(now.getFullYear(), 0, 1)); g = 'MONTH' }
    else if (kind === '12m') { f = iso(new Date(now.getFullYear() - 1, now.getMonth(), 1)); g = 'MONTH' }
    setFrom(f); setTo(t); setGroupBy(g)
    load({ from: f, to: t, groupBy: g })
  }

  function changeGroup(g) {
    setGroupBy(g)
    load({ groupBy: g })
  }

  async function exportExcel() {
    setExporting(true)
    try {
      downloadBlob(await reportApi.exportExcel(from, to, groupBy), `bao-cao-${groupBy.toLowerCase()}-${from}_${to}.xlsx`)
      toast.success('Đã xuất file Excel')
    } catch (e) { toast.error(errMsg(e)) } finally { setExporting(false) }
  }

  // Dựng đủ chuỗi kỳ theo đúng tham số ĐÃ TẢI (applied) để biểu đồ liền mạch, không bị vài chấm rời.
  const chart = buildSeries(applied.from, applied.to, applied.groupBy, data?.points)
  const avg = data && data.totalInvoices > 0 ? data.totalRevenue / data.totalInvoices : 0
  const groupLabel = GROUPS.find((g) => g.key === applied.groupBy)?.label || 'Ngày'
  const exportBtn = (
    <Button variant="soft" onClick={exportExcel} disabled={exporting}>
      <i className="bi bi-file-earmark-excel me-1"></i>{exporting ? 'Đang xuất…' : 'Xuất Excel'}
    </Button>
  )

  return (
    <div>
      <InfoBanner id="reports" title="Cách xem báo cáo">
        Chọn <b>khoảng thời gian</b> và <b>cách xem</b> (theo ngày, tuần, tháng hay năm) rồi bấm "Xem báo cáo".
        <b> Lợi nhuận</b> là tiền bán được trừ đi tiền vốn của số hàng đã bán. Bảng bên dưới cho biết doanh thu
        và việc kiểm tiền của từng ca, từng thu ngân. Bấm <b>Xuất Excel</b> để tải báo cáo về máy.
      </InfoBanner>

      <Card className="border-0 mb-3">
        <Card.Body>
          <Row className="g-2 align-items-end">
            <Col md={3}><Form.Label>Từ ngày</Form.Label><Form.Control type="date" value={from} onChange={(e) => setFrom(e.target.value)} /></Col>
            <Col md={3}><Form.Label>Đến ngày</Form.Label><Form.Control type="date" value={to} onChange={(e) => setTo(e.target.value)} /></Col>
            <Col md={3}>
              <Form.Label>Xem theo</Form.Label>
              <Form.Select value={groupBy} onChange={(e) => changeGroup(e.target.value)}>
                {GROUPS.map((g) => <option key={g.key} value={g.key}>{g.label}</option>)}
              </Form.Select>
            </Col>
            <Col md="auto"><Button onClick={() => load()}><i className="bi bi-funnel me-1"></i>Xem báo cáo</Button></Col>
            <Col md="auto">{exportBtn}</Col>
          </Row>
          <div className="mt-3 d-flex flex-wrap gap-2">
            <span className="text-muted2 small align-self-center me-1">Khoảng nhanh:</span>
            <ButtonGroup size="sm">
              <Button variant="outline-secondary" onClick={() => quickRange('7d')}>7 ngày</Button>
              <Button variant="outline-secondary" onClick={() => quickRange('30d')}>30 ngày</Button>
              <Button variant="outline-secondary" onClick={() => quickRange('month')}>Tháng này</Button>
              <Button variant="outline-secondary" onClick={() => quickRange('12m')}>12 tháng</Button>
              <Button variant="outline-secondary" onClick={() => quickRange('year')}>Năm nay</Button>
            </ButtonGroup>
          </div>
        </Card.Body>
      </Card>

      {loading ? <Loading /> : (
        <>
          <Row className="g-3 mb-3 stagger">
            <Col md={3}><StatCard icon="bi-cash-stack" chip="emerald" label="Tổng doanh thu" value={formatMoney(data?.totalRevenue)} /></Col>
            <Col md={3}><StatCard icon="bi-graph-up-arrow" chip="violet" label="Tổng lợi nhuận" value={formatMoney(data?.totalProfit)} /></Col>
            <Col md={3}><StatCard icon="bi-receipt" chip="sky" label="Số hóa đơn" value={data?.totalInvoices} /></Col>
            <Col md={3}><StatCard icon="bi-basket3" chip="amber" label="Trung bình / hóa đơn" value={formatMoney(avg)} /></Col>
          </Row>

          <Card className="border-0 mb-3">
            <Card.Body>
              <Card.Title className="fs-6 mb-3">Doanh thu & lợi nhuận theo {groupLabel.toLowerCase()}</Card.Title>
              {chart.length === 0 ? <EmptyState title="Chưa có dữ liệu trong khoảng thời gian đã chọn" /> : (
                <ResponsiveContainer width="100%" height={340}>
                  <ComposedChart data={chart}>
                    <defs>
                      <linearGradient id="revFill" x1="0" y1="0" x2="0" y2="1">
                        <stop offset="0%" stopColor={REVENUE_FILL} stopOpacity={0.35} />
                        <stop offset="100%" stopColor={REVENUE_FILL} stopOpacity={0} />
                      </linearGradient>
                    </defs>
                    <CartesianGrid strokeDasharray="3 3" vertical={false} />
                    <XAxis dataKey="label" />
                    <YAxis tickFormatter={(v) => `${v / 1000}k`} />
                    <Tooltip cursor={false} formatter={(v, n) => [formatMoney(v), n === 'revenue' ? 'Doanh thu' : 'Lợi nhuận']} />
                    <Legend formatter={(v) => (v === 'revenue' ? 'Doanh thu' : 'Lợi nhuận')} />
                    <Area type="monotone" dataKey="revenue" stroke={REVENUE_STROKE} strokeWidth={2.5} fill="url(#revFill)" />
                    <Line type="monotone" dataKey="profit" stroke={PROFIT_COLOR} strokeWidth={2.5} dot={false} />
                  </ComposedChart>
                </ResponsiveContainer>
              )}
            </Card.Body>
          </Card>

          <Card className="border-0">
            <Card.Body className="pb-0"><Card.Title className="fs-6">Doanh thu & kiểm tiền theo từng ca, từng thu ngân</Card.Title></Card.Body>
            <div className="table-responsive">
              <Table hover className="mb-0">
                <thead><tr>
                  <th>Ca</th><th>Thu ngân</th>
                  <th className="text-end" title="Tiền có sẵn trong két khi bắt đầu ca">Tiền đầu ca</th>
                  <th className="text-end" title="Tiền mặt khách trả trong ca">Tiền mặt thu được</th>
                  <th className="text-end" title="Số tiền đáng lẽ phải có trong két: tiền đầu ca cộng tiền mặt bán được">Đáng lẽ có</th>
                  <th className="text-end" title="Số tiền đếm được thực tế khi đóng ca">Đếm thực tế</th>
                  <th className="text-end" title="Chênh giữa tiền đếm thực tế và số đáng lẽ có">Thừa/Thiếu</th>
                  <th className="text-end">Doanh thu</th>
                  <th className="text-center">Số hóa đơn</th>
                  <th>Trạng thái</th>
                </tr></thead>
                <tbody>
                  {shifts.map((s) => (
                    <tr key={s.shiftId}>
                      <td className="fw-semibold">#{s.shiftId}</td>
                      <td>{s.cashierName}</td>
                      <td className="text-end num">{formatMoney(s.openingCash)}</td>
                      <td className="text-end num">{formatMoney(s.cashSales)}</td>
                      <td className="text-end num">{formatMoney(s.expectedCash)}</td>
                      <td className="text-end num">{s.closingCash != null ? formatMoney(s.closingCash) : '—'}</td>
                      <td className="text-end num">{s.cashDifference != null ? <DiffBadge value={s.cashDifference} /> : '—'}</td>
                      <td className="text-end num fw-semibold text-success">{formatMoney(s.totalSales)}</td>
                      <td className="text-center num">{s.invoiceCount}</td>
                      <td><StatusPill value={s.status} /></td>
                    </tr>
                  ))}
                  {shifts.length === 0 && <tr><td colSpan={10}><EmptyState icon="bi-clock-history" title="Chưa có ca làm việc nào" /></td></tr>}
                </tbody>
              </Table>
            </div>
          </Card>
        </>
      )}
    </div>
  )
}
