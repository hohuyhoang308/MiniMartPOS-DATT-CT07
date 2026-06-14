import { useEffect, useMemo, useState } from 'react'
import { Button, Card, Form, Modal, Spinner, Table } from 'react-bootstrap'
import PageHeader from '../../components/ui/PageHeader'
import InfoBanner from '../../components/ui/InfoBanner'
import EmptyState from '../../components/ui/EmptyState'
import Loading from '../../components/ui/Loading'
import ExpiryPill from '../../components/ui/ExpiryPill'
import { productApi } from '../../api/catalog'
import { inventoryApi, stockAdjustmentApi } from '../../api/misc'
import { useToast } from '../../context/ToastContext'
import { errMsg } from '../../api/client'
import { formatDateTime } from '../../utils/format'

/** Lý do xuất hủy → nhãn + màu pill (đồng bộ với enum AdjustmentReason ở backend). */
export const REASONS = {
  EXPIRED: { label: 'Hết hạn sử dụng', cls: 'pill-danger', icon: 'bi-clock-history' },
  DAMAGED: { label: 'Hư hỏng', cls: 'pill-warning', icon: 'bi-exclamation-triangle-fill' },
  LOST: { label: 'Thất thoát / hao hụt', cls: 'pill-violet', icon: 'bi-question-octagon-fill' },
  OTHER: { label: 'Khác', cls: 'pill-muted', icon: 'bi-three-dots' },
}

