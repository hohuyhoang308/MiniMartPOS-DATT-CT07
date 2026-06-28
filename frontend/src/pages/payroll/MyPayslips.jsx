import { useEffect, useState } from 'react'
import { Badge, Button, Card, Col, Row, Table } from 'react-bootstrap'
import PageHeader from '../../components/ui/PageHeader'
import InfoBanner from '../../components/ui/InfoBanner'
import EmptyState from '../../components/ui/EmptyState'
import Loading from '../../components/ui/Loading'
import { payrollApi } from '../../api/misc'
import { useToast } from '../../context/ToastContext'
import { errMsg } from '../../api/client'
import { formatMoney } from '../../utils/format'

const fmtH = (h) => `${Number(h || 0).toLocaleString('vi-VN', { maximumFractionDigits: 2 })}h`

/** Phiếu lương của chính nhân viên đăng nhập (chỉ các kỳ đã khóa/đã chi). */
export default function MyPayslips() {
  const toast = useToast()
  const [slips, setSlips] = useState([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    payrollApi.myPayslips().then(setSlips).catch((e) => toast.error(errMsg(e))).finally(() => setLoading(false))
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  async function printPdf(id) {
    try {
      const blob = await payrollApi.payslipPdf(id)
      window.open(URL.createObjectURL(blob), '_blank')
    } catch (e) { toast.error(errMsg(e)) }
  }

  if (loading) return <Loading />

  return (
    <div>
      <PageHeader title="Phiếu lương của tôi" subtitle="Lương theo công làm việc của bạn ở các kỳ đã chốt." />
      <InfoBanner id="my-payslips" title="Cách đọc phiếu lương">
        <b>Giờ công</b> được tính tự động từ các <b>ca bạn đã đóng</b> trong tháng. <b>Lương gốc</b> = công ×
        đơn giá; làm vượt <b>công chuẩn</b> thì phần dư tính <b>tăng ca</b>. <b>Thực lĩnh</b> = lương gộp +
        thưởng − phạt/tạm ứng. Chỉ những kỳ quản lý đã <b>chốt</b> mới hiển thị ở đây.
      </InfoBanner>

      {slips.length === 0 ? (
        <Card className="border-0"><Card.Body><EmptyState icon="bi-receipt" title="Chưa có phiếu lương nào" hint="Phiếu sẽ xuất hiện khi quản lý chốt kỳ lương." /></Card.Body></Card>
      ) : (
        <Row className="g-3">
          {slips.map((p) => (
            <Col md={6} key={p.id}>
              <Card className="border-0 h-100 fade-up">
                <Card.Body>
                  <div className="d-flex justify-content-between align-items-start mb-3">
                    <div>
                      <div className="text-muted2 small">Kỳ lương</div>
                      <div className="fs-5 fw-bold">{p.fullName}</div>
                    </div>
                    <div className="text-end">
                      <div className="text-muted2 small">Thực lĩnh</div>
                      <div className="fs-5 fw-bold text-success">{formatMoney(p.netPay)}</div>
                      <Button size="sm" variant="soft" className="mt-1" onClick={() => printPdf(p.id)}>
                        <i className="bi bi-printer me-1"></i>In PDF
                      </Button>
                    </div>
                  </div>
                  <Table size="sm" className="mb-0">
                    <tbody>
                      <tr><td className="text-muted2">Số ca làm</td><td className="text-end">{p.shiftCount} ca</td></tr>
                      <tr><td className="text-muted2">Giờ công</td><td className="text-end num">{fmtH(p.workedHours)}</td></tr>
                      <tr><td className="text-muted2">Trong đó tăng ca</td><td className="text-end num">{Number(p.otHours) > 0 ? <Badge bg="warning" text="dark">{fmtH(p.otHours)}</Badge> : '—'}</td></tr>
                      <tr><td className="text-muted2">Đơn giá</td><td className="text-end num">{formatMoney(p.baseRate)}{p.payType === 'HOURLY' ? '/giờ' : '/tháng'}</td></tr>
                      <tr><td className="text-muted2">Lương gốc</td><td className="text-end num">{formatMoney(p.regularPay)}</td></tr>
                      <tr><td className="text-muted2">Tiền tăng ca</td><td className="text-end num">{formatMoney(p.otPay)}</td></tr>
                      <tr><td className="text-muted2">Phụ cấp</td><td className="text-end num">{formatMoney(p.allowance)}</td></tr>
                      <tr><td className="text-muted2">Thưởng</td><td className="text-end num text-success">{Number(p.totalBonus) > 0 ? `+${formatMoney(p.totalBonus)}` : '—'}</td></tr>
                      <tr><td className="text-muted2">Phạt / tạm ứng</td><td className="text-end num text-danger">{Number(p.totalDeduction) > 0 ? `−${formatMoney(p.totalDeduction)}` : '—'}</td></tr>
                    </tbody>
                  </Table>
                  {p.adjustments?.length > 0 && (
                    <div className="mt-2 small text-muted2">
                      {p.adjustments.map((a) => (
                        <div key={a.id}>
                          <i className={`bi ${a.type === 'BONUS' ? 'bi-plus-circle text-success' : 'bi-dash-circle text-danger'} me-1`}></i>
                          {a.reason}: {formatMoney(a.amount)}
                        </div>
                      ))}
                    </div>
                  )}
                </Card.Body>
              </Card>
            </Col>
          ))}
        </Row>
      )}
    </div>
  )
}
