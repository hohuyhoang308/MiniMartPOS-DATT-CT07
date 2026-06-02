import { useEffect, useMemo, useState } from 'react'
import { Button, Card, Col, Form, Modal, Row, Spinner, Table } from 'react-bootstrap'
import PageHeader from '../../components/ui/PageHeader'
import InfoBanner from '../../components/ui/InfoBanner'
import StatCard from '../../components/ui/StatCard'
import EmptyState from '../../components/ui/EmptyState'
import Loading from '../../components/ui/Loading'
import ExpiryPill from '../../components/ui/ExpiryPill'
import ShelfContentModal from '../../components/inventory/ShelfContentModal'
import { productApi } from '../../api/catalog'
import { inventoryApi, shelfApi } from '../../api/misc'
import { useToast } from '../../context/ToastContext'
import { errMsg } from '../../api/client'

const needShelf = (p) => (p.shelfStock ?? 0) <= (p.minStock ?? 0) && (p.warehouseStock ?? 0) > 0

export default function Shelf() {
  const toast = useToast()
  const [products, setProducts] = useState([])
  const [shelves, setShelves] = useState([])
  const [loading, setLoading] = useState(true)
  const [q, setQ] = useState('')
  const [onlyNeed, setOnlyNeed] = useState(true)
  const [target, setTarget] = useState(null)
  const [shelfDetail, setShelfDetail] = useState(null) // kệ đang xem nội dung / về kho

  function load(first) {
    if (first) setLoading(true)
    Promise.all([productApi.list(), shelfApi.list()])
      .then(([ps, sh]) => { setProducts(ps.filter((p) => p.status === 'ACTIVE')); setShelves(sh) })
      .catch((e) => toast.error(errMsg(e))).finally(() => setLoading(false))
  }
  useEffect(() => { load(true) }, [])

  const needCount = useMemo(() => products.filter(needShelf).length, [products])
  const rows = useMemo(() => {
    const kw = q.trim().toLowerCase()
    let list = products.filter((p) => (p.warehouseStock ?? 0) > 0)
    if (onlyNeed) list = list.filter(needShelf)
    if (kw) list = list.filter((p) => p.name.toLowerCase().includes(kw) || p.barcode.includes(kw))
    return list.sort((a, b) => (needShelf(b) - needShelf(a)) || (a.shelfStock ?? 0) - (b.shelfStock ?? 0))
  }, [products, q, onlyNeed])

  if (loading) return <Loading />

  return (
    <div>
      <PageHeader title="Kệ hàng (lên kệ / về kho)" subtitle="Thao tác kệ hằng ngày của thu ngân — đưa hàng ra kệ để bán hoặc lấy bớt về kho" />

      <InfoBanner id="shelf" title="Lên kệ & Về kho">
        Hàng nhập về nằm trong <b>KHO</b>; muốn bán phải đưa ra <b>KỆ</b>. Khi <b>kệ cạn</b> (≤ tối thiểu) mà
        <b> kho còn</b> → bấm <b>"Lên kệ"</b> (tự lấy <b>lô cận hạn trước - FIFO</b>). Ngược lại, mở một <b>kệ</b> ở
        mục <b>"Kệ trong cửa hàng"</b> bên dưới để xem kệ chứa gì và bấm <b>"Về kho"</b> (đặt xuống). Đây là thao
        tác hằng ngày của <b>thu ngân</b>; còn <b>thêm/sửa/xoá kệ</b> là việc của <b>quản lý</b> (trang Cấu hình kệ).
      </InfoBanner>

      <Row className="g-3 mb-3 stagger">
        <Col md={4}><StatCard icon="bi-arrow-up-square-fill" chip="emerald" label="Mặt hàng cần lên kệ" value={needCount} /></Col>
        <Col md={4}><StatCard icon="bi-grid-3x3-gap-fill" chip="sky" label="Số kệ" value={shelves.length} /></Col>
        <Col md={4}><StatCard icon="bi-box-seam" chip="violet" label="Tổng mặt hàng" value={products.length} /></Col>
      </Row>

      <Card className="border-0">
        <Card.Body className="d-flex justify-content-between align-items-center flex-wrap gap-2">
          <div className="input-group" style={{ maxWidth: 320 }}>
            <span className="input-group-text"><i className="bi bi-search"></i></span>
            <Form.Control placeholder="Tìm sản phẩm / mã vạch…" value={q} onChange={(e) => setQ(e.target.value)} />
          </div>
          <Form.Check type="switch" id="onlyNeed" label="Chỉ hiện hàng cần lên kệ" checked={onlyNeed} onChange={(e) => setOnlyNeed(e.target.checked)} />
        </Card.Body>
        <div className="table-responsive" style={{ maxHeight: 460, overflowY: 'auto' }}>
          <Table hover className="mb-0 align-middle">
            <thead><tr>
              <th>Sản phẩm</th><th className="text-center">Trên kệ</th><th className="text-center">Trong kho</th>
              <th className="text-center">Trạng thái</th><th className="text-end">Thao tác</th>
            </tr></thead>
            <tbody>
              {rows.map((p) => {
                const shelf = p.shelfStock ?? 0, wh = p.warehouseStock ?? 0
                return (
                  <tr key={p.id}>
                    <td className="fw-semibold">{p.name}<div className="text-muted2 small">{p.barcode}</div></td>
                    <td className="text-center num"><span className={`fw-semibold ${shelf <= 0 ? 'text-danger' : shelf <= p.minStock ? 'text-warning' : 'text-success'}`}>{shelf}</span></td>
                    <td className="text-center num text-primary">{wh}</td>
                    <td className="text-center">
                      {shelf <= 0 ? <span className="pill pill-danger"><i className="bi bi-x-octagon-fill"></i>Hết kệ</span>
                        : needShelf(p) ? <span className="pill pill-warning"><i className="bi bi-arrow-up"></i>Cần lên kệ</span>
                        : <span className="pill pill-success"><i className="bi bi-check-circle-fill"></i>Đủ kệ</span>}
                    </td>
                    <td className="text-end"><Button size="sm" onClick={() => setTarget(p)}><i className="bi bi-arrow-up me-1"></i>Lên kệ</Button></td>
                  </tr>
                )
              })}
              {rows.length === 0 && <tr><td colSpan={5}><EmptyState icon="bi-check2-circle" title={onlyNeed ? 'Kệ đầy đủ — không có gì cần lên' : 'Không có hàng trong kho'} /></td></tr>}
            </tbody>
          </Table>
        </div>
      </Card>

      <h6 className="fw-bold mt-4 mb-2"><i className="bi bi-grid-3x3-gap me-2"></i>Kệ trong cửa hàng <span className="text-muted2 fw-normal small">— bấm để xem kệ chứa gì & lấy hàng về kho</span></h6>
      <Row className="g-2">
        {shelves.map((s) => {
          const cap = s.capacity ?? 0
          const pct = cap > 0 ? Math.min(100, Math.round((s.totalQuantity / cap) * 100)) : 0
          return (
            <Col md={3} sm={6} key={s.id}>
              <Card className="border-0 h-100 cursor-pointer" onClick={() => setShelfDetail(s)}>
                <Card.Body className="py-2 px-3">
                  <div className="d-flex justify-content-between align-items-center">
                    <span className="fw-semibold"><span className="stat-chip chip-sky me-2" style={{ width: 28, height: 28, fontSize: '.72rem' }}><i className="bi bi-grid-3x3-gap"></i></span>{s.code}</span>
                    <span className="num small text-muted2">{s.totalQuantity}/{cap > 0 ? cap : '∞'}</span>
                  </div>
                  <div className="text-muted2 small text-truncate">{s.name || '—'} · {s.productCount} mặt hàng</div>
                  {cap > 0 && (
                    <div className="progress mt-1" style={{ height: 4, background: '#eef2f7' }}>
                      <div className="progress-bar" style={{ width: `${pct}%`, background: pct >= 100 ? '#f43f5e' : pct >= 80 ? '#f59e0b' : 'var(--brand-500)' }} />
                    </div>
                  )}
                </Card.Body>
              </Card>
            </Col>
          )
        })}
        {shelves.length === 0 && <Col><EmptyState icon="bi-grid-3x3-gap" title="Chưa có kệ — nhờ quản lý tạo ở trang Cấu hình kệ" /></Col>}
      </Row>

      <ShelfTransferModal product={target} shelves={shelves} onHide={() => setTarget(null)} onDone={() => { setTarget(null); load(false) }} />
      <ShelfContentModal shelf={shelfDetail} onHide={() => setShelfDetail(null)} onChanged={() => load(false)} />
    </div>
  )
}

