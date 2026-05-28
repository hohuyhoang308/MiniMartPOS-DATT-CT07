import { useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Button, Card, Col, Form, Modal, Nav, Row, Spinner, Table } from 'react-bootstrap'
import {
  Bar, BarChart, CartesianGrid, Cell, ResponsiveContainer, Tooltip, XAxis, YAxis,
} from 'recharts'
import PageHeader from '../../components/ui/PageHeader'
import InfoBanner from '../../components/ui/InfoBanner'
import StatCard from '../../components/ui/StatCard'
import EmptyState from '../../components/ui/EmptyState'
import Loading from '../../components/ui/Loading'
import { inventoryApi } from '../../api/misc'
import { useToast } from '../../context/ToastContext'
import { errMsg } from '../../api/client'

const URGENCY = {
  OUT: { cls: 'pill-danger', icon: 'bi-x-octagon-fill', label: 'Hết hàng' },
  URGENT: { cls: 'pill-warning', icon: 'bi-exclamation-triangle-fill', label: 'Khẩn cấp' },
  REORDER: { cls: 'pill-info', icon: 'bi-arrow-repeat', label: 'Nên nhập' },
}

/** Ô tồn Kệ với màu: 0 = đỏ, ≤ tối thiểu = vàng, còn lại xanh. */
function ShelfCell({ s }) {
  const shelf = s.shelfStock ?? 0
  const cls = shelf <= 0 ? 'text-danger' : shelf <= s.minStock ? 'text-warning' : 'text-success'
  return <span className={`fw-semibold ${cls}`}>{shelf}</span>
}

