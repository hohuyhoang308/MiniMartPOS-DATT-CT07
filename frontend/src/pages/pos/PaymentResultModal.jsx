import { useEffect, useState } from 'react'
import { Badge, Button, Modal, Spinner } from 'react-bootstrap'
import client from '../../api/client'
import { paymentApi, invoiceApi } from '../../api/sales'
import { formatMoney } from '../../utils/format'

export default function PaymentResultModal({ invoice, onClose }) {
  const [payStatus, setPayStatus] = useState(null)
  const [printing, setPrinting] = useState(false)

  const isQr = invoice?.paymentMethod === 'QR'

  // Poll trạng thái thanh toán QR (FE poll như thiết kế UC22)
  useEffect(() => {
    if (!invoice || !isQr) return
    setPayStatus(invoice.payment?.status || 'PENDING')
    const timer = setInterval(async () => {
      try {
        const s = await paymentApi.status(invoice.id)
        setPayStatus(s.status)
        if (s.status === 'PAID') clearInterval(timer)
      } catch { /* bỏ qua */ }
    }, 3000)
    return () => clearInterval(timer)
  }, [invoice, isQr])

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

        {invoice.pointsEarned > 0 && (
          <div className="d-flex justify-content-between mb-1">
            <span>Điểm tích lũy</span><span>+{invoice.pointsEarned}</span>
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
            {payStatus !== 'PAID' && <Spinner size="sm" className="mt-2" />}
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
