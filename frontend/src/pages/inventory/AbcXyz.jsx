import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Button, Card, Col, Row, Table } from 'react-bootstrap'
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
  A: 'Nhóm bán chạy, mang về phần lớn doanh thu. Luôn giữ đủ hàng, đừng để hết.',
  B: 'Nhóm trung bình. Thỉnh thoảng xem lại để nhập cho hợp lý.',
  C: 'Nhóm bán ít. Nhập ít thôi và gom đơn cho đỡ tốn.',
}
/** Ý nghĩa dễ hiểu của nhóm XYZ (hiện khi rê chuột vào nhãn). */
const XYZ_MEAN = {
  X: 'Bán đều đặn mỗi tuần. Dễ đoán, ít cần hàng dự phòng.',
  Y: 'Bán lúc nhiều lúc ít. Nên theo dõi thường xuyên.',
  Z: 'Bán thất thường, khó đoán. Cần để nhiều hàng dự phòng hơn.',
}

export default function AbcXyz({ embedded = false }) {
  const toast = useToast()
  const navigate = useNavigate()
  const [rows, setRows] = useState([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    inventoryApi.abcXyz().then(setRows).catch((e) => toast.error(errMsg(e))).finally(() => setLoading(false))
  }, [])

  if (loading) return <Loading />

  const count = (c, key) => rows.filter((r) => r[key] === c).length

  return (
    <div className="page-fill">
      {!embedded && <PageHeader title="Phân loại hàng bán chạy" subtitle="Xếp hàng theo mức doanh thu và theo bán đều hay thất thường (90 ngày gần nhất)" />}

      <InfoBanner id="abcxyz" title="Bảng này nói gì?">
        <div className="mb-2">
          <b>Cột ABC xếp theo tiền bán được.</b> <b>A</b> là nhóm bán chạy, mang về phần lớn doanh thu.
          <b> B</b> là nhóm trung bình. <b>C</b> là nhóm bán ít.
        </div>
        <div className="mb-2">
          <b>Cột XYZ cho biết hàng bán đều hay thất thường.</b>
          <span className="badge bg-primary mx-1">X</span> bán đều đặn,
          <span className="badge bg-info mx-1">Y</span> bán lúc nhiều lúc ít,
          <span className="badge bg-danger mx-1">Z</span> bán thất thường, khó đoán.
        </div>
        <div>
          <b>Cách dùng đơn giản:</b> hàng vừa bán chạy vừa bán đều thì luôn giữ đủ hàng.
          Hàng bán ít và thất thường thì nhập ít thôi cho đỡ tồn đọng.
        </div>
      </InfoBanner>

      <div className="d-flex justify-content-end mb-3">
        <Button size="sm" onClick={() => navigate('/inventory', { state: { tab: 'suggest' } })}>
          <i className="bi bi-cart-plus me-1"></i>Gợi ý nhập hàng theo bảng này
        </Button>
      </div>

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

      <Card className="border-0 fill-card">
        <div className="table-responsive fill-scroll">
          <Table hover className="mb-0 align-middle">
            <thead><tr>
              <th>Sản phẩm</th><th className="text-end">Doanh thu (90 ngày)</th>
              <th className="text-end" title="Mặt hàng này chiếm bao nhiêu phần trăm trong tổng doanh thu">% doanh thu</th>
              <th className="text-end" title="Cộng dồn phần trăm doanh thu từ trên xuống, dùng để chia nhóm A/B/C">Cộng dồn</th>
              <th className="text-center">ABC</th>
              <th className="text-end">Đã bán</th>
              <th className="text-end" title="Số này càng nhỏ thì hàng bán càng đều, càng lớn thì càng thất thường">Mức đều</th>
              <th className="text-center">XYZ</th>
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
                  <td className="text-center">
                    <span className={`badge bg-${XYZ_COLOR[r.xyzClass]}`} title={XYZ_MEAN[r.xyzClass]} style={{ cursor: 'help' }}>{r.xyzClass}</span>
                  </td>
                </tr>
              ))}
              {rows.length === 0 && <tr><td colSpan={8}><EmptyState icon="bi-bar-chart" title="Chưa đủ dữ liệu bán hàng để xếp loại" /></td></tr>}
            </tbody>
          </Table>
        </div>
      </Card>
    </div>
  )
}
