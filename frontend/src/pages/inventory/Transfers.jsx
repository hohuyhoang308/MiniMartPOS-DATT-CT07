import { useEffect, useState } from 'react'
import { Button, Card, Col, Form, Modal, Row, Spinner, Table } from 'react-bootstrap'
import PageHeader from '../../components/ui/PageHeader'
import InfoBanner from '../../components/ui/InfoBanner'
import EmptyState from '../../components/ui/EmptyState'
import ConfirmModal from '../../components/ui/ConfirmModal'
import { SkeletonRows } from '../../components/ui/Loading'
import { transferApi, storeApi, inventoryApi } from '../../api/misc'
import { productApi } from '../../api/catalog'
import { useStoreScope } from '../../context/StoreScopeContext'
import { useToast } from '../../context/ToastContext'
import { errMsg } from '../../api/client'
import { formatDateTime } from '../../utils/format'
import { useList } from '../../hooks/useList'

/** Trạng thái phiếu điều chuyển → nhãn + màu pill (đồng bộ enum TransferStatus ở backend). */
const STATUS = {
  PENDING: { label: 'Chờ xuất hàng', cls: 'pill-warning', icon: 'bi-hourglass-split' },
  SHIPPING: { label: 'Đang chuyển', cls: 'pill-info', icon: 'bi-truck' },
  RECEIVED: { label: 'Đã nhận', cls: 'pill-success', icon: 'bi-check-circle-fill' },
  CANCELLED: { label: 'Đã hủy', cls: 'pill-danger', icon: 'bi-x-circle-fill' },
}

const emptyLine = () => ({ productId: '', batchId: '', quantity: 1 })

