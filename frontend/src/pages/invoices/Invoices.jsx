import { useEffect, useState } from 'react'
import { Button, Col, Form, Modal, Row, Table } from 'react-bootstrap'
import PageHeader from '../../components/ui/PageHeader'
import InfoBanner from '../../components/ui/InfoBanner'
import StatusPill from '../../components/ui/StatusPill'
import EmptyState from '../../components/ui/EmptyState'
import { SkeletonRows } from '../../components/ui/Loading'
import { invoiceApi, returnApi } from '../../api/sales'
import client from '../../api/client'
import { useToast } from '../../context/ToastContext'
import { useAuth } from '../../context/AuthContext'
import { errMsg } from '../../api/client'
import { formatMoney, formatDateTime } from '../../utils/format'

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
  const [returnTarget, setReturnTarget] = useState(null)

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
    if (cancelReason.trim().length < 3) { toast.warning('Nhập lý do hủy (tối thiểu 3 ký tự)'); return }
    setCancelling(true)
    try {
      await invoiceApi.cancel(cancelTarget.id, cancelReason.trim())
      toast.success('Đã hủy hóa đơn — tồn kho hoàn tự động')
      setCancelTarget(null); setCancelReason(''); setDetail(null); load()
    } catch (e) { toast.error(errMsg(e)) } finally { setCancelling(false) }
  }
  async function openPdf(id) {
    try {
      const res = await client.get(`/invoices/${id}/pdf`, { responseType: 'blob' })
      window.open(URL.createObjectURL(res.data), '_blank')
    } catch (e) { toast.error(errMsg(e)) }
  }

  return (
    <div>
      <PageHeader title="Hóa đơn" subtitle="Tra cứu, in lại & hủy hóa đơn" />

      <InfoBanner id="invoices" title="Quản lý hóa đơn">
        Lọc theo <b>ngày</b> và <b>trạng thái</b>. Bấm mã HĐ hoặc <i className="bi bi-eye"></i> để xem chi tiết,
        <i className="bi bi-printer"></i> để in PDF. <b>Hủy hóa đơn</b> (Quản lý/Chủ cửa hàng) sẽ
        <b> tự động hoàn trả tồn kho</b> và trừ lại điểm đã tích — dùng khi khách trả hàng hoặc lập sai.
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
            <tr><th>Mã HĐ</th><th>Thời gian</th><th>Thu ngân</th><th>Khách</th><th>Thanh toán</th>
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
                    <Button size="sm" variant="light" className="me-1" onClick={() => openDetail(i.id)} title="Chi tiết"><i className="bi bi-eye"></i></Button>
                    <Button size="sm" variant="light" className="me-1" onClick={() => openPdf(i.id)} title="In PDF"><i className="bi bi-printer"></i></Button>
                    {canCancel && i.status === 'COMPLETED' && (
                      <Button size="sm" variant="light" className="text-danger" onClick={() => setCancelTarget(i)} title="Hủy"><i className="bi bi-x-circle"></i></Button>
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
              {detail?.paymentMethod === 'CASH' && (
                <div className="d-flex justify-content-between small mt-1"><span className="text-muted2">Tiền thừa</span><span className="num">{formatMoney(detail?.changeAmount)}</span></div>
              )}
            </div>
          </div>
          {detail?.payment && (
            <div className="soft-card p-3 mt-3 d-flex align-items-center gap-3">
              {detail.payment.qrUrl && <img src={detail.payment.qrUrl} alt="QR" style={{ width: 90 }} />}
              <div className="small">
                <div>Thanh toán QR · <StatusPill value={detail.payment.status} /></div>
                <div className="text-muted2">Nội dung CK: <b>{detail.payment.transferContent}</b></div>
              </div>
            </div>
          )}
        </Modal.Body>
        <Modal.Footer>
          <Button variant="light" onClick={() => openPdf(detail.id)}><i className="bi bi-printer me-1"></i>In PDF</Button>
          {canCancel && detail?.status === 'COMPLETED' && <>
            <Button variant="outline-primary" onClick={() => setReturnTarget(detail)}><i className="bi bi-arrow-return-left me-1"></i>Trả hàng</Button>
            <Button variant="danger" onClick={() => setCancelTarget(detail)}><i className="bi bi-x-circle me-1"></i>Hủy hóa đơn</Button>
          </>}
        </Modal.Footer>
      </Modal>

      <ReturnModal invoice={returnTarget} onHide={() => setReturnTarget(null)}
        onDone={() => { setReturnTarget(null); setDetail(null); load() }} />

      <Modal show={!!cancelTarget} onHide={() => { setCancelTarget(null); setCancelReason('') }} centered>
        <Modal.Header closeButton><Modal.Title>Hủy hóa đơn {cancelTarget?.code}</Modal.Title></Modal.Header>
        <Modal.Body>
          <p className="text-muted2 small mb-2">Tồn kho sẽ được hoàn lại tự động. Hành động này được ghi nhật ký (ai hủy, khi nào, lý do).</p>
          <Form.Label>Lý do hủy <span className="text-danger">*</span></Form.Label>
          <Form.Control as="textarea" rows={2} value={cancelReason} autoFocus
            onChange={(e) => setCancelReason(e.target.value)} placeholder="vd: khách đổi ý, bấm nhầm, sai số lượng…" />
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

/** Modal trả hàng: chọn dòng + số lượng (≤ còn được trả), xem trước tiền hoàn, gửi. */
function ReturnModal({ invoice, onHide, onDone }) {
  const toast = useToast()
  const [lines, setLines] = useState([])
  const [qty, setQty] = useState({}) // invoiceItemId -> số trả
  const [reason, setReason] = useState('')
  const [loading, setLoading] = useState(false)
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    if (!invoice) return
    setLoading(true); setQty({}); setReason('')
    returnApi.returnable(invoice.id)
      .then((rs) => setLines(rs.filter((l) => l.returnableQty > 0)))
      .catch((e) => toast.error(errMsg(e))).finally(() => setLoading(false))
  }, [invoice])

  if (!invoice) return null
  const refund = lines.reduce((s, l) => s + (Number(qty[l.invoiceItemId]) || 0) * l.unitPrice, 0)
  const picked = lines.filter((l) => Number(qty[l.invoiceItemId]) > 0)

  async function submit() {
    if (picked.length === 0) { toast.warning('Chọn ít nhất 1 sản phẩm để trả'); return }
    if (reason.trim().length < 3) { toast.warning('Nhập lý do trả (≥ 3 ký tự)'); return }
    setSaving(true)
    try {
      const body = { invoiceId: invoice.id, reason: reason.trim(),
        items: picked.map((l) => ({ invoiceItemId: l.invoiceItemId, quantity: Number(qty[l.invoiceItemId]) })) }
      const r = await returnApi.create(body)
      toast.success(`Đã trả hàng — hoàn ${formatMoney(r.refundAmount)}`)
      onDone()
    } catch (e) { toast.error(errMsg(e)) } finally { setSaving(false) }
  }

  return (
    <Modal show={!!invoice} onHide={onHide} centered size="lg">
      <Modal.Header closeButton><Modal.Title>Trả hàng · HĐ {invoice.code}</Modal.Title></Modal.Header>
      <Modal.Body>
        <p className="text-muted2 small">Chọn số lượng trả cho từng sản phẩm (tối đa phần chưa trả). Hàng trả về <b>kho</b>, hoàn tiền theo đơn giá bán. Ghi nhật ký.</p>
        {loading ? <div className="text-center py-3">Đang tải…</div> : lines.length === 0 ? (
          <div className="text-muted2 text-center py-3">Hóa đơn này không còn gì để trả.</div>
        ) : (
          <Table size="sm" className="align-middle">
            <thead><tr><th>Sản phẩm</th><th className="text-center">Đã bán</th><th className="text-center">Đã trả</th><th className="text-center">Trả lần này</th><th className="text-end">Hoàn</th></tr></thead>
            <tbody>
              {lines.map((l) => (
                <tr key={l.invoiceItemId}>
                  <td className="fw-semibold">{l.productName}</td>
                  <td className="text-center num">{l.soldQty}</td>
                  <td className="text-center num text-muted2">{l.returnedQty}</td>
                  <td className="text-center" style={{ width: 110 }}>
                    <Form.Control type="number" size="sm" min={0} max={l.returnableQty} value={qty[l.invoiceItemId] ?? ''}
                      onChange={(e) => { const v = Math.max(0, Math.min(l.returnableQty, parseInt(e.target.value, 10) || 0)); setQty((q) => ({ ...q, [l.invoiceItemId]: v })) }}
                      placeholder={`≤ ${l.returnableQty}`} />
                  </td>
                  <td className="text-end num">{formatMoney((Number(qty[l.invoiceItemId]) || 0) * l.unitPrice)}</td>
                </tr>
              ))}
            </tbody>
          </Table>
        )}
        <Form.Label className="mt-2">Lý do trả <span className="text-danger">*</span></Form.Label>
        <Form.Control as="textarea" rows={2} value={reason} onChange={(e) => setReason(e.target.value)}
          placeholder="vd: hàng lỗi, khách đổi ý, mua nhầm…" />
        <div className="d-flex justify-content-end mt-3">
          <div className="fw-bold fs-6">Tổng hoàn: <span className="text-primary num">{formatMoney(refund)}</span></div>
        </div>
      </Modal.Body>
      <Modal.Footer>
        <Button variant="light" onClick={onHide}>Đóng</Button>
        <Button onClick={submit} disabled={saving || picked.length === 0 || reason.trim().length < 3}>
          {saving ? 'Đang xử lý…' : 'Xác nhận trả hàng'}
        </Button>
      </Modal.Footer>
    </Modal>
  )
}
