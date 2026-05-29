import { useEffect, useState } from 'react'
import { Button, Form, Modal, Spinner, Table } from 'react-bootstrap'
import PageHeader from '../../components/ui/PageHeader'
import InfoBanner from '../../components/ui/InfoBanner'
import StatusPill from '../../components/ui/StatusPill'
import EmptyState from '../../components/ui/EmptyState'
import Loading from '../../components/ui/Loading'
import ConfirmModal from '../../components/ui/ConfirmModal'
import ExpiryPill from '../../components/ui/ExpiryPill'
import { shelfApi } from '../../api/misc'
import { useToast } from '../../context/ToastContext'
import { errMsg } from '../../api/client'

const EMPTY = { code: '', name: '', capacity: 200, status: 'ACTIVE' }

export default function ShelfManage() {
  const toast = useToast()
  const [shelves, setShelves] = useState([])
  const [loading, setLoading] = useState(true)
  const [form, setForm] = useState(null)
  const [saving, setSaving] = useState(false)
  const [del, setDel] = useState(null)
  const [detail, setDetail] = useState(null)

  function load() {
    setLoading(true)
    shelfApi.list().then(setShelves).catch((e) => toast.error(errMsg(e))).finally(() => setLoading(false))
  }
  useEffect(() => { load() }, [])

  async function save(e) {
    e.preventDefault(); setSaving(true)
    try {
      if (form.id) { await shelfApi.update(form.id, form); toast.success('Đã cập nhật kệ') }
      else { await shelfApi.create(form); toast.success('Đã thêm kệ') }
      setForm(null); load()
    } catch (e) { toast.error(errMsg(e)) } finally { setSaving(false) }
  }
  async function remove() {
    try { await shelfApi.remove(del.id); toast.success('Đã xóa kệ'); setDel(null); load() }
    catch (e) { toast.error(errMsg(e)); setDel(null) }
  }

  if (loading) return <Loading />

  return (
    <div>
      <PageHeader title="Quản lý kệ" subtitle="Các kệ trưng bày trong cửa hàng — xem kệ chứa gì, lô/HSD nào">
        <Button onClick={() => setForm({ ...EMPTY })}><i className="bi bi-plus-lg me-1"></i>Thêm kệ</Button>
      </PageHeader>

      <InfoBanner id="shelfmanage" title="Kệ trưng bày">
        Mỗi <b>kệ</b> có <b>mã</b> (K01, A1…) và khu vực. Bấm vào một kệ để xem <b>kệ đang chứa sản phẩm gì</b>,
        từ <b>lô nào</b>, <b>HSD</b> bao nhiêu — và bấm <b>"Về kho"</b> để lấy bớt hàng từ kệ trả lại kho.
        Việc đưa hàng từ kho <b>lên kệ</b> làm ở trang <b>Lên kệ</b>.
      </InfoBanner>

      <div className="table-wrap fade-up">
        <Table hover className="mb-0 align-middle">
          <thead><tr>
            <th>Mã kệ</th><th>Khu vực</th><th className="text-center">Số mặt hàng</th>
            <th className="text-center" style={{ minWidth: 160 }}>Đang chứa / Sức chứa</th><th className="text-center">Trạng thái</th><th className="text-end">Thao tác</th>
          </tr></thead>
          <tbody>
            {shelves.map((s) => {
              const cap = s.capacity ?? 0
              const pct = cap > 0 ? Math.min(100, Math.round((s.totalQuantity / cap) * 100)) : 0
              return (
              <tr key={s.id}>
                <td className="fw-semibold cursor-pointer" onClick={() => setDetail(s)}>
                  <span className="stat-chip chip-sky me-2" style={{ width: 32, height: 32, fontSize: '.8rem' }}><i className="bi bi-grid-3x3-gap"></i></span>{s.code}
                </td>
                <td className="text-muted2">{s.name || '—'}</td>
                <td className="text-center num">{s.productCount}</td>
                <td className="text-center">
                  <div className="num small">{s.totalQuantity} / {cap > 0 ? cap : '∞'}</div>
                  {cap > 0 && (
                    <div className="progress mt-1" style={{ height: 5, background: '#eef2f7' }}>
                      <div className="progress-bar" style={{ width: `${pct}%`, background: pct >= 100 ? '#f43f5e' : pct >= 80 ? '#f59e0b' : 'var(--brand-500)' }} />
                    </div>
                  )}
                </td>
                <td className="text-center"><StatusPill value={s.status} /></td>
                <td className="text-end">
                  <Button size="sm" variant="light" className="me-1" onClick={() => setDetail(s)} title="Xem kệ chứa gì"><i className="bi bi-eye"></i></Button>
                  <Button size="sm" variant="light" className="me-1" onClick={() => setForm({ ...s })}><i className="bi bi-pencil"></i></Button>
                  <Button size="sm" variant="light" className="text-danger" onClick={() => setDel(s)}><i className="bi bi-trash"></i></Button>
                </td>
              </tr>
            )})}
            {shelves.length === 0 && <tr><td colSpan={6}><EmptyState icon="bi-grid-3x3-gap" title="Chưa có kệ" /></td></tr>}
          </tbody>
        </Table>
      </div>

      {/* Thêm/sửa kệ */}
      <Modal show={!!form} onHide={() => setForm(null)} centered size="sm">
        <Form onSubmit={save}>
          <Modal.Header closeButton><Modal.Title>{form?.id ? 'Sửa kệ' : 'Thêm kệ'}</Modal.Title></Modal.Header>
          <Modal.Body>
            <Form.Group className="mb-3"><Form.Label>Mã kệ *</Form.Label>
              <Form.Control required value={form?.code || ''} onChange={(e) => setForm({ ...form, code: e.target.value })} placeholder="vd: K01, A1" /></Form.Group>
            <Form.Group className="mb-3"><Form.Label>Khu vực / tên</Form.Label>
              <Form.Control value={form?.name || ''} onChange={(e) => setForm({ ...form, name: e.target.value })} placeholder="vd: Nước giải khát" /></Form.Group>
            <Form.Group className="mb-3"><Form.Label>Sức chứa tối đa (0 = không giới hạn)</Form.Label>
              <Form.Control type="number" min={0} value={form?.capacity ?? 0} onChange={(e) => setForm({ ...form, capacity: Number(e.target.value) })} /></Form.Group>
            <Form.Group><Form.Label>Trạng thái</Form.Label>
              <Form.Select value={form?.status} onChange={(e) => setForm({ ...form, status: e.target.value })}>
                <option value="ACTIVE">Đang dùng</option><option value="INACTIVE">Ngừng</option>
              </Form.Select></Form.Group>
          </Modal.Body>
          <Modal.Footer>
            <Button variant="light" onClick={() => setForm(null)}>Hủy</Button>
            <Button type="submit" disabled={saving}>{saving ? <Spinner size="sm" /> : 'Lưu'}</Button>
          </Modal.Footer>
        </Form>
      </Modal>

      <ShelfContentModal shelf={detail} onHide={() => setDetail(null)} onChanged={load} />

      <ConfirmModal show={!!del} onHide={() => setDel(null)} onConfirm={remove}
        title="Xóa kệ" message={`Xóa kệ "${del?.code}"? Không xóa được nếu kệ còn hàng.`} confirmText="Xóa" />
    </div>
  )
}

/** Modal xem kệ đang chứa sản phẩm gì, lô nào, HSD — và lấy hàng từ kệ về kho. */
function ShelfContentModal({ shelf, onHide, onChanged }) {
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