export default function StockAdjust({ embedded = false }) {
  const toast = useToast()
  const [products, setProducts] = useState([])
  const [history, setHistory] = useState([])
  const [loading, setLoading] = useState(true)
  const [q, setQ] = useState('')
  const [target, setTarget] = useState(null) // sản phẩm đang xuất hủy

  function load(first) {
    if (first) setLoading(true)
    Promise.all([productApi.list(), stockAdjustmentApi.list()])
      .then(([ps, hs]) => { setProducts(ps); setHistory(hs) })
      .catch((e) => toast.error(errMsg(e))).finally(() => setLoading(false))
  }
  useEffect(() => { load(true) }, [])

  const rows = useMemo(() => {
    const kw = q.trim().toLowerCase()
    // Chỉ hàng còn TRONG KHO mới xuất hủy được (hàng trên kệ phải lấy về kho trước).
    let list = products.filter((p) => (p.warehouseStock ?? 0) > 0)
    if (kw) list = list.filter((p) => p.name.toLowerCase().includes(kw) || p.barcode.includes(kw))
    return list.sort((a, b) => a.name.localeCompare(b.name))
  }, [products, q])

  if (loading) return <Loading />

  return (
    <div>
      {!embedded && <PageHeader title="Xuất hủy / Điều chỉnh kho" subtitle="Rút hàng hết hạn, hư hỏng hoặc thất thoát ra khỏi tồn kho" />}

      <InfoBanner id="stock-adjust" title="Xuất hủy / điều chỉnh kho">
        Khi hàng <b>hết hạn</b>, <b>hư hỏng</b> hoặc <b>thất thoát</b> (kiểm kê thiếu), bạn dùng trang này để
        <b> rút số lượng đó khỏi tồn kho</b> — nhờ vậy cảnh báo tồn kho, gợi ý nhập hàng và giá trị kho mới chính xác.
        Chỉ xuất hủy được hàng đang <b>trong kho</b>; nếu hàng còn trên kệ, hãy <b>"lấy về kho"</b> ở trang Kệ trước.
        Mỗi lần xuất hủy đều được <b>ghi vào nhật ký thao tác</b>.
      </InfoBanner>

      <Card className="border-0 mb-3">
        <Card.Body className="d-flex align-items-center gap-2 flex-wrap">
          <div className="input-group" style={{ maxWidth: 340 }}>
            <span className="input-group-text"><i className="bi bi-search"></i></span>
            <Form.Control placeholder="Tìm theo tên sản phẩm hoặc mã vạch…" value={q} onChange={(e) => setQ(e.target.value)} />
          </div>
          <span className="text-muted2 small">Chỉ hiện sản phẩm còn hàng trong kho.</span>
        </Card.Body>
        <div className="table-responsive" style={{ maxHeight: 420, overflowY: 'auto' }}>
          <Table hover className="mb-0 align-middle">
            <thead><tr>
              <th>Sản phẩm</th><th className="text-center">Trên kệ</th><th className="text-center">Trong kho</th><th className="text-end">Thao tác</th>
            </tr></thead>
            <tbody>
              {rows.map((p) => (
                <tr key={p.id}>
                  <td className="fw-semibold">{p.name}<div className="text-muted2 small">{p.barcode}</div></td>
                  <td className="text-center num text-muted2">{p.shelfStock ?? 0}</td>
                  <td className="text-center num text-primary fw-semibold">{p.warehouseStock ?? 0}</td>
                  <td className="text-end">
                    <Button size="sm" variant="outline-danger" onClick={() => setTarget(p)}>
                      <i className="bi bi-trash3 me-1"></i>Xuất hủy
                    </Button>
                  </td>
                </tr>
              ))}
              {rows.length === 0 && <tr><td colSpan={4}><EmptyState icon="bi-box-seam" title="Không có hàng trong kho để xuất hủy" /></td></tr>}
            </tbody>
          </Table>
        </div>
      </Card>

      <h6 className="fw-bold mt-4 mb-2"><i className="bi bi-clock-history me-2"></i>Lịch sử xuất hủy gần đây</h6>
      <div className="table-wrap">
        <Table hover className="mb-0 align-middle">
          <thead><tr>
            <th>Thời điểm</th><th>Sản phẩm</th><th className="text-center">Số lượng</th><th>Lý do</th><th>Ghi chú</th><th>Người làm</th>
          </tr></thead>
          <tbody>
            {history.map((h) => {
              const r = REASONS[h.reason] || REASONS.OTHER
              return (
                <tr key={h.id}>
                  <td className="text-muted2 small" style={{ whiteSpace: 'nowrap' }}>{formatDateTime(h.createdAt)}</td>
                  <td className="fw-semibold">{h.productName}<div className="text-muted2 small">Lô #{h.batchId}</div></td>
                  <td className="text-center num fw-bold text-danger">−{h.quantity}</td>
                  <td><span className={`pill ${r.cls}`}><i className={`bi ${r.icon}`}></i>{r.label}</span></td>
                  <td className="small text-muted2">{h.note || '—'}</td>
                  <td className="small text-muted2">{h.createdByName || '—'}</td>
                </tr>
              )
            })}
            {history.length === 0 && <tr><td colSpan={6}><EmptyState icon="bi-clipboard-check" title="Chưa có lần xuất hủy nào" /></td></tr>}
          </tbody>
        </Table>
      </div>

      <WriteOffModal product={target} onHide={() => setTarget(null)} onDone={() => { setTarget(null); load(false) }} />
    </div>
  )
}

