import { useEffect, useState } from 'react'
import { Card, Col, Row, Table } from 'react-bootstrap'
import PageHeader from '../../components/ui/PageHeader'
import InfoBanner from '../../components/ui/InfoBanner'
import EmptyState from '../../components/ui/EmptyState'
import Loading from '../../components/ui/Loading'
import { inventoryApi } from '../../api/misc'
import { useToast } from '../../context/ToastContext'
import { errMsg } from '../../api/client'
import { formatMoney } from '../../utils/format'

const ABC_COLOR = { A: 'success', B: 'warning', C: 'secondary' }
const XYZ_COLOR = { X: 'primary', Y: 'info', Z: 'danger' }
const ADVICE = {
  A: 'Doanh thu chủ lực — kiểm soát chặt, không để hết hàng.',
  B: 'Quan trọng vừa — theo dõi định kỳ.',
  C: 'Đuôi dài — nới lỏng, gom đơn để tiết kiệm chi phí đặt.',
}

export default function AbcXyz() {
  const toast = useToast()
  const [rows, setRows] = useState([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    inventoryApi.abcXyz().then(setRows).catch((e) => toast.error(errMsg(e))).finally(() => setLoading(false))
  }, [])

  if (loading) return <Loading />

  const count = (c, key) => rows.filter((r) => r[key] === c).length

  return (
    <div>
      <PageHeader title="Phân tích ABC / XYZ" subtitle="Phân loại sản phẩm theo doanh thu (Pareto) × độ biến động nhu cầu (90 ngày)" />

      <InfoBanner id="abcxyz" title="ABC/XYZ là gì?">
        <b>ABC</b> theo doanh thu luỹ kế: <b>A</b> = nhóm tạo ~80% doanh thu, <b>B</b> = 80–95%, <b>C</b> = đuôi còn lại.
        <b> XYZ</b> theo độ biến động nhu cầu (CV = σ/μ): <b>X</b> ổn định (&lt;0.5), <b>Y</b> dao động, <b>Z</b> thất thường (≥1.0).
        Nhóm <b>AX</b> (bán chạy & đều) cần đảm bảo luôn còn hàng; nhóm <b>CZ</b> (ít & thất thường) nên gom đơn, giảm tồn.
      </InfoBanner>

      <Row className="g-3 mb-3">
        {['A', 'B', 'C'].map((c) => (
          <Col md={4} key={c}>
            <Card className="border-0"><Card.Body className="d-flex align-items-center gap-3">
              <span className={`badge bg-${ABC_COLOR[c]}`} style={{ fontSize: '1.1rem', width: 40, height: 40, display: 'grid', placeItems: 'center' }}>{c}</span>
              <div><div className="fw-bold fs-5">{count(c, 'abcClass')} mặt hàng</div>
                <div className="text-muted2 small">{ADVICE[c]}</div></div>
            </Card.Body></Card>
          </Col>
        ))}
      </Row>

      <Card className="border-0">
        <div className="table-responsive" style={{ maxHeight: 540, overflowY: 'auto' }}>
          <Table hover className="mb-0 align-middle">
            <thead><tr>
              <th>Sản phẩm</th><th className="text-end">Doanh thu (90n)</th><th className="text-end">% DT</th>
              <th className="text-end">Luỹ kế</th><th className="text-center">ABC</th>
              <th className="text-end">Đã bán</th><th className="text-end">CV</th><th className="text-center">XYZ</th>
            </tr></thead>
            <tbody>
              {rows.map((r) => (
                <tr key={r.productId}>
                  <td className="fw-semibold">{r.name}</td>
                  <td className="text-end num">{formatMoney(r.revenue)}</td>
                  <td className="text-end num text-muted2">{r.revenueShare}%</td>
                  <td className="text-end num text-muted2">{r.cumulativeShare}%</td>
                  <td className="text-center"><span className={`badge bg-${ABC_COLOR[r.abcClass]}`}>{r.abcClass}</span></td>
                  <td className="text-end num">{r.soldQty}</td>
                  <td className="text-end num text-muted2">{r.cv}</td>
                  <td className="text-center"><span className={`badge bg-${XYZ_COLOR[r.xyzClass]}`}>{r.xyzClass}</span></td>
                </tr>
              ))}
              {rows.length === 0 && <tr><td colSpan={8}><EmptyState icon="bi-bar-chart" title="Chưa đủ dữ liệu bán hàng để phân tích" /></td></tr>}
            </tbody>
          </Table>
        </div>
      </Card>
    </div>
  )
}