export default function Inventory() {
  const toast = useToast()
  const navigate = useNavigate()
  const [stock, setStock] = useState([])
  const [expiring, setExpiring] = useState([])
  const [suggestions, setSuggestions] = useState([])
  const [loading, setLoading] = useState(true)
  const [tab, setTab] = useState('all')
  const [shelfTarget, setShelfTarget] = useState(null) // sản phẩm đang "lên kệ"
  const [batchTarget, setBatchTarget] = useState(null) // sản phẩm đang xem chi tiết lô

  function load(first) {
    if (first) setLoading(true)
    Promise.all([inventoryApi.stock(), inventoryApi.expiring(), inventoryApi.suggestions()])
      .then(([s, e, sg]) => { setStock(s); setExpiring(e); setSuggestions(sg) })
      .catch((e) => toast.error(errMsg(e)))
      .finally(() => setLoading(false))
  }
  useEffect(() => { load(true) }, [])

  const low = useMemo(() => stock.filter((s) => s.lowStock), [stock])
  const shelfLow = useMemo(() => stock.filter((s) => s.shelfLow), [stock]) // kệ cạn mà kho còn
  const chartData = useMemo(
    () => [...stock].sort((a, b) => (a.shelfStock ?? 0) - (b.shelfStock ?? 0)).slice(0, 8)
      .map((s) => ({ name: s.name.length > 16 ? s.name.slice(0, 16) + '…' : s.name, stock: s.shelfStock ?? 0, min: s.minStock })),
    [stock],
  )

  if (loading) return <Loading />

  const rows = tab === 'low' ? low : stock

  return (
    <div>
      <PageHeader title="Tồn kho · Kho & Kệ" subtitle="Theo dõi tồn theo lô/HSD, đưa hàng từ kho lên kệ, đề xuất nhập" />

      <InfoBanner id="inventory" title="Kho và Kệ hoạt động ra sao?">
        Hàng nhập vào <b>KHO</b>; muốn bán phải <b>đưa lên KỆ</b> (chọn lô theo <b>FIFO/HSD</b> — lô cận hạn lên trước).
        POS chỉ bán phần <b>trên kệ</b>. Khi <b>kệ cạn mà kho còn</b> → tab <b>Cần lên kệ</b> nhắc bổ sung. Còn khi
        <b> cả kho lẫn kệ</b> xuống thấp → tab <b>Đề xuất nhập</b> (kèm điểm tái đặt & EOQ) để nhập thêm.
      </InfoBanner>

      <Row className="g-3 mb-3 stagger">
        <Col md={3}><StatCard icon="bi-shop" chip="sky" label="Tổng mặt hàng" value={stock.length} /></Col>
        <Col md={3}><StatCard icon="bi-arrow-up-square-fill" chip="emerald" label="Cần lên kệ" value={shelfLow.length} /></Col>
        <Col md={3}><StatCard icon="bi-cart-plus" chip="violet" label="Cần nhập hàng" value={suggestions.length} /></Col>
        <Col md={3}><StatCard icon="bi-calendar-x-fill" chip="rose" label="Lô cận/quá HSD (30 ngày)" value={expiring.length} /></Col>
      </Row>

      <Row className="g-3">
        <Col lg={5}>
          <Card className="border-0 h-100">
            <Card.Body>
              <Card.Title className="fs-6 mb-3">8 mặt hàng tồn KỆ thấp nhất</Card.Title>
              {chartData.length === 0 ? <EmptyState title="Chưa có dữ liệu" /> : (
                <ResponsiveContainer width="100%" height={300}>
                  <BarChart data={chartData} layout="vertical" margin={{ left: 8 }}>
                    <CartesianGrid strokeDasharray="3 3" horizontal={false} />
                    <XAxis type="number" allowDecimals={false} />
                    <YAxis type="category" dataKey="name" width={110} tick={{ fontSize: 11 }} />
                    <Tooltip />
                    <Bar dataKey="stock" radius={[0, 6, 6, 0]} barSize={16}>
                      {chartData.map((d, i) => (
                        <Cell key={i} fill={d.stock <= 0 ? '#f43f5e' : d.stock <= d.min ? '#f59e0b' : '#10b981'} />
                      ))}
                    </Bar>
                  </BarChart>
                </ResponsiveContainer>
              )}
            </Card.Body>
          </Card>
        </Col>

        <Col lg={7}>
          <Card className="border-0 h-100">
            <Card.Body className="pb-0 d-flex justify-content-between align-items-start flex-wrap gap-2">
              <Nav variant="pills" activeKey={tab} onSelect={setTab} className="mb-3 gap-2">
                <Nav.Item><Nav.Link eventKey="all">Tất cả ({stock.length})</Nav.Link></Nav.Item>
                <Nav.Item><Nav.Link eventKey="shelf">Cần lên kệ ({shelfLow.length})</Nav.Link></Nav.Item>
                <Nav.Item><Nav.Link eventKey="suggest">Đề xuất nhập ({suggestions.length})</Nav.Link></Nav.Item>
                <Nav.Item><Nav.Link eventKey="expiring">Cận HSD ({expiring.length})</Nav.Link></Nav.Item>
              </Nav>
              {tab === 'suggest' && suggestions.length > 0 && (
                <Button size="sm" onClick={() => navigate('/receipts', {
                  state: { prefill: suggestions.map((s) => ({ productId: s.productId, quantity: s.suggestedQty })) },
                })}>
                  <i className="bi bi-box-arrow-in-down me-1"></i>Lập phiếu nhập ({suggestions.length})
                </Button>
              )}
            </Card.Body>
            <div style={{ maxHeight: 380, overflowY: 'auto' }}>
              {tab === 'expiring' ? (
                <Table hover className="mb-0">
                  <thead><tr><th>Sản phẩm</th><th className="text-center">Tồn lô</th><th>HSD</th><th className="text-center">Còn lại</th></tr></thead>
                  <tbody>
                    {expiring.map((b) => (
                      <tr key={b.batchId}>
                        <td className="fw-semibold">{b.productName}</td>
                        <td className="text-center num">{b.quantityRemaining}</td>
                        <td>{b.expiryDate}</td>
                        <td className="text-center">
                          <span className={`pill ${b.daysLeft < 0 ? 'pill-danger' : b.daysLeft <= 7 ? 'pill-warning' : 'pill-info'}`}>
                            {b.daysLeft < 0 ? `Quá ${-b.daysLeft} ngày` : `${b.daysLeft} ngày`}
                          </span>
                        </td>
                      </tr>
                    ))}
                    {expiring.length === 0 && <tr><td colSpan={4}><EmptyState icon="bi-calendar-check" title="Không có lô cận hạn" /></td></tr>}
                  </tbody>
                </Table>
              ) : tab === 'shelf' ? (
                <Table hover className="mb-0 align-middle">
                  <thead><tr><th>Sản phẩm</th><th className="text-center">Kệ</th><th className="text-center">Kho</th><th className="text-end">Thao tác</th></tr></thead>
                  <tbody>
                    {shelfLow.map((s) => (
                      <tr key={s.productId}>
                        <td className="fw-semibold">{s.name}<div className="text-muted2 small">Tối thiểu kệ: {s.minStock}</div></td>
                        <td className="text-center num"><ShelfCell s={s} /></td>
                        <td className="text-center num">{s.warehouseStock ?? 0}</td>
                        <td className="text-end">
                          <Button size="sm" onClick={() => setShelfTarget(s)}><i className="bi bi-arrow-up me-1"></i>Lên kệ</Button>
                        </td>
                      </tr>
                    ))}
                    {shelfLow.length === 0 && <tr><td colSpan={4}><EmptyState icon="bi-check2-circle" title="Kệ đầy đủ — không cần lên hàng" /></td></tr>}
                  </tbody>
                </Table>
              ) : tab === 'suggest' ? (
                <Table hover className="mb-0 align-middle">
                  <thead><tr>
                    <th>Sản phẩm</th>
                    <th className="text-center">Tồn / Min</th>
                    <th className="text-center">Bán 30 ngày</th>
                    <th className="text-center">Tái đặt</th>
                    <th className="text-center">EOQ</th>
                    <th className="text-center">Đề xuất</th>
                    <th className="text-center">Độ khẩn</th>
                  </tr></thead>
                  <tbody>
                    {suggestions.map((s) => {
                      const u = URGENCY[s.urgency] || URGENCY.REORDER
                      return (
                        <tr key={s.productId}>
                          <td className="fw-semibold">{s.name}<div className="text-muted2 small">{s.soldLast30} bán · {s.avgDailySold}/ngày</div></td>
                          <td className="text-center num">{s.currentStock} / {s.minStock}</td>
                          <td className="text-center num">{s.daysUntilStockout != null ? `${s.daysUntilStockout} ngày` : '—'}</td>
                          <td className="text-center num" title="Điểm tái đặt hàng">{s.reorderPoint}</td>
                          <td className="text-center num text-primary fw-semibold" title="Lượng đặt kinh tế (EOQ)">{s.eoq}</td>
                          <td className="text-center num fw-bold text-success">+{s.suggestedQty}</td>
                          <td className="text-center"><span className={`pill ${u.cls}`}><i className={`bi ${u.icon}`}></i>{u.label}</span></td>
                        </tr>
                      )
                    })}
                    {suggestions.length === 0 && <tr><td colSpan={7}><EmptyState icon="bi-check2-circle" title="Tồn kho ổn — chưa cần nhập thêm" /></td></tr>}
                  </tbody>
                </Table>
              ) : (
                <Table hover className="mb-0 align-middle">
                  <thead><tr>
                    <th>Sản phẩm</th><th className="text-center">Kệ</th><th className="text-center">Kho</th>
                    <th className="text-center">Tổng / Min</th><th className="text-center">Trạng thái</th><th className="text-end"></th>
                  </tr></thead>
                  <tbody>
                    {rows.map((s) => (
                      <tr key={s.productId}>
                        <td className="fw-semibold cursor-pointer" onClick={() => setBatchTarget(s)} title="Xem các lô & HSD">
                          {s.name} <i className="bi bi-card-list text-muted2"></i>
                          <div className="text-muted2 small">{s.barcode}</div>
                        </td>
                        <td className="text-center num"><ShelfCell s={s} /></td>
                        <td className="text-center num">{s.warehouseStock ?? 0}</td>
                        <td className="text-center num">{s.currentStock} / {s.minStock}</td>
                        <td className="text-center">
                          {s.currentStock <= 0 ? <span className="pill pill-danger"><i className="bi bi-x-octagon-fill"></i>Hết hàng</span>
                            : s.shelfLow ? <span className="pill pill-warning"><i className="bi bi-arrow-up"></i>Cần lên kệ</span>
                            : s.lowStock ? <span className="pill pill-warning"><i className="bi bi-exclamation-triangle-fill"></i>Tồn thấp</span>
                            : <span className="pill pill-success"><i className="bi bi-check-circle-fill"></i>Đủ hàng</span>}
                        </td>
                        <td className="text-end">
                          {(s.warehouseStock ?? 0) > 0 && (
                            <Button size="sm" variant="soft" onClick={() => setShelfTarget(s)}><i className="bi bi-arrow-up me-1"></i>Lên kệ</Button>
                          )}
                        </td>
                      </tr>
                    ))}
                    {rows.length === 0 && <tr><td colSpan={6}><EmptyState icon="bi-clipboard-check" title="Không có mặt hàng" /></td></tr>}
                  </tbody>
                </Table>
              )}
            </div>
          </Card>
        </Col>
      </Row>

      <ShelfTransferModal product={shelfTarget} onHide={() => setShelfTarget(null)}
        onDone={() => { setShelfTarget(null); load(false) }} />
      <BatchDetailModal product={batchTarget} onHide={() => setBatchTarget(null)} />
    </div>
  )
}

