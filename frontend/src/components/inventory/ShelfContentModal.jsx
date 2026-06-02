import { useEffect, useState } from 'react'
import { Button, Form, Modal, Spinner, Table } from 'react-bootstrap'
import Loading from '../ui/Loading'
import EmptyState from '../ui/EmptyState'
import ExpiryPill from '../ui/ExpiryPill'
import { shelfApi } from '../../api/misc'
import { useToast } from '../../context/ToastContext'
import { errMsg } from '../../api/client'

/**
 * Modal xem một KỆ đang chứa gì (sản phẩm/lô/HSD) + lấy hàng từ kệ VỀ KHO.
 * Dùng chung cho trang "Lên kệ" (thu ngân) và "Cấu hình kệ" (quản lý).
 * Thao tác về kho cho mọi vai trò có ca (backend đã cho phép).
 */
export default function ShelfContentModal({ shelf, onHide, onChanged }) {
  const toast = useToast()
  const [items, setItems] = useState([])
  const [loading, setLoading] = useState(false)
  const [ret, setRet] = useState(null) // lô đang lấy về kho

  function load() {
    if (!shelf) return
    setLoading(true)
    shelfApi.inventory(shelf.id).then(setItems).catch((e) => toast.error(errMsg(e))).finally(() => setLoading(false))
  }
  useEffect(() => { load() }, [shelf])

  if (!shelf) return null

  return (
    <>
      <Modal show={!!shelf && !ret} onHide={onHide} centered size="lg">
        <Modal.Header closeButton><Modal.Title>Kệ {shelf.code}{shelf.name ? ` · ${shelf.name}` : ''}</Modal.Title></Modal.Header>
        <Modal.Body>
          {loading ? <Loading /> : (
            <Table size="sm" hover className="mb-0 align-middle">
              <thead><tr><th>Sản phẩm</th><th>HSD (lô)</th><th className="text-end">Trên kệ</th><th className="text-end">Thao tác</th></tr></thead>
              <tbody>
                {items.map((it) => (
                  <tr key={it.batchId}>
                    <td className="fw-semibold">{it.productName}</td>
                    <td><ExpiryPill days={it.daysLeft} date={it.expiryDate} /></td>
                    <td className="text-end num fw-semibold text-success">{it.quantity}</td>
                    <td className="text-end">
                      <Button size="sm" variant="light" className="text-primary" title="Lấy hàng từ kệ về kho"
                        onClick={() => setRet({ ...it, qty: it.quantity })}>
                        <i className="bi bi-arrow-down-square me-1"></i>Về kho
                      </Button>
                    </td>
                  </tr>
                ))}
                {items.length === 0 && <tr><td colSpan={4}><EmptyState icon="bi-inboxes" title="Kệ đang trống" /></td></tr>}
              </tbody>
            </Table>
          )}
        </Modal.Body>
      </Modal>

      <ShelfReturnModal item={ret} onHide={() => setRet(null)}
        onDone={() => { setRet(null); load(); onChanged?.() }} />
    </>
  )
}

/** Modal nhập số lượng lấy một LÔ từ kệ về kho. */
function ShelfReturnModal({ item, onHide, onDone }) {
  const toast = useToast()
  const [qty, setQty] = useState(0)
  const [loading, setLoading] = useState(false)

  useEffect(() => { if (item) setQty(item.quantity) }, [item])
  if (!item) return null
  const max = item.quantity
  const qtyNum = Number(qty) || 0
  const qtyInvalid = qtyNum < 1 || qtyNum > max

  async function submit(e) {
    e.preventDefault()
    if (qtyInvalid) { toast.warning(`Nhập số lượng từ 1 đến ${max}`); return }
    setLoading(true)
    try {
      const n = await shelfApi.returnToWarehouse(item.batchId, Number(qty))
      toast.success(`Đã lấy ${n} sản phẩm từ kệ về kho`)
      onDone()
    } catch (e) { toast.error(errMsg(e)) } finally { setLoading(false) }
  }

  return (
    <Modal show={!!item} onHide={onHide} centered size="sm">
      <Form onSubmit={submit}>
        <Modal.Header closeButton><Modal.Title>Lấy về kho</Modal.Title></Modal.Header>
        <Modal.Body>
          <div className="mb-2 fw-semibold">{item.productName}</div>
          <div className="small text-muted2 mb-3">Đang trên kệ: <b className="text-success">{max}</b> · lấy bớt về kho để nhường chỗ / đổi hàng cận hạn.</div>
          <Form.Label>Số lượng lấy về kho <span className="text-muted2">(tối đa {max})</span></Form.Label>
          <div className="input-group">
            <Form.Control type="number" min={1} max={max} value={qty} isInvalid={qtyInvalid}
              onChange={(e) => { const v = e.target.value; setQty(v === '' ? '' : Math.min(Math.max(0, Math.floor(Number(v) || 0)), max)) }}
              onBlur={() => setQty((q) => { const n = Number(q); return n >= 1 ? Math.min(n, max) : 1 })} />
            <Button variant="outline-secondary" type="button" onClick={() => setQty(max)}>Tất cả</Button>
          </div>
        </Modal.Body>
        <Modal.Footer>
          <Button variant="light" onClick={onHide}>Hủy</Button>
          <Button type="submit" disabled={loading || max <= 0 || qtyInvalid}>{loading ? <Spinner size="sm" /> : 'Lấy về kho'}</Button>
        </Modal.Footer>
      </Form>
    </Modal>
  )
}