export default function Transfers({ embedded = false }) {
  const toast = useToast()
  const { scopeStoreId } = useStoreScope()
  const { data: list, loading, reload: load } = useList(transferApi.list)
  const [creating, setCreating] = useState(false)
  const [detail, setDetail] = useState(null)
  const [busyId, setBusyId] = useState(null)
  const [cancelTarget, setCancelTarget] = useState(null)

  // Cửa hàng đang xem (MANAGER: cố định; ADMIN: chi nhánh đã chọn ở phù hiệu phạm vi).
  const scope = scopeStoreId ? String(scopeStoreId) : ''
  const isSource = (t) => scope && String(t.sourceStoreId) === scope
  const isDest = (t) => scope && String(t.destStoreId) === scope

  async function act(t, action) {
    setBusyId(t.id)
    try {
      if (action === 'ship') { await transferApi.ship(t.id); toast.success(`Đã xuất hàng phiếu ${t.code}`) }
      else if (action === 'receive') { await transferApi.receive(t.id); toast.success(`Đã nhận hàng phiếu ${t.code}`) }
      load()
    } catch (e) { toast.error(errMsg(e)) } finally { setBusyId(null) }
  }

  async function doCancel(reason) {
    const t = cancelTarget
    setBusyId(t.id)
    try {
      await transferApi.cancel(t.id, reason)
      toast.success(`Đã hủy phiếu ${t.code}`)
      setCancelTarget(null); load()
    } catch (e) { toast.error(errMsg(e)) } finally { setBusyId(null) }
  }

  return (
    <div>
      {embedded ? (
        <div className="d-flex justify-content-end mb-3">
          <Button onClick={() => setCreating(true)}><i className="bi bi-plus-lg me-1"></i>Tạo phiếu điều chuyển</Button>
        </div>
      ) : (
        <PageHeader title="Điều chuyển kho" subtitle="Chuyển hàng giữa các chi nhánh: chi nhánh nguồn xuất hàng, chi nhánh đích nhận hàng vào kho.">
          <Button onClick={() => setCreating(true)}><i className="bi bi-plus-lg me-1"></i>Tạo phiếu điều chuyển</Button>
        </PageHeader>
      )}

      <InfoBanner id="stock-transfers" title="Điều chuyển kho hoạt động ra sao?">
        Khi một chi nhánh thừa hàng còn chi nhánh khác thiếu, bạn lập một phiếu điều chuyển từ
        <b> chi nhánh của bạn (nguồn)</b> sang <b>chi nhánh đích</b>. Quy trình gồm 3 bước:
        bên nguồn bấm <b>"Xuất hàng"</b> (trừ kho nguồn), hàng <b>đang chuyển</b>, rồi bên đích bấm
        <b> "Nhận hàng"</b> (cộng vào kho đích). Mỗi bước chỉ làm được ở đúng chi nhánh tương ứng.
      </InfoBanner>

      <div className="table-wrap fade-up">
        <Table hover className="mb-0 align-middle">
          <thead><tr>
            <th>Mã phiếu</th><th>Từ chi nhánh</th><th>Đến chi nhánh</th>
            <th className="text-center">Số dòng</th><th>Trạng thái</th><th>Tạo lúc</th><th className="text-end">Thao tác</th>
          </tr></thead>
          {loading ? <SkeletonRows cols={7} /> : (
            <tbody>
              {list.map((t) => {
                const s = STATUS[t.status] || STATUS.PENDING
                const canShip = t.status === 'PENDING' && isSource(t)
                const canReceive = t.status === 'SHIPPING' && isDest(t)
                const canCancel = t.status === 'PENDING' && isSource(t)
                const busy = busyId === t.id
                return (
                  <tr key={t.id}>
                    <td className="fw-semibold cursor-pointer" onClick={() => setDetail(t)}>{t.code}</td>
                    <td>{t.sourceStoreName}</td>
                    <td>{t.destStoreName}</td>
                    <td className="text-center num">{t.items?.length || 0}</td>
                    <td><span className={`pill ${s.cls}`}><i className={`bi ${s.icon}`}></i>{s.label}</span></td>
                    <td className="text-muted2 small" style={{ whiteSpace: 'nowrap' }}>{formatDateTime(t.createdAt)}</td>
                    <td className="text-end">
                      <div className="d-inline-flex gap-2">
                        {canShip && (
                          <Button size="sm" variant="outline-primary" disabled={busy} onClick={() => act(t, 'ship')}>
                            {busy ? <Spinner size="sm" /> : <><i className="bi bi-box-arrow-up me-1"></i>Xuất hàng</>}
                          </Button>
                        )}
                        {canReceive && (
                          <Button size="sm" variant="outline-success" disabled={busy} onClick={() => act(t, 'receive')}>
                            {busy ? <Spinner size="sm" /> : <><i className="bi bi-box-arrow-in-down me-1"></i>Nhận hàng</>}
                          </Button>
                        )}
                        {canCancel && (
                          <Button size="sm" variant="outline-danger" disabled={busy} onClick={() => setCancelTarget(t)}>
                            <i className="bi bi-x-lg me-1"></i>Hủy
                          </Button>
                        )}
                        {!canShip && !canReceive && !canCancel && (
                          <Button size="sm" variant="light" onClick={() => setDetail(t)}>Chi tiết</Button>
                        )}
                      </div>
                    </td>
                  </tr>
                )
              })}
            </tbody>
          )}
        </Table>
        {!loading && list.length === 0 && <EmptyState icon="bi-arrow-left-right" title="Chưa có phiếu điều chuyển nào" />}
      </div>

      <CreateModal show={creating} onHide={() => setCreating(false)} onDone={() => { setCreating(false); load() }} />

      <ConfirmModal show={!!cancelTarget} onHide={() => setCancelTarget(null)} loading={busyId === cancelTarget?.id}
        title="Hủy phiếu điều chuyển" confirmText="Hủy phiếu" icon="bi-x-octagon"
        message={`Hủy phiếu ${cancelTarget?.code}? Hàng vẫn ở kho chi nhánh nguồn.`}
        onConfirm={() => doCancel('Hủy bởi người dùng')} />

      <DetailModal transfer={detail} onHide={() => setDetail(null)} />
    </div>
  )
}

