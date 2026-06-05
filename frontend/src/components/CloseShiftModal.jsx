import { useEffect, useState } from 'react'
import { Button, Form, InputGroup, Modal, Spinner } from 'react-bootstrap'
import Recon from './ui/Recon'
import MoneyInput from './ui/MoneyInput'
import { shiftApi } from '../api/sales'
import { useToast } from '../context/ToastContext'
import { errMsg } from '../api/client'
import { formatMoney } from '../utils/format'

/**
 * Modal đóng ca + đối soát quỹ (DÙNG CHUNG cho trang POS và trang Quản lý ca).
 * Chỉ đếm TIỀN MẶT trong két; QR/CK chỉ hiển thị tham khảo (đã vào ngân hàng).
 *
 * Props:
 *  - shift: ca cần đóng (null/undefined ⇒ ẩn modal).
 *  - onHide(): đóng modal không lưu.
 *  - onClosed(): đã đóng ca thành công.
 *  - refetchCurrent: true ⇒ tải lại số liệu ca hiện tại qua /shifts/current khi mở
 *    (dùng ở POS — thu ngân đóng ca CỦA MÌNH; trang Quản lý ca đóng HỘ nên để false).
 */
export default function CloseShiftModal({ shift, onHide, onClosed, refetchCurrent = false }) {
  const toast = useToast()
  const [info, setInfo] = useState(shift)
  const [cash, setCash] = useState('')
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    if (!shift) return
    setInfo(shift)
    setCash(String(shift.expectedCash ?? shift.openingCash ?? 0))
    if (refetchCurrent) {
      // Lấy số liệu mới nhất (tiền mặt bán, dự kiến két) để đối soát chính xác sau các giao dịch.
      shiftApi.current().then((s) => {
        if (s) { setInfo(s); setCash(String(s.expectedCash ?? s.openingCash ?? 0)) }
      }).catch(() => {})
    }
  }, [shift, refetchCurrent])

  if (!shift) return null
  const expected = Number(info?.expectedCash ?? 0)
  const diff = Number(cash || 0) - expected

  async function submit(e) {
    e.preventDefault(); setLoading(true)
    try {
      await shiftApi.close(info.id, Number(cash))
      toast.success(`Đã đóng ca #${info.id}`)
      onClosed()
    } catch (e) { toast.error(errMsg(e)) } finally { setLoading(false) }
  }

  return (
    <Modal show={!!shift} onHide={onHide} centered>
      <Form onSubmit={submit}>
        <Modal.Header closeButton>
          <Modal.Title>Đóng ca #{info?.id}{info?.cashierName ? ` · ${info.cashierName}` : ''} — đối soát quỹ</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <div className="soft-card p-3 mb-3">
            <Recon label="Tiền đầu ca" value={info?.openingCash} />
            <Recon label="Tiền mặt bán trong ca" value={info?.cashSales} icon="bi-plus-lg" />
            {Number(info?.cashRefunds || 0) > 0 && (
              <Recon label="Tiền hoàn trả hàng" value={info.cashRefunds} icon="bi-dash-lg" />
            )}
            <hr className="my-2" />
            <Recon label="Tiền mặt dự kiến trong két" value={expected} strong />
            <div className="d-flex justify-content-between text-muted2 small mt-2">
              <span><i className="bi bi-qr-code me-1"></i>Tiền QR/CK (vào ngân hàng, không tính két)</span>
              <span className="num">{formatMoney(info?.qrSales)}</span>
            </div>
            {info?.totalSales != null && (
              <div className="text-muted2 small">Doanh thu ca: <b>{formatMoney(info.totalSales)}</b> · {info.invoiceCount} HĐ</div>
            )}
          </div>

          <div className="d-flex justify-content-between align-items-end mb-1">
            <Form.Label className="mb-0">Tiền mặt đếm thực tế trong két</Form.Label>
            <Button size="sm" variant="outline-primary" onClick={() => setCash(String(expected))}>
              <i className="bi bi-magic me-1"></i>Khớp quỹ
            </Button>
          </div>
          <InputGroup>
            <MoneyInput autoFocus value={cash} onChange={setCash} />
            <InputGroup.Text>đ</InputGroup.Text>
          </InputGroup>
          <div className="small text-muted2 mt-1">Không cần đếm lại nếu khớp — bấm <b>Khớp quỹ</b> để dùng số dự kiến.</div>

          <div className="d-flex justify-content-between align-items-center mt-3">
            <span className="fw-semibold">Chênh lệch</span>
            {diff === 0
              ? <span className="pill pill-success"><i className="bi bi-check-circle-fill"></i>Khớp quỹ</span>
              : <span className={`fw-bold ${diff > 0 ? 'text-success' : 'text-danger'}`}>
                  {diff > 0 ? `Thừa +${formatMoney(diff)}` : `Thiếu ${formatMoney(diff)}`}
                </span>}
          </div>
        </Modal.Body>
        <Modal.Footer>
          <Button variant="light" onClick={onHide}>Hủy</Button>
          <Button type="submit" variant="danger" disabled={loading}>{loading ? <Spinner size="sm" /> : 'Đóng ca'}</Button>
        </Modal.Footer>
      </Form>
    </Modal>
  )
}