/** Modal: chọn KỆ + số lượng, xem trước LÔ sẽ lấy (FIFO/HSD). */
function ShelfTransferModal({ product, shelves, onHide, onDone }) {
  const toast = useToast()
  const [qty, setQty] = useState(0)
  const [shelfId, setShelfId] = useState('')
  const [batches, setBatches] = useState([])
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    if (!product) return
    const wh = product.warehouseStock ?? 0
    setQty(Math.min(wh, Math.max(1, (product.minStock || 0) * 2 - (product.shelfStock ?? 0))))
    inventoryApi.batches(product.id).then((bs) => {
      const whb = bs.filter((b) => b.inWarehouse > 0)
      setBatches(whb)
      // Nếu lô trong kho đã gắn 1 kệ (1 lô / 1 kệ) → khóa theo kệ đó; nếu chưa → cho chọn
      const assigned = whb.find((b) => b.shelfId)?.shelfId
      setShelfId(String(assigned ?? (shelves[0]?.id ?? '')))
    }).catch(() => setBatches([]))
  }, [product, shelves])

  if (!product) return null
  const wh = product.warehouseStock ?? 0
  const lockedShelf = batches.find((b) => b.shelfId)?.shelfId // lô đã thuộc kệ này
  const chosen = shelves.find((s) => String(s.id) === String(shelfId))
  const free = chosen && chosen.freeSpace >= 0 ? chosen.freeSpace : null // null = không giới hạn
  const maxQty = free == null ? wh : Math.min(wh, free)

  // Lô đủ điều kiện lên kệ đang chọn: chưa gắn kệ, hoặc gắn đúng kệ chọn
  const eligible = batches.filter((b) => !b.shelfId || String(b.shelfId) === String(shelfId))
  const qtyNum = Number(qty) || 0
  const qtyInvalid = qtyNum < 1 || qtyNum > maxQty
  let need = qtyNum
  const plan = []
  for (const b of eligible) { if (need <= 0) break; const take = Math.min(b.inWarehouse, need); plan.push({ ...b, take }); need -= take }

  async function submit(e) {
    e.preventDefault()
    if (!shelfId) { toast.warning('Chọn kệ'); return }
    if (qtyInvalid) { toast.warning(`Nhập số lượng từ 1 đến ${maxQty}`); return }
    setLoading(true)
    try {
      const moved = await shelfApi.transfer(product.id, Number(shelfId), Number(qty))
      toast.success(`Đã đưa ${moved} sản phẩm lên kệ`)
      onDone()
    } catch (e) { toast.error(errMsg(e)) } finally { setLoading(false) }
  }

  return (
    <Modal show={!!product} onHide={onHide} centered>
      <Form onSubmit={submit}>
        <Modal.Header closeButton><Modal.Title>Lên kệ · {product.name}</Modal.Title></Modal.Header>
        <Modal.Body>
          <div className="soft-card p-3 mb-3 d-flex justify-content-around text-center">
            <div><div className="text-muted2 small">Trên kệ</div><div className="num fw-bold fs-5 text-success">{product.shelfStock ?? 0}</div></div>
            <div><div className="text-muted2 small">Trong kho</div><div className="num fw-bold fs-5 text-primary">{wh}</div></div>
          </div>

          <Form.Label>Chọn kệ</Form.Label>
          <Form.Select value={shelfId} onChange={(e) => setShelfId(e.target.value)} disabled={!!lockedShelf} className="mb-1">
            {shelves.map((s) => <option key={s.id} value={s.id}>{s.code}{s.name ? ` · ${s.name}` : ''}{s.capacity > 0 ? ` (còn trống ${s.freeSpace})` : ''}</option>)}
          </Form.Select>
          <div className="small text-muted2 mb-2">
            {lockedShelf && <>Lô đã thuộc kệ này (1 lô chỉ ở 1 kệ). </>}
            {chosen && (chosen.capacity > 0
              ? <>Kệ còn trống <b>{free}</b>/{chosen.capacity}.</>
              : <>Kệ không giới hạn sức chứa.</>)}
          </div>

          <Form.Label>Số lượng đưa lên kệ <span className="text-muted2">(gợi ý {qtyNum || '—'}, tối đa {maxQty})</span></Form.Label>
          <div className="input-group">
            <Form.Control type="number" min={1} max={maxQty} value={qty} isInvalid={qtyInvalid}
              onChange={(e) => { const v = e.target.value; setQty(v === '' ? '' : Math.min(Math.max(0, Math.floor(Number(v) || 0)), maxQty)) }}
              onBlur={() => setQty((q) => { const n = Number(q); return n >= 1 ? Math.min(n, maxQty) : 1 })} />
            <Button variant="outline-secondary" type="button" disabled={maxQty <= 0} onClick={() => setQty(maxQty)}>Tối đa</Button>
          </div>
          <Form.Text className={qtyInvalid ? 'text-danger' : 'text-muted2'}>
            {maxQty <= 0 ? 'Kệ đã đầy hoặc kho hết hàng — không thể lên kệ.'
              : qtyInvalid ? `Nhập từ 1 đến ${maxQty}.`
              : 'Có thể sửa số lượng tuỳ ý.'}
          </Form.Text>

          <div className="small text-muted2 mt-3 mb-1">Sẽ lấy từ lô (cận hạn trước):</div>
          {plan.length === 0 ? <div className="small text-muted2">—</div> : (
            <Table size="sm" className="mb-0">
              <thead><tr><th>HSD</th><th className="text-center">Kho</th><th className="text-end">Lấy lên kệ</th></tr></thead>
              <tbody>
                {plan.map((b) => (
                  <tr key={b.batchId}>
                    <td><ExpiryPill days={b.daysLeft} date={b.expiryDate} /></td>
                    <td className="text-center num text-muted2">{b.inWarehouse}</td>
                    <td className="text-end num fw-bold text-primary">+{b.take}</td>
                  </tr>
                ))}
              </tbody>
            </Table>
          )}
        </Modal.Body>
        <Modal.Footer>
          <Button variant="light" onClick={onHide}>Hủy</Button>
          <Button type="submit" disabled={loading || wh <= 0 || qtyInvalid}>{loading ? <Spinner size="sm" /> : 'Lên kệ'}</Button>
        </Modal.Footer>
      </Form>
    </Modal>
  )
}