/** Pill HSD theo số ngày còn lại. */
function ExpiryPill({ days, date }) {
  if (date == null) return <span className="text-muted2">Không HSD</span>
  const cls = days < 0 ? 'pill-danger' : days <= 7 ? 'pill-warning' : days <= 30 ? 'pill-info' : 'pill-muted'
  return <span className={`pill ${cls}`}>{date} · {days < 0 ? `quá ${-days}n` : `${days}n`}</span>
}

/** Modal xem CHI TIẾT CÁC LÔ của 1 sản phẩm: lô nào, HSD, tồn kho/kệ. */
function BatchDetailModal({ product, onHide }) {
  const toast = useToast()
  const [batches, setBatches] = useState([])
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    if (!product) return
    setLoading(true)
    inventoryApi.batches(product.productId)
      .then(setBatches).catch((e) => toast.error(errMsg(e))).finally(() => setLoading(false))
  }, [product])

  if (!product) return null

  return (
    <Modal show={!!product} onHide={onHide} centered>
      <Modal.Header closeButton><Modal.Title>Lô hàng · {product.name}</Modal.Title></Modal.Header>
      <Modal.Body>
        <div className="small text-muted2 mb-2">Bán theo <b>FIFO</b> — lô cận hạn (trên cùng) xuất trước.</div>
        {loading ? <Loading /> : (
          <Table size="sm" hover className="mb-0">
            <thead><tr><th>HSD</th><th className="text-center">Nhập</th><th className="text-center">Kho</th><th className="text-center">Kệ</th><th className="text-center">Còn</th></tr></thead>
            <tbody>
              {batches.map((b) => (
                <tr key={b.batchId}>
                  <td><ExpiryPill days={b.daysLeft} date={b.expiryDate} /></td>
                  <td className="text-center num text-muted2">{b.quantityIn}</td>
                  <td className="text-center num">{b.inWarehouse}</td>
                  <td className="text-center num text-success fw-semibold">{b.onShelf}</td>
                  <td className="text-center num fw-semibold">{b.quantityRemaining}</td>
                </tr>
              ))}
              {batches.length === 0 && <tr><td colSpan={5}><EmptyState icon="bi-box" title="Sản phẩm không còn lô tồn" /></td></tr>}
            </tbody>
          </Table>
        )}
      </Modal.Body>
    </Modal>
  )
}