/** Modal tạo phiếu: chọn chi nhánh đích, thêm dòng (tìm sản phẩm → chọn lô còn trong kho → số lượng). */
function CreateModal({ show, onHide, onDone }) {
  const toast = useToast()
  const { scopeStoreId } = useStoreScope()
  const [stores, setStores] = useState([])
  const [products, setProducts] = useState([])
  const [batchesByProduct, setBatchesByProduct] = useState({}) // productId -> [batch]
  const [destStoreId, setDestStoreId] = useState('')
  const [note, setNote] = useState('')
  const [items, setItems] = useState([emptyLine()])
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    if (!show) return
    setDestStoreId(''); setNote(''); setItems([emptyLine()])
    storeApi.list().then(setStores).catch(() => setStores([]))
    productApi.list().then(setProducts).catch(() => setProducts([]))
  }, [show])

  // Tải các lô còn trong kho của 1 sản phẩm (chỉ khi chưa có cache).
  function ensureBatches(productId) {
    if (!productId || batchesByProduct[productId]) return
    inventoryApi.batches(productId)
      .then((bs) => setBatchesByProduct((m) => ({ ...m, [productId]: bs.filter((b) => b.inWarehouse > 0) })))
      .catch(() => setBatchesByProduct((m) => ({ ...m, [productId]: [] })))
  }

  const setItem = (idx, patch) => setItems((arr) => arr.map((it, i) => i === idx ? { ...it, ...patch } : it))

  function onProduct(idx, productId) {
    setItem(idx, { productId, batchId: '', quantity: 1 })
    ensureBatches(productId)
  }

  // Bộ chọn chi nhánh đích: loại trừ cửa hàng nguồn (chi nhánh đang xem).
  const scope = scopeStoreId ? String(scopeStoreId) : ''
  const destOptions = stores.filter((s) => String(s.id) !== scope)

  async function submit(e) {
    e.preventDefault()
    if (!destStoreId) { toast.warning('Vui lòng chọn chi nhánh đích'); return }
    if (items.some((it) => !it.productId || !it.batchId)) { toast.warning('Vui lòng chọn sản phẩm và lô cho mọi dòng'); return }
    if (items.some((it) => Number(it.quantity) < 1)) { toast.warning('Số lượng mỗi dòng phải lớn hơn 0'); return }
    setSaving(true)
    try {
      await transferApi.create({
        destStoreId: Number(destStoreId),
        note: note.trim() || null,
        items: items.map((it) => ({ batchId: Number(it.batchId), quantity: Number(it.quantity) })),
      })
      toast.success('Đã tạo phiếu điều chuyển. Bấm "Xuất hàng" để trừ kho và bắt đầu chuyển.')
      onDone()
    } catch (e) { toast.error(errMsg(e)) } finally { setSaving(false) }
  }

  return (
    <Modal show={show} onHide={onHide} centered size="lg">
      <Form onSubmit={submit}>
        <Modal.Header closeButton><Modal.Title>Tạo phiếu điều chuyển kho</Modal.Title></Modal.Header>
        <Modal.Body>
          <Row className="mb-3">
            <Col md={6}>
              <Form.Label>Chuyển đến chi nhánh *</Form.Label>
              <Form.Select value={destStoreId} onChange={(e) => setDestStoreId(e.target.value)}>
                <option value="">— chọn chi nhánh đích —</option>
                {destOptions.map((s) => <option key={s.id} value={s.id}>{s.name}</option>)}
              </Form.Select>
            </Col>
            <Col md={6}>
              <Form.Label>Ghi chú</Form.Label>
              <Form.Control value={note} onChange={(e) => setNote(e.target.value)} placeholder="Vd: tiếp hàng cho chi nhánh thiếu…" />
            </Col>
          </Row>

          <Table size="sm" className="align-middle">
            <thead><tr>
              <th style={{ width: '42%' }}>Sản phẩm</th><th>Lô (còn trong kho)</th><th style={{ width: 110 }}>Số lượng</th><th></th>
            </tr></thead>
            <tbody>
              {items.map((it, idx) => {
                const batches = batchesByProduct[it.productId] || []
                const chosen = batches.find((b) => String(b.batchId) === String(it.batchId))
                const max = chosen ? chosen.inWarehouse : 0
                return (
                  <tr key={idx}>
                    <td>
                      <Form.Select size="sm" value={it.productId} onChange={(e) => onProduct(idx, e.target.value)}>
                        <option value="">— chọn —</option>
                        {products.map((p) => <option key={p.id} value={p.id}>{p.name} ({p.barcode})</option>)}
                      </Form.Select>
                    </td>
                    <td>
                      <Form.Select size="sm" value={it.batchId} disabled={!it.productId}
                        onChange={(e) => setItem(idx, { batchId: e.target.value, quantity: 1 })}>
                        <option value="">{it.productId ? '— chọn lô —' : '— chọn sản phẩm trước —'}</option>
                        {batches.map((b) => (
                          <option key={b.batchId} value={b.batchId}>
                            Lô #{b.batchId} · còn {b.inWarehouse}{b.expiryDate ? ` · HSD ${b.expiryDate}` : ''}
                          </option>
                        ))}
                      </Form.Select>
                      {it.productId && batches.length === 0 && (
                        <div className="text-danger" style={{ fontSize: '.72rem' }}>Không còn hàng trong kho</div>
                      )}
                    </td>
                    <td>
                      <Form.Control size="sm" type="number" min={1} max={max || undefined} value={it.quantity}
                        disabled={!it.batchId}
                        onChange={(e) => setItem(idx, { quantity: e.target.value })} />
                      {chosen && <div className="text-muted2" style={{ fontSize: '.72rem' }}>nhiều nhất {max}</div>}
                    </td>
                    <td className="text-end">
                      <Button size="sm" variant="light" className="text-danger" disabled={items.length === 1}
                        onClick={() => setItems((arr) => arr.filter((_, i) => i !== idx))}>
                        <i className="bi bi-x-lg"></i>
                      </Button>
                    </td>
                  </tr>
                )
              })}
            </tbody>
          </Table>
          <Button size="sm" variant="soft" onClick={() => setItems((arr) => [...arr, emptyLine()])}>
            <i className="bi bi-plus-lg me-1"></i>Thêm dòng
          </Button>
        </Modal.Body>
        <Modal.Footer>
          <Button variant="light" onClick={onHide}>Hủy</Button>
          <Button type="submit" disabled={saving}>{saving ? <Spinner size="sm" /> : 'Tạo phiếu'}</Button>
        </Modal.Footer>
      </Form>
    </Modal>
  )
}