/** Modal: chọn LÔ trong kho + số lượng + lý do để xuất hủy. */
function WriteOffModal({ product, onHide, onDone }) {
  const toast = useToast()
  const [batches, setBatches] = useState([])
  const [batchId, setBatchId] = useState('')
  const [qty, setQty] = useState(1)
  const [reason, setReason] = useState('EXPIRED')
  const [note, setNote] = useState('')
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    if (!product) return
    setBatchId(''); setQty(1); setReason('EXPIRED'); setNote('')
    inventoryApi.batches(product.id)
      .then((bs) => {
        const whb = bs.filter((b) => b.inWarehouse > 0)
        setBatches(whb)
        // Ưu tiên chọn lô hết hạn/gần hết hạn trước (daysLeft nhỏ nhất).
        const first = [...whb].sort((a, b) => (a.daysLeft ?? 99999) - (b.daysLeft ?? 99999))[0]
        if (first) setBatchId(String(first.batchId))
      })
      .catch(() => setBatches([]))
  }, [product])

  if (!product) return null
  const chosen = batches.find((b) => String(b.batchId) === String(batchId))
  const maxQty = chosen ? chosen.inWarehouse : 0
  const qtyNum = Number(qty) || 0
  const qtyInvalid = qtyNum < 1 || qtyNum > maxQty
  // Gợi ý: lô đã quá hạn nên chọn lý do "Hết hạn".
  const expired = chosen && chosen.daysLeft != null && chosen.daysLeft < 0

  async function submit(e) {
    e.preventDefault()
    if (!batchId) { toast.warning('Vui lòng chọn lô hàng'); return }
    if (qtyInvalid) { toast.warning(`Vui lòng nhập số lượng từ 1 đến ${maxQty}`); return }
    setLoading(true)
    try {
      await stockAdjustmentApi.create({ batchId: Number(batchId), quantity: qtyNum, reason, note: note.trim() || null })
      toast.success(`Đã xuất hủy ${qtyNum} "${product.name}" khỏi tồn kho`)
      onDone()
    } catch (e) { toast.error(errMsg(e)) } finally { setLoading(false) }
  }

  return (
    <Modal show={!!product} onHide={onHide} centered>
      <Form onSubmit={submit}>
        <Modal.Header closeButton><Modal.Title>Xuất hủy · {product.name}</Modal.Title></Modal.Header>
        <Modal.Body>
          {batches.length === 0 ? (
            <EmptyState icon="bi-box-seam" title="Lô này không còn hàng trong kho"
              hint="Nếu hàng đang trên kệ, hãy lấy về kho ở trang Kệ trước khi xuất hủy." />
          ) : (
            <>
              <Form.Group className="mb-3">
                <Form.Label>Chọn lô (theo hạn sử dụng)</Form.Label>
                <Form.Select value={batchId} onChange={(e) => setBatchId(e.target.value)}>
                  {batches.map((b) => (
                    <option key={b.batchId} value={b.batchId}>
                      Lô #{b.batchId} · trong kho {b.inWarehouse}
                      {b.expiryDate ? ` · HSD ${b.expiryDate}${b.daysLeft < 0 ? ' (đã quá hạn)' : ''}` : ' · không HSD'}
                    </option>
                  ))}
                </Form.Select>
                {chosen && <div className="mt-2"><ExpiryPill days={chosen.daysLeft} date={chosen.expiryDate} /></div>}
              </Form.Group>

              <Form.Group className="mb-3">
                <Form.Label>Số lượng xuất hủy <span className="text-muted2">(nhiều nhất {maxQty})</span></Form.Label>
                <div className="input-group">
                  <Form.Control type="number" min={1} max={maxQty} value={qty} isInvalid={qtyInvalid}
                    onChange={(e) => { const v = e.target.value; setQty(v === '' ? '' : Math.min(Math.max(0, Math.floor(Number(v) || 0)), maxQty)) }}
                    onBlur={() => setQty((x) => { const n = Number(x); return n >= 1 ? Math.min(n, maxQty) : 1 })} />
                  <Button variant="outline-secondary" type="button" disabled={maxQty <= 0} onClick={() => setQty(maxQty)}>Toàn bộ</Button>
                </div>
              </Form.Group>

              <Form.Group className="mb-3">
                <Form.Label>Lý do</Form.Label>
                <Form.Select value={reason} onChange={(e) => setReason(e.target.value)}>
                  {Object.entries(REASONS).map(([k, v]) => <option key={k} value={k}>{v.label}</option>)}
                </Form.Select>
                {expired && reason !== 'EXPIRED' && (
                  <Form.Text className="text-danger">Lô này đã quá hạn — nên chọn lý do "Hết hạn sử dụng".</Form.Text>
                )}
              </Form.Group>

              <Form.Group>
                <Form.Label>Ghi chú <span className="text-muted2">(không bắt buộc)</span></Form.Label>
                <Form.Control as="textarea" rows={2} maxLength={255} value={note} onChange={(e) => setNote(e.target.value)}
                  placeholder="Vd: hộp bị móp, kiểm kê thiếu 2 lon…" />
              </Form.Group>
            </>
          )}
        </Modal.Body>
        <Modal.Footer>
          <Button variant="light" onClick={onHide}>Hủy</Button>
          <Button type="submit" variant="danger" disabled={loading || batches.length === 0 || qtyInvalid}>
            {loading ? <Spinner size="sm" /> : 'Xác nhận xuất hủy'}
          </Button>
        </Modal.Footer>
      </Form>
    </Modal>
  )
}
