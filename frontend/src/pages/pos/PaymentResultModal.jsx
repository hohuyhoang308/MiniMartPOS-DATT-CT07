import { useEffect, useState } from 'react'
import { Badge, Button, Modal, Spinner } from 'react-bootstrap'
import client, { errMsg } from '../../api/client'
import { paymentApi, invoiceApi } from '../../api/sales'
import { useToast } from '../../context/ToastContext'
import { formatMoney } from '../../utils/format'

export default function PaymentResultModal({ invoice, onClose, onPaid }) {
  const toast = useToast()
  const [payStatus, setPayStatus] = useState(null)
  const [printing, setPrinting] = useState(false)
  const [confirming, setConfirming] = useState(false)

  const isQr = invoice?.paymentMethod === 'QR'

  // Poll trạng thái thanh toán QR (FE poll như thiết kế UC22).
  // Khi nhận PAID (tự động qua WEB2M hoặc xác nhận tay) → báo cha refresh số liệu ca.
  useEffect(() => {
    if (!invoice || !isQr) return
    setPayStatus(invoice.payment?.status || 'PENDING')
    const timer = setInterval(async () => {
      try {
        const s = await paymentApi.status(invoice.id)
        setPayStatus(s.status)
        if (s.status === 'PAID') { clearInterval(timer); onPaid?.() }
      } catch { /* bỏ qua */ }
    }, 3000)
    return () => clearInterval(timer)
  }, [invoice, isQr])

  // Thu ngân xác nhận đã nhận tiền QR (khi chưa bật/khớp WEB2M tự động).
  async function confirmPaid() {
    setConfirming(true)
    try {
      await paymentApi.confirm(invoice.id)
      setPayStatus('PAID')
      onPaid?.()
      toast.success('Đã xác nhận nhận tiền — hóa đơn hoàn tất')
    } catch (e) {
      toast.error(errMsg(e, 'Không xác nhận được thanh toán'))
    } finally {
      setConfirming(false)
    }
  }

  async function printPdf() {
    setPrinting(true)
    try {
      const res = await client.get(invoiceApi.pdfUrl(invoice.id).replace('/api', ''), {
        responseType: 'blob',
      })
      const url = URL.createObjectURL(res.data)
      window.open(url, '_blank')
    } finally {
      setPrinting(false)
    }
  }

  if (!invoice) return null

  return (
    <Modal show={!!invoice} onHide={onClose} centered>
      <Modal.Header closeButton>
        <Modal.Title className="fs-6">
          <i className="bi bi-check-circle-fill text-success me-2"></i>
          Hóa đơn {invoice.code}
        </Modal.Title>
      </Modal.Header>
      <Modal.Body>
        <div className="d-flex justify-content-between mb-1">
          <span>Tổng tiền</span><b>{formatMoney(invoice.totalAmount)}</b>
        </div>

        {invoice.paymentMethod === 'CASH' && (
          <>
            <div className="d-flex justify-content-between mb-1">
              <span>Khách đưa</span><span>{formatMoney(invoice.customerPaid)}</span>
            </div>
            <div className="d-flex justify-content-between mb-1">
              <span>Tiền thừa</span>
              <b className="text-success">{formatMoney(invoice.changeAmount)}</b>
            </div>
          </>
        )}

        {invoice.pointsUsed > 0 && (
          <div className="d-flex justify-content-between mb-1">
            <span>Điểm đã dùng</span><span className="text-danger">−{invoice.pointsUsed}</span>
          </div>
        )}
        {invoice.pointsEarned > 0 && (
          <div className="d-flex justify-content-between mb-1">
            <span>Điểm tích lũy</span><span className="text-success">+{invoice.pointsEarned}</span>
          </div>
        )}

        {isQr && (
          <div className="text-center mt-3">
            <div className="mb-2">
              Trạng thái:{' '}
              <Badge bg={payStatus === 'PAID' ? 'success' : 'warning'}>
                {payStatus === 'PAID' ? 'Đã nhận tiền' : 'Chờ thanh toán'}
              </Badge>
            </div>
            {invoice.payment?.qrUrl ? (
              <img src={invoice.payment.qrUrl} alt="VietQR" style={{ maxWidth: 240 }} className="img-fluid" />
            ) : (
              <p className="text-muted small">Chưa cấu hình ngân hàng để tạo QR (vào Cấu hình).</p>
            )}
            <div className="small text-muted mt-1">
              Nội dung CK: <b>{invoice.payment?.transferContent}</b>
            </div>
            {payStatus !== 'PAID' && (
              <div className="mt-3">
                <div className="small text-muted2 mb-2">
                  <Spinner size="sm" className="me-1" />Đang chờ tiền về (tự đối soát WEB2M)…
                </div>
                <Button variant="success" size="sm" onClick={confirmPaid} disabled={confirming}>
                  {confirming ? <Spinner size="sm" /> : <><i className="bi bi-check2-circle me-1"></i>Xác nhận đã nhận tiền</>}
                </Button>
                <div className="small text-muted2 mt-1">Bấm khi đã thấy tiền vào tài khoản (hoặc đợi hệ thống tự xác nhận).</div>
              </div>
            )}
          </div>
        )}
      </Modal.Body>
      <Modal.Footer>
        <Button variant="outline-secondary" onClick={printPdf} disabled={printing}>
          {printing ? <Spinner size="sm" /> : <><i className="bi bi-printer me-1"></i>In hóa đơn (PDF)</>}
        </Button>
        <Button onClick={onClose}>
          <i className="bi bi-plus-circle me-1"></i>Đơn mới
        </Button>
      </Modal.Footer>
    </Modal>
  )
}
