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
  A: 'Doanh thu chủ lực — kiểm soát chặt, không để hết hàng.',
  B: 'Quan trọng vừa — theo dõi định kỳ.',
  C: 'Đuôi dài — nới lỏng, gom đơn để tiết kiệm chi phí đặt.',
}
/** Ý nghĩa dễ hiểu của nhóm XYZ (hiện khi rê chuột vào nhãn). */
const XYZ_MEAN = {
  X: 'Bán ĐỀU đặn mỗi tuần — dễ dự báo, ít cần tồn dự phòng.',
  Y: 'Bán DAO ĐỘNG vừa phải theo tuần — cần theo dõi.',
  Z: 'Bán THẤT THƯỜNG (lúc nhiều lúc ít) — khó dự báo, cần dự phòng nhiều hơn.',
}

export default function AbcXyz() {
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
      <PageHeader title="Phân tích ABC / XYZ" subtitle="Phân loại sản phẩm theo doanh thu (Pareto) × độ biến động nhu cầu (90 ngày)" />

      <InfoBanner id="abcxyz" title="ABC/XYZ là gì? Đọc bảng thế nào?">
        <div className="mb-2">
          <b>ABC — xếp theo DOANH THU</b> (quy tắc Pareto 80/20): <b>A</b> = nhóm tạo ~80% doanh thu
          (ít mặt hàng nhưng quan trọng nhất), <b>B</b> = phần kế tiếp (80–95%), <b>C</b> = phần đuôi còn lại.
        </div>
        <div className="mb-2">
          <b>XYZ — xếp theo ĐỘ ỔN ĐỊNH nhu cầu.</b> Đo bằng <b>CV = độ lệch chuẩn ÷ trung bình</b> lượng bán
          <b> mỗi TUẦN</b> (gộp theo tuần để bỏ nhiễu những ngày không phát sinh bán). <b>CV càng nhỏ → bán càng đều</b>:
          <span className="badge bg-primary mx-1">X</span> đều đặn (CV &lt; 0,5),
          <span className="badge bg-info mx-1">Y</span> dao động vừa (0,5–1,0),
          <span className="badge bg-danger mx-1">Z</span> thất thường (≥ 1,0).
        </div>
        <div>
          <b>Phối hợp để quyết định:</b> <b>A·X</b> (bán chạy &amp; đều) → luôn giữ đủ hàng, kiểm soát chặt;
          <b> C·Z</b> (ít &amp; thất thường) → đặt thưa, gom đơn, giảm tồn để đỡ đọng vốn.
        </div>
      </InfoBanner>

      <div className="d-flex justify-content-end mb-3">
        <Button size="sm" onClick={() => navigate('/inventory', { state: { tab: 'suggest' } })}>
          <i className="bi bi-cart-plus me-1"></i>Đề xuất nhập theo phân loại này
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
              <th>Sản phẩm</th><th className="text-end">Doanh thu (90n)</th>
              <th className="text-end" title="Phần trăm doanh thu của mặt hàng trên tổng">% DT</th>
              <th className="text-end" title="Doanh thu luỹ kế cộng dồn — dùng để chia nhóm A/B/C">Luỹ kế</th>
              <th className="text-center">ABC</th>
              <th className="text-end">Đã bán</th>
              <th className="text-end" title="Hệ số biến động THEO TUẦN (σ/μ): càng nhỏ thì bán càng đều">CV/tuần</th>
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
              {rows.length === 0 && <tr><td colSpan={8}><EmptyState icon="bi-bar-chart" title="Chưa đủ dữ liệu bán hàng để phân tích" /></td></tr>}
            </tbody>
          </Table>
        </div>
      </Card>
    </div>
  )
}
