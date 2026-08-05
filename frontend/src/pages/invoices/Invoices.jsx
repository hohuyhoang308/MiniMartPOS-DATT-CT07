import { useEffect, useState } from 'react'
import { Button, Col, Form, Modal, Row, Table } from 'react-bootstrap'
import PageHeader from '../../components/ui/PageHeader'
import InfoBanner from '../../components/ui/InfoBanner'
import StatusPill from '../../components/ui/StatusPill'
import EmptyState from '../../components/ui/EmptyState'
import { SkeletonRows } from '../../components/ui/Loading'
import { invoiceApi } from '../../api/sales'
import { useToast } from '../../context/ToastContext'
import { useAuth } from '../../context/AuthContext'
import { errMsg } from '../../api/client'
import { formatMoney, formatDateTime } from '../../utils/format'
import { openBlob } from '../../utils/download'

export default function Invoices() {
  const toast = useToast()
  const { hasRole } = useAuth()
  const canCancel = hasRole('ADMIN', 'MANAGER')
  const [list, setList] = useState([])
  const [loading, setLoading] = useState(true)
  const [date, setDate] = useState('')
  const [status, setStatus] = useState('')
  const [detail, setDetail] = useState(null)
  const [cancelTarget, setCancelTarget] = useState(null)
  const [cancelReason, setCancelReason] = useState('')
  const [cancelling, setCancelling] = useState(false)

  async function load() {
    setLoading(true)
    try {
      const params = {}
      if (date) params.date = date
      if (status) params.status = status
      setList(await invoiceApi.list(params))
    } catch (e) { toast.error(errMsg(e)) } finally { setLoading(false) }
  }
  useEffect(() => { load() }, [date, status])

  async function openDetail(id) {
    try { setDetail(await invoiceApi.get(id)) } catch (e) { toast.error(errMsg(e)) }
  }
  async function doCancel() {
    if (cancelReason.trim().length < 3) { toast.warning('Vui lòng nhập lý do hủy (ít nhất 3 ký tự)'); return }
    setCancelling(true)
    try {
      await invoiceApi.cancel(cancelTarget.id, cancelReason.trim())
      toast.success('Đã hủy hóa đơn. Số hàng đã được trả lại vào kho.')
      setCancelTarget(null); setCancelReason(''); setDetail(null); load()
    } catch (e) { toast.error(errMsg(e)) } finally { setCancelling(false) }
  }
  async function openPdf(id) {
    try { openBlob(await invoiceApi.pdf(id)) } catch (e) { toast.error(errMsg(e)) }
  }

  return (
    <div>
      <PageHeader title="Hóa đơn" subtitle="Tra cứu, in lại và hủy hóa đơn" />

      <InfoBanner id="invoices" title="Quản lý hóa đơn">
        Lọc hóa đơn theo <b>ngày</b> và <b>trạng thái</b>. Bấm vào mã hóa đơn hoặc <i className="bi bi-eye"></i> để xem chi tiết,
        bấm <i className="bi bi-printer"></i> để in. Khi <b>hủy hóa đơn</b> (chỉ Quản lý và Chủ cửa hàng làm được),
        hệ thống sẽ <b>tự trả lại số hàng vào kho</b> và trừ lại số điểm đã tích cho khách. Dùng khi bạn lập nhầm hóa đơn.
      </InfoBanner>

      <Row className="g-2 mb-3">
        <Col md={3}>
          <Form.Control type="date" value={date} onChange={(e) => setDate(e.target.value)} />
        </Col>
        <Col md={3}>
          <Form.Select value={status} onChange={(e) => setStatus(e.target.value)}>
            <option value="">Tất cả trạng thái</option>
            <option value="COMPLETED">Hoàn tất</option>
            <option value="CANCELLED">Đã hủy</option>
          </Form.Select>
        </Col>
        {(date || status) && <Col md="auto"><Button variant="light" onClick={() => { setDate(''); setStatus('') }}>
          <i className="bi bi-x-lg me-1"></i>Xóa lọc</Button></Col>}
      </Row>

      <div className="table-wrap fade-up">
        <Table hover className="mb-0">
          <thead>
            <tr><th>Mã hóa đơn</th><th>Thời gian</th><th>Thu ngân</th><th>Khách</th><th>Thanh toán</th>
              <th className="text-end">Tổng tiền</th><th>Trạng thái</th><th className="text-end">Thao tác</th></tr>
          </thead>
          {loading ? <SkeletonRows cols={8} /> : (
            <tbody>
              {list.map((i) => (
                <tr key={i.id}>
                  <td className="fw-semibold cursor-pointer" onClick={() => openDetail(i.id)}>{i.code}</td>
                  <td className="text-muted2 small">{formatDateTime(i.createdAt)}</td>
                  <td>{i.cashierName}</td>
                  <td className="text-muted2">{i.customerName || '—'}</td>
                  <td><StatusPill value={i.paymentMethod} /></td>
                  <td className="text-end num fw-semibold">{formatMoney(i.totalAmount)}</td>
                  <td><StatusPill value={i.status} /></td>
                  <td className="text-end">
                    <Button size="sm" variant="light" className="me-1" onClick={() => openDetail(i.id)} title="Xem chi tiết"><i className="bi bi-eye"></i></Button>
                    <Button size="sm" variant="light" className="me-1" onClick={() => openPdf(i.id)} title="In hóa đơn"><i className="bi bi-printer"></i></Button>
                    {canCancel && i.status === 'COMPLETED' && (
                      <Button size="sm" variant="light" className="text-danger" onClick={() => setCancelTarget(i)} title="Hủy hóa đơn"><i className="bi bi-x-circle"></i></Button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          )}
        </Table>
        {!loading && list.length === 0 && <EmptyState icon="bi-receipt" title="Không có hóa đơn" />}
      </div>

      {/* Chi tiết HĐ */}
      <Modal show={!!detail} onHide={() => setDetail(null)} centered size="lg">
        <Modal.Header closeButton>
          <Modal.Title>Hóa đơn {detail?.code} <StatusPill value={detail?.status} /></Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <Row className="small text-muted2 mb-3">
            <Col>Thu ngân: <b className="text-dark">{detail?.cashierName}</b></Col>
            <Col>Khách: <b className="text-dark">{detail?.customerName || 'Khách lẻ'}</b></Col>
            <Col>Thời gian: <b className="text-dark">{formatDateTime(detail?.createdAt)}</b></Col>
          </Row>
          <Table size="sm" hover>
            <thead><tr><th>Sản phẩm</th><th className="text-center">SL</th><th className="text-end">Đơn giá</th><th className="text-end">Thành tiền</th></tr></thead>
            <tbody>
              {detail?.items?.map((it, idx) => (
                <tr key={idx}><td>{it.productName}</td><td className="text-center num">{it.quantity}</td>
                  <td className="text-end num">{formatMoney(it.unitPrice)}</td><td className="text-end num">{formatMoney(it.subtotal)}</td></tr>
              ))}
            </tbody>
          </Table>
          <div className="d-flex justify-content-end">
            <div style={{ minWidth: 240 }}>
              <div className="d-flex justify-content-between small"><span className="text-muted2">Tạm tính</span><span className="num">{formatMoney(detail?.subtotal)}</span></div>
              <div className="d-flex justify-content-between small text-success"><span>Giảm giá</span><span className="num">-{formatMoney(detail?.discountAmount)}</span></div>
              <div className="d-flex justify-content-between fw-bold fs-6 mt-1"><span>Tổng cộng</span><span className="num text-primary">{formatMoney(detail?.totalAmount)}</span></div>
              {detail?.taxAmount > 0 && (
                <div className="d-flex justify-content-between small text-muted2"><span>Trong đó tiền thuế VAT</span><span className="num">{formatMoney(detail?.taxAmount)}</span></div>
              )}
              {detail?.paymentMethod === 'CASH' && (
                <div className="d-flex justify-content-between small mt-1"><span className="text-muted2">Tiền thừa</span><span className="num">{formatMoney(detail?.changeAmount)}</span></div>
              )}
            </div>
          </div>
          {detail?.payment && (
            <div className="soft-card p-3 mt-3 d-flex align-items-center gap-3">
              {detail.payment.qrUrl && <img src={detail.payment.qrUrl} alt="QR" style={{ width: 90 }} />}
              <div className="small">
                <div>Thanh toán chuyển khoản (QR) · <StatusPill value={detail.payment.status} /></div>
                <div className="text-muted2">Nội dung chuyển khoản: <b>{detail.payment.transferContent}</b></div>
              </div>
            </div>
          )}
        </Modal.Body>
        <Modal.Footer>
          <Button variant="light" onClick={() => openPdf(detail.id)}><i className="bi bi-printer me-1"></i>In hóa đơn</Button>
          {canCancel && detail?.status === 'COMPLETED' && (
            <Button variant="danger" onClick={() => setCancelTarget(detail)}><i className="bi bi-x-circle me-1"></i>Hủy hóa đơn</Button>
          )}
        </Modal.Footer>
      </Modal>

      <Modal show={!!cancelTarget} onHide={() => { setCancelTarget(null); setCancelReason('') }} centered>
        <Modal.Header closeButton><Modal.Title>Hủy hóa đơn {cancelTarget?.code}</Modal.Title></Modal.Header>
        <Modal.Body>
          <p className="text-muted2 small mb-2">Số hàng trong hóa đơn sẽ được trả lại vào kho. Việc hủy này được lưu lại đầy đủ: ai hủy, vào lúc nào và vì lý do gì.</p>
          <Form.Label>Lý do hủy <span className="text-danger">*</span></Form.Label>
          <Form.Control as="textarea" rows={2} value={cancelReason} autoFocus
            onChange={(e) => setCancelReason(e.target.value)} placeholder="Ví dụ: khách đổi ý, bấm nhầm, sai số lượng…" />
        </Modal.Body>
        <Modal.Footer>
          <Button variant="light" onClick={() => { setCancelTarget(null); setCancelReason('') }}>Đóng</Button>
          <Button variant="danger" onClick={doCancel} disabled={cancelling || cancelReason.trim().length < 3}>
            {cancelling ? 'Đang hủy…' : 'Hủy hóa đơn'}
          </Button>
        </Modal.Footer>
      </Modal>
    </div>
  )
}
