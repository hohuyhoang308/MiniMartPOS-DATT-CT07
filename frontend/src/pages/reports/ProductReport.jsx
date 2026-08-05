import { useEffect, useMemo, useState } from 'react'
import { Button, Card, Col, Form, Row, Table } from 'react-bootstrap'
import InfoBanner from '../../components/ui/InfoBanner'
import StatCard from '../../components/ui/StatCard'
import EmptyState from '../../components/ui/EmptyState'
import Loading from '../../components/ui/Loading'
import { reportApi } from '../../api/misc'
import { useToast } from '../../context/ToastContext'
import { errMsg } from '../../api/client'
import { formatMoney, monthRange } from '../../utils/format'

/** Màu biên lợi nhuận: thấp (đỏ) → cao (xanh). */
function marginCls(m) {
  if (m == null) return 'text-muted2'
  if (m < 5) return 'text-danger'
  if (m < 15) return 'text-warning'
  return 'text-success'
}

export default function ProductReport() {
  const toast = useToast()
  const def = monthRange()
  const [from, setFrom] = useState(def.from)
  const [to, setTo] = useState(def.to)
  const [q, setQ] = useState('')
  const [data, setData] = useState({ products: [], deadStock: [] })
  const [loading, setLoading] = useState(true)

  function load(f = from, t = to) {
    setLoading(true)
    reportApi.products(f, t).then((d) => setData(d || { products: [], deadStock: [] }))
      .catch((e) => toast.error(errMsg(e))).finally(() => setLoading(false))
  }
  useEffect(() => { load() }, [])

  const products = useMemo(() => {
    const kw = q.trim().toLowerCase()
    return kw ? data.products.filter((p) => p.productName.toLowerCase().includes(kw)) : data.products
  }, [data, q])

  const stats = useMemo(() => {
    const revenue = data.products.reduce((s, p) => s + Number(p.revenue || 0), 0)
    const profit = data.products.reduce((s, p) => s + Number(p.profit || 0), 0)
    const margin = revenue > 0 ? (profit / revenue) * 100 : 0
    return { profit, margin, dead: data.deadStock.length }
  }, [data])

  return (
    <div>
      <InfoBanner id="prod-report" title="Trang này dùng để làm gì?">
        Xem mỗi mặt hàng bán được bao nhiêu, <b>lãi bao nhiêu</b> và <b>biên lợi nhuận</b> (lãi trên doanh thu) —
        để biết hàng nào lãi tốt, hàng nào lãi mỏng. Phần <b>Hàng bán chậm</b> liệt kê sản phẩm <b>còn tồn nhưng
        không bán được</b> trong khoảng đã chọn (đọng vốn, nên cân nhắc giảm giá/đẩy hàng).
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
            <Col md={4}><StatCard icon="bi-graph-up-arrow" chip="violet" label="Tổng lợi nhuận" value={formatMoney(stats.profit)} /></Col>
            <Col md={4}><StatCard icon="bi-percent" chip="emerald" label="Biên lợi nhuận TB" value={`${stats.margin.toFixed(1)}%`} /></Col>
            <Col md={4}><StatCard icon="bi-box-seam" chip="amber" label="Mặt hàng bán chậm" value={stats.dead} /></Col>
          </Row>

          <Card className="border-0 mb-4">
            <Card.Body className="pb-0 d-flex justify-content-between align-items-center flex-wrap gap-2">
              <Card.Title className="fs-6 mb-0">Lợi nhuận theo mặt hàng</Card.Title>
              <div className="input-group" style={{ maxWidth: 280 }}>
                <span className="input-group-text"><i className="bi bi-search"></i></span>
                <Form.Control size="sm" placeholder="Tìm sản phẩm…" value={q} onChange={(e) => setQ(e.target.value)} />
              </div>
            </Card.Body>
            <div className="table-responsive" style={{ maxHeight: 460, overflowY: 'auto' }}>
              <Table hover className="mb-0 align-middle">
                <thead><tr>
                  <th>Sản phẩm</th><th>Nhóm</th>
                  <th className="text-center">SL bán</th>
                  <th className="text-end">Doanh thu</th>
                  <th className="text-end">Giá vốn</th>
                  <th className="text-end">Lợi nhuận</th>
                  <th className="text-end">Biên</th>
                </tr></thead>
                <tbody>
                  {products.map((p) => (
                    <tr key={p.productId}>
                      <td className="fw-semibold">{p.productName}</td>
                      <td className="text-muted2 small">{p.categoryName || '—'}</td>
                      <td className="text-center num">{p.qtySold}</td>
                      <td className="text-end num">{formatMoney(p.revenue)}</td>
                      <td className="text-end num text-muted2">{formatMoney(p.cogs)}</td>
                      <td className="text-end num fw-semibold text-success">{formatMoney(p.profit)}</td>
                      <td className={`text-end num fw-semibold ${marginCls(Number(p.marginPercent))}`}>{Number(p.marginPercent).toFixed(1)}%</td>
                    </tr>
                  ))}
                  {products.length === 0 && <tr><td colSpan={7}><EmptyState icon="bi-box-seam" title="Chưa có hàng nào bán được trong khoảng đã chọn" /></td></tr>}
                </tbody>
              </Table>
            </div>
          </Card>

          <h6 className="fw-bold mb-2"><i className="bi bi-hourglass-split me-2"></i>Hàng bán chậm <span className="text-muted2 fw-normal small">— còn tồn nhưng không bán được đơn vị nào trong khoảng</span></h6>
          <div className="table-wrap">
            <Table hover className="mb-0 align-middle">
              <thead><tr><th>Sản phẩm</th><th>Mã vạch</th><th className="text-center">Tồn hiện tại</th></tr></thead>
              <tbody>
                {data.deadStock.map((p) => (
                  <tr key={p.productId}>
                    <td className="fw-semibold">{p.name}</td>
                    <td className="text-muted2 small">{p.barcode}</td>
                    <td className="text-center num text-warning fw-semibold">{p.currentStock}</td>
                  </tr>
                ))}
                {data.deadStock.length === 0 && <tr><td colSpan={3}><EmptyState icon="bi-check2-circle" title="Không có hàng tồn bán chậm — mọi mặt hàng còn tồn đều có bán" /></td></tr>}
              </tbody>
            </Table>
          </div>
        </>
      )}
    </div>
  )
}