/** Modal chi tiết: dòng hàng + mốc thời gian xuất/nhận. */
function DetailModal({ transfer, onHide }) {
  if (!transfer) return null
  const s = STATUS[transfer.status] || STATUS.PENDING
  return (
    <Modal show={!!transfer} onHide={onHide} centered size="lg">
      <Modal.Header closeButton><Modal.Title>Phiếu điều chuyển {transfer.code}</Modal.Title></Modal.Header>
      <Modal.Body>
        <div className="d-flex justify-content-between flex-wrap gap-2 mb-3 small text-muted2">
          <span><i className="bi bi-shop me-1"></i>{transfer.sourceStoreName} <i className="bi bi-arrow-right mx-1"></i>{transfer.destStoreName}</span>
          <span className={`pill ${s.cls}`}><i className={`bi ${s.icon}`}></i>{s.label}</span>
        </div>
        <div className="d-flex justify-content-between flex-wrap gap-3 mb-3 small text-muted2">
          <span><i className="bi bi-clock me-1"></i>Tạo: {formatDateTime(transfer.createdAt)}</span>
          {transfer.shippedAt && <span><i className="bi bi-box-arrow-up me-1"></i>Xuất: {formatDateTime(transfer.shippedAt)}</span>}
          {transfer.receivedAt && <span><i className="bi bi-box-arrow-in-down me-1"></i>Nhận: {formatDateTime(transfer.receivedAt)}</span>}
        </div>
        <Table size="sm" hover>
          <thead><tr><th>Sản phẩm</th><th className="text-center">Số lượng</th><th>HSD</th><th>Lô</th></tr></thead>
          <tbody>
            {transfer.items?.map((it, i) => (
              <tr key={it.batchId ?? i}>
                <td>{it.productName}</td>
                <td className="text-center num">{it.quantity}</td>
                <td>{it.expiryDate || '—'}</td>
                <td className="text-muted2 small">#{it.batchId}</td>
              </tr>
            ))}
          </tbody>
        </Table>
        {transfer.note && <div className="text-muted2 small mt-2"><i className="bi bi-sticky me-1"></i>{transfer.note}</div>}
      </Modal.Body>
    </Modal>
  )
}
