import { useEffect, useState } from 'react'
import { Button, Card, Col, InputGroup, Row, Spinner } from 'react-bootstrap'
import { shiftApi } from '../../api/sales'
import { useToast } from '../../context/ToastContext'
import { errMsg } from '../../api/client'
import { formatMoney } from '../../utils/format'
import MoneyInput from '../../components/ui/MoneyInput'

/* ---------------- Mở ca ---------------- */
export default function OpenShiftPanel({ onOpened }) {
  const toast = useToast()
  const [openingCash, setOpeningCash] = useState('')
  const [suggested, setSuggested] = useState(null)
  const [loading, setLoading] = useState(false)

  // Tự điền tiền đầu ca = tiền cuối ca trước (két chuyển tiếp) → khỏi đếm lại.
  useEffect(() => {
    shiftApi.suggestedOpening()
      .then((v) => { const n = Number(v || 0); setSuggested(n); setOpeningCash(String(n)) })
      .catch(() => setOpeningCash('0'))
  }, [])

  async function open() {
    setLoading(true)
    try { onOpened(await shiftApi.open(Number(openingCash))) }
    catch (e) { toast.error(errMsg(e)) } finally { setLoading(false) }
  }

  return (
    <Row className="justify-content-center">
      <Col md={5}>
        <Card className="border-0 mt-4 fade-up">
          <Card.Body className="p-4 text-center">
            <div className="login-logo mb-3" style={{ margin: '0 auto' }}><i className="bi bi-clock-history"></i></div>
            <h5 className="fw-bold">Mở ca làm việc</h5>
            <p className="text-muted2">Tiền đầu ca đã được điền sẵn theo <b>tiền còn lại của ca trước</b>. Bạn chỉ cần xác nhận, không phải đếm lại tiền trong két.</p>
            <InputGroup className="mb-2">
              <InputGroup.Text>Tiền đầu ca</InputGroup.Text>
              <MoneyInput value={openingCash} onChange={setOpeningCash} />
              <InputGroup.Text>đ</InputGroup.Text>
            </InputGroup>
            {suggested != null && (
              <div className="small text-muted2 mb-3">
                {suggested > 0
                  ? <>Gợi ý từ ca trước: <b>{formatMoney(suggested)}</b>{Number(openingCash) !== suggested &&
                      <Button variant="link" size="sm" className="p-0 ms-1 align-baseline" onClick={() => setOpeningCash(String(suggested))}>dùng số này</Button>}</>
                  : <>Chưa có ca trước. Bạn hãy nhập số tiền lẻ đang có sẵn trong két (ví dụ 500.000đ).</>}
              </div>
            )}
            <Button className="w-100" onClick={open} disabled={loading}>
              {loading ? <Spinner size="sm" /> : <><i className="bi bi-unlock me-1"></i>Mở ca</>}
            </Button>
          </Card.Body>
        </Card>
      </Col>
    </Row>
  )
}