/** Modal đưa hàng từ KHO lên KỆ — hiển thị các lô trong kho + XEM TRƯỚC lô nào sẽ lấy (FIFO/HSD). */
function ShelfTransferModal({ product, onHide, onDone }) {
  const toast = useToast()
  const [qty, setQty] = useState(0)
  const [batches, setBatches] = useState([])
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    if (!product) return
    const wh = product.warehouseStock ?? 0
    const fill = Math.max(1, (product.minStock || 0) * 2 - (product.shelfStock ?? 0))
    setQty(Math.min(wh, fill))
    inventoryApi.batches(product.productId)
      .then((bs) => setBatches(bs.filter((b) => b.inWarehouse > 0)))
      .catch(() => setBatches([]))
  }, [product])

  if (!product) return null
  const wh = product.warehouseStock ?? 0

  // Xem trước phân bổ FIFO: lấy từ lô cận hạn trước cho tới đủ qty.
  let need = Number(qty) || 0
  const plan = []
  for (const b of batches) {
    if (need <= 0) break
    const take = Math.min(b.inWarehouse, need)
    plan.push({ ...b, take }); need -= take
  }

  async function submit(e) {
    e.preventDefault(); setLoading(true)
    try {
      const moved = await inventoryApi.shelfTransfer(product.productId, Number(qty))
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
            <div><div className="text-muted2 small">Đang trên kệ</div><div className="num fw-bold fs-5 text-success">{product.shelfStock ?? 0}</div></div>
            <div><div className="text-muted2 small">Trong kho</div><div className="num fw-bold fs-5 text-primary">{wh}</div></div>
          </div>
          <Form.Label>Số lượng đưa lên kệ (tối đa {wh})</Form.Label>
          <Form.Control type="number" min={1} max={wh} value={qty} autoFocus
            onChange={(e) => setQty(Math.max(1, Math.min(Number(e.target.value) || 0, wh)))} />

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
          <Button type="submit" disabled={loading || wh <= 0}>{loading ? <Spinner size="sm" /> : 'Lên kệ'}</Button>
        </Modal.Footer>
      </Form>
    </Modal>
  )
}
