import { useEffect, useMemo, useRef, useState } from 'react'
import { Badge, Button, Card, Col, Form, InputGroup, Modal, Row, Spinner } from 'react-bootstrap'
import { shiftApi, invoiceApi } from '../../api/sales'
import { productApi, categoryApi } from '../../api/catalog'
import { customerApi, promotionApi } from '../../api/misc'
import { useCart } from '../../context/CartContext'
import { useToast } from '../../context/ToastContext'
import { errMsg } from '../../api/client'
import { formatMoney } from '../../utils/format'
import Loading from '../../components/ui/Loading'
import EmptyState from '../../components/ui/EmptyState'
import Calculator from '../../components/ui/Calculator'
import InfoBanner from '../../components/ui/InfoBanner'
import Recon from '../../components/ui/Recon'
import PaymentResultModal from './PaymentResultModal'

const QUICK_CASH = [20000, 50000, 100000, 200000, 500000]

export default function Pos() {
  const [shift, setShift] = useState(null)
  const [loadingShift, setLoadingShift] = useState(true)

  useEffect(() => {
    shiftApi.current().then(setShift).finally(() => setLoadingShift(false))
  }, [])

  if (loadingShift) return <Loading />
  if (!shift) return <OpenShiftPanel onOpened={setShift} />
  return <PosBoard shift={shift} onShiftClosed={() => setShift(null)} />
}

/* ---------------- Mở ca ---------------- */
function OpenShiftPanel({ onOpened }) {
  const toast = useToast()
  const [openingCash, setOpeningCash] = useState('')
  const [suggested, setSuggested] = useState(null)
  const [loading, setLoading] = useState(false)

  // Tự điền tiền đầu ca = tiền cuối ca trước (két chuyển tiếp) → khỏi đếm lại.
  useEffect(() => {
    shiftApi.suggestedOpening()
      .then((v) => { const n = Number(v || 0); setSuggested(n); setOpeningCash(String(n)) })
      .catch(() => setOpeningCash('0'))
  }, [])

  async function open() {
    setLoading(true)
    try { onOpened(await shiftApi.open(Number(openingCash))) }
    catch (e) { toast.error(errMsg(e)) } finally { setLoading(false) }
  }

  return (
    <Row className="justify-content-center">
      <Col md={5}>
        <Card className="border-0 mt-4 fade-up">
          <Card.Body className="p-4 text-center">
            <div className="login-logo mb-3" style={{ margin: '0 auto' }}><i className="bi bi-clock-history"></i></div>
            <h5 className="fw-bold">Mở ca làm việc</h5>
            <p className="text-muted2">Tiền đầu ca được điền sẵn theo <b>tiền cuối ca trước</b> — chỉ cần xác nhận, không phải đếm lại.</p>
            <InputGroup className="mb-2">
              <InputGroup.Text>Tiền đầu ca</InputGroup.Text>
              <Form.Control type="number" value={openingCash} onChange={(e) => setOpeningCash(e.target.value)} />
              <InputGroup.Text>đ</InputGroup.Text>
            </InputGroup>
            {suggested != null && (
              <div className="small text-muted2 mb-3">
                {suggested > 0
                  ? <>Gợi ý từ ca trước: <b>{formatMoney(suggested)}</b>{Number(openingCash) !== suggested &&
                      <Button variant="link" size="sm" className="p-0 ms-1 align-baseline" onClick={() => setOpeningCash(String(suggested))}>dùng số này</Button>}</>
                  : <>Chưa có ca trước — nhập tiền lẻ ban đầu trong két (vd 500.000đ).</>}
              </div>
            )}
            <Button className="w-100" onClick={open} disabled={loading}>
              {loading ? <Spinner size="sm" /> : <><i className="bi bi-unlock me-1"></i>Mở ca</>}
            </Button>
          </Card.Body>
        </Card>
      </Col>
    </Row>
  )
}

/* ---------------- Bàn bán hàng ---------------- */
function PosBoard({ shift, onShiftClosed }) {
  const cart = useCart()
  const toast = useToast()
  const searchRef = useRef(null)
  const [products, setProducts] = useState([])
  const [search, setSearch] = useState('')
  const [phone, setPhone] = useState('')
  const [promoCode, setPromoCode] = useState('')
  const [paymentMethod, setPaymentMethod] = useState('CASH')
  const [customerPaid, setCustomerPaid] = useState('')
  const [processing, setProcessing] = useState(false)
  const [result, setResult] = useState(null)
  const [closing, setClosing] = useState(false)
  const [categories, setCategories] = useState([])
  const [selectedCat, setSelectedCat] = useState('')
  const [showCalc, setShowCalc] = useState(false)
  const [shiftInfo, setShiftInfo] = useState(shift)

  async function loadProducts() {
    try { setProducts(await productApi.list()) } catch (e) { toast.error(errMsg(e)) }
  }
  useEffect(() => {
    loadProducts()
    categoryApi.list().then(setCategories).catch(() => {})
    searchRef.current?.focus()
  }, [])

  const filtered = useMemo(() => {
    const q = search.trim().toLowerCase()
    let active = products.filter((p) => p.status === 'ACTIVE')
    if (selectedCat) active = active.filter((p) => p.categoryId === Number(selectedCat))
    if (!q) return active
    return active.filter((p) => p.name.toLowerCase().includes(q) || p.barcode.includes(q))
  }, [products, search, selectedCat])

  function add(p) {
    if (p.currentStock <= 0) { toast.warning(`"${p.name}" đã hết hàng`); return }
    cart.addProduct(p)
  }

  async function onSearchEnter(e) {
    e.preventDefault()
    const code = search.trim()
    if (!code) return
    // Nếu khớp đúng 1 mã vạch → thêm luôn
    const exact = products.find((p) => p.barcode === code)
    if (exact) { add(exact); setSearch(''); return }
    try {
      const p = await productApi.byBarcode(code)
      add(p); setSearch('')
    } catch {
      // không phải mã vạch — giữ làm bộ lọc lưới
    }
  }

  async function attachCustomer() {
    if (!phone.trim()) { cart.setCustomer(null); return }
    try { const c = await customerApi.byPhone(phone.trim()); cart.setCustomer(c); toast.success(`Khách: ${c.fullName} · ${c.loyaltyPoints} điểm`) }
    catch (e) { toast.error(errMsg(e, 'Không tìm thấy khách')) }
  }
  async function applyPromo() {
    if (!promoCode.trim()) { cart.setPromo(null); return }
    try { const r = await promotionApi.validate(promoCode.trim(), cart.subtotal); cart.setPromo({ code: r.code, name: r.name, discountAmount: r.discountAmount }); toast.success(`Áp mã ${r.code}: -${formatMoney(r.discountAmount)}`) }
    catch (e) { cart.setPromo(null); toast.error(errMsg(e, 'Mã không hợp lệ')) }
  }

  const change = paymentMethod === 'CASH' ? Math.max(0, Number(customerPaid || 0) - cart.total) : 0
  const cashShort = paymentMethod === 'CASH' && Number(customerPaid || 0) < cart.total

  async function checkout() {
    if (cart.items.length === 0) return
    if (cashShort) { toast.warning('Tiền khách đưa chưa đủ'); return }
    setProcessing(true)
    try {
      const inv = await invoiceApi.create({
        items: cart.items.map((i) => ({ productId: i.productId, quantity: i.quantity })),
        customerId: cart.customer?.id || null,
        promotionCode: cart.promo?.code || null,
        paymentMethod,
        customerPaid: paymentMethod === 'CASH' ? Number(customerPaid) : null,
        pointsToRedeem: cart.effectiveRedeem || 0,
      })
      setResult(inv)
      cart.reset(); setPhone(''); setPromoCode(''); setCustomerPaid('')
      loadProducts()
      shiftApi.current().then((s) => { if (s) setShiftInfo(s) }).catch(() => {})
      toast.success(`Đã tạo hóa đơn ${inv.code}`)
    } catch (e) { toast.error(errMsg(e, 'Thanh toán thất bại')) }
    finally { setProcessing(false) }
  }

  return (
    <div>
      <div className="page-header">
        <div><h1 className="ph-title">Bán hàng</h1><p className="ph-sub">Quét mã vạch hoặc chọn sản phẩm để thêm vào giỏ</p></div>
        <div className="d-flex align-items-center gap-2">
          <span className="pill pill-success"><i className="bi bi-unlock-fill"></i>Ca #{shift.id}</span>
          <span className="pill pill-info"><i className="bi bi-cash-coin"></i>Doanh thu ca: {formatMoney(shiftInfo.totalSales)} · {shiftInfo.invoiceCount} HĐ</span>
          <Button size="sm" variant="light" onClick={() => setShowCalc(true)} title="Máy tính"><i className="bi bi-calculator"></i></Button>
          <Button size="sm" variant="light" onClick={() => setClosing(true)}><i className="bi bi-door-closed me-1"></i>Đóng ca</Button>
        </div>
      </div>

      <InfoBanner id="pos" title="Cách bán hàng">
        <b>Quét mã vạch</b> (gõ rồi Enter) hoặc <b>bấm vào sản phẩm</b> để thêm vào giỏ. Có thể gắn
        <b> khách thân thiết</b> để <b>tích điểm</b> (1 điểm mỗi 10.000đ) và <b>dùng điểm</b> giảm trừ
        (1 điểm = 1.000đ), kèm <b>mã giảm giá</b>. Chọn <b>Tiền mặt</b> (dùng nút mệnh giá
        nhanh để tính tiền thừa) hoặc <b>QR</b>. Cần máy tính tay? Bấm <i className="bi bi-calculator"></i> ở góc trên.
      </InfoBanner>

      <div className="pos-grid">
        {/* Trái: tìm + lưới sản phẩm */}
        <div>
          <Form onSubmit={onSearchEnter} className="mb-3">
            <InputGroup size="lg">
              <InputGroup.Text><i className="bi bi-upc-scan"></i></InputGroup.Text>
              <Form.Control ref={searchRef} placeholder="Quét mã vạch (Enter) hoặc tìm tên sản phẩm…"
                value={search} onChange={(e) => setSearch(e.target.value)} />
            </InputGroup>
          </Form>

          <div className="d-flex gap-2 flex-wrap mb-3">
            <button type="button" className={`btn btn-sm ${selectedCat === '' ? 'btn-primary' : 'btn-light'}`} onClick={() => setSelectedCat('')}>Tất cả</button>
            {categories.map((c) => (
              <button key={c.id} type="button" className={`btn btn-sm ${selectedCat === String(c.id) ? 'btn-primary' : 'btn-light'}`} onClick={() => setSelectedCat(String(c.id))}>{c.name}</button>
            ))}
          </div>

          {products.length === 0 ? <Loading /> : filtered.length === 0 ? (
            <div className="soft-card"><EmptyState icon="bi-search" title="Không tìm thấy sản phẩm" /></div>
          ) : (
            <div className="pos-products">
              {filtered.map((p) => (
                <div key={p.id} className={`product-tile ${p.currentStock <= 0 ? 'disabled' : ''}`} onClick={() => add(p)}>
                  <div className="pt-thumb">{p.imageUrl ? <img src={p.imageUrl} alt="" /> : <i className="bi bi-box"></i>}</div>
                  <div className="pt-name">{p.name}</div>
                  <div className="d-flex justify-content-between align-items-center">
                    <span className="pt-price">{formatMoney(p.salePrice)}</span>
                    <span className={`pill ${p.currentStock <= 0 ? 'pill-danger' : p.currentStock <= p.minStock ? 'pill-warning' : 'pill-muted'}`} style={{ fontSize: '.66rem' }}>
                      {p.currentStock <= 0 ? 'Hết' : p.currentStock}
                    </span>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Phải: ticket */}
        <div className="pos-cart">
          <Card className="border-0">
            <Card.Body>
              <div className="d-flex justify-content-between align-items-center mb-2">
                <h6 className="fw-bold mb-0"><i className="bi bi-basket me-2"></i>Giỏ hàng</h6>
                {cart.count > 0 && <span className="pill pill-success">{cart.count} món</span>}
              </div>

              <div className="ticket-items mb-2">
                {cart.items.length === 0 ? (
                  <div className="text-center text-muted2 py-4"><i className="bi bi-cart3 fs-3 d-block mb-1 opacity-50"></i>Giỏ hàng trống</div>
                ) : cart.items.map((i) => (
                  <div className="ticket-line" key={i.productId}>
                    <div className="flex-grow-1 min-w-0">
                      <div className="fw-semibold text-truncate" style={{ fontSize: '.88rem' }}>{i.name}</div>
                      <small className="text-muted2">{formatMoney(i.salePrice)}</small>
                    </div>
                    <div className="qty-stepper">
                      <button type="button" onClick={() => cart.setQuantity(i.productId, i.quantity - 1)}>−</button>
                      <span className="q">{i.quantity}</span>
                      <button type="button" onClick={() => cart.setQuantity(i.productId, i.quantity + 1)}>+</button>
                    </div>
                    <div className="num fw-semibold text-end" style={{ width: 78, fontSize: '.85rem' }}>{formatMoney(i.salePrice * i.quantity)}</div>
                    <button type="button" className="btn btn-sm btn-link text-danger p-0" onClick={() => cart.removeItem(i.productId)}><i className="bi bi-x-lg"></i></button>
                  </div>
                ))}
              </div>

              <InputGroup size="sm" className="mb-2">
                <InputGroup.Text><i className="bi bi-person"></i></InputGroup.Text>
                <Form.Control placeholder="SĐT khách thân thiết" value={phone} onChange={(e) => setPhone(e.target.value)} />
                <Button variant="outline-primary" onClick={attachCustomer}>Tìm</Button>
              </InputGroup>
              {cart.customer && (
                <div className="mb-2">
                  <div className="mb-1"><span className="pill pill-info"><i className="bi bi-star-fill"></i>{cart.customer.fullName} · {cart.customer.loyaltyPoints} điểm</span></div>
                  {cart.maxRedeem > 0 ? (
                    <InputGroup size="sm">
                      <InputGroup.Text><i className="bi bi-coin"></i></InputGroup.Text>
                      <Form.Control type="number" min={0} max={cart.maxRedeem} placeholder="Dùng điểm" value={cart.redeemPoints || ''}
                        onChange={(e) => cart.setRedeemPoints(Math.max(0, Math.min(Number(e.target.value) || 0, cart.maxRedeem)))} />
                      <Button variant="outline-primary" onClick={() => cart.setRedeemPoints(cart.maxRedeem)}>Tối đa</Button>
                    </InputGroup>
                  ) : (
                    <div className="small text-muted2">1 điểm = {formatMoney(cart.POINT_VALUE)} — chưa đủ điểm/đơn để đổi.</div>
                  )}
                  {cart.effectiveRedeem > 0 && <div className="small text-success mt-1"><i className="bi bi-coin me-1"></i>Đổi {cart.effectiveRedeem} điểm = -{formatMoney(cart.redeemValue)}</div>}
                </div>
              )}

              <InputGroup size="sm" className="mb-3">
                <InputGroup.Text><i className="bi bi-ticket-perforated"></i></InputGroup.Text>
                <Form.Control placeholder="Mã giảm giá" value={promoCode} onChange={(e) => setPromoCode(e.target.value)} />
                <Button variant="outline-primary" onClick={applyPromo}>Áp dụng</Button>
              </InputGroup>

              <div className="d-flex justify-content-between small"><span className="text-muted2">Tạm tính</span><span className="num">{formatMoney(cart.subtotal)}</span></div>
              <div className="d-flex justify-content-between small text-success"><span>Giảm giá</span><span className="num">-{formatMoney(cart.discount)}</span></div>
              {cart.effectiveRedeem > 0 && (
                <div className="d-flex justify-content-between small text-success"><span>Đổi {cart.effectiveRedeem} điểm</span><span className="num">-{formatMoney(cart.redeemValue)}</span></div>
              )}
              <hr className="my-2" />
              <div className="d-flex justify-content-between align-items-center mb-3">
                <span className="fw-bold">Tổng cộng</span><span className="num fw-bold fs-4 text-primary">{formatMoney(cart.total)}</span>
              </div>

              <div className="d-flex gap-2 mb-2">
                {['CASH', 'QR'].map((m) => (
                  <button key={m} type="button" className={`btn flex-grow-1 ${paymentMethod === m ? 'btn-primary' : 'btn-light'}`} onClick={() => setPaymentMethod(m)}>
                    <i className={`bi ${m === 'CASH' ? 'bi-cash-stack' : 'bi-qr-code'} me-1`}></i>{m === 'CASH' ? 'Tiền mặt' : 'QR'}
                  </button>
                ))}
              </div>

              {paymentMethod === 'CASH' && (
                <>
                  <InputGroup size="sm" className="mb-2">
                    <InputGroup.Text>Khách đưa</InputGroup.Text>
                    <Form.Control type="number" value={customerPaid} onChange={(e) => setCustomerPaid(e.target.value)} />
                    <InputGroup.Text>đ</InputGroup.Text>
                  </InputGroup>
                  <div className="d-flex gap-1 flex-wrap mb-2">
                    <button type="button" className="btn btn-sm btn-soft flex-grow-1" onClick={() => setCustomerPaid(String(cart.total))}>Đủ tiền</button>
                    {QUICK_CASH.filter((v) => v >= cart.total).slice(0, 4).map((v) => (
                      <button key={v} type="button" className="btn btn-sm btn-light" onClick={() => setCustomerPaid(String(v))}>{(v / 1000)}k</button>
                    ))}
                  </div>
                  <div className="d-flex justify-content-between small mb-2"><span>Tiền thừa</span>
                    <span className={cashShort ? 'text-danger fw-semibold' : 'text-success fw-semibold'}>{cashShort ? 'Chưa đủ' : formatMoney(change)}</span></div>
                </>
              )}

              <Button className="w-100 py-2" size="lg" onClick={checkout} disabled={processing || cart.items.length === 0}>
                {processing ? <Spinner size="sm" /> : <><i className="bi bi-check2-circle me-1"></i>Thanh toán {cart.total > 0 ? formatMoney(cart.total) : ''}</>}
              </Button>
            </Card.Body>
          </Card>
        </div>
      </div>

      <PaymentResultModal invoice={result} onClose={() => setResult(null)} />
      <CloseShiftModal show={closing} shift={shiftInfo} onHide={() => setClosing(false)} onClosed={onShiftClosed} />
      <Calculator show={showCalc} onHide={() => setShowCalc(false)} />
    </div>
  )
}

function CloseShiftModal({ show, shift, onHide, onClosed }) {
  const toast = useToast()
  const [info, setInfo] = useState(shift)
  const [cash, setCash] = useState('')
  const [loading, setLoading] = useState(false)

  // Khi mở modal: lấy số liệu ca mới nhất (tiền mặt bán, dự kiến két) để đối soát chính xác.
  useEffect(() => {
    if (!show) return
    setInfo(shift)
    setCash(String(shift.expectedCash ?? shift.openingCash ?? 0))
    shiftApi.current().then((s) => {
      if (s) { setInfo(s); setCash(String(s.expectedCash ?? s.openingCash ?? 0)) }
    }).catch(() => {})
  }, [show, shift])

  const expected = Number(info.expectedCash ?? 0)
  const diff = Number(cash || 0) - expected

  async function submit(e) {
    e.preventDefault(); setLoading(true)
    try { await shiftApi.close(info.id, Number(cash)); toast.success('Đã đóng ca'); onHide(); onClosed() }
    catch (e) { toast.error(errMsg(e)) } finally { setLoading(false) }
  }

  return (
    <Modal show={show} onHide={onHide} centered>
      <Form onSubmit={submit}>
        <Modal.Header closeButton><Modal.Title>Đóng ca #{info.id} — đối soát quỹ</Modal.Title></Modal.Header>
        <Modal.Body>
          <div className="soft-card p-3 mb-3">
            <Recon label="Tiền đầu ca" value={info.openingCash} />
            <Recon label="Tiền mặt bán trong ca" value={info.cashSales} icon="bi-plus-lg" />
            <hr className="my-2" />
            <Recon label="Tiền mặt dự kiến trong két" value={expected} strong />
            <div className="d-flex justify-content-between text-muted2 small mt-2">
              <span><i className="bi bi-qr-code me-1"></i>Tiền QR/CK (vào ngân hàng, không tính két)</span>
              <span className="num">{formatMoney(info.qrSales)}</span>
            </div>
            <div className="text-muted2 small">Doanh thu ca: <b>{formatMoney(info.totalSales)}</b> · {info.invoiceCount} HĐ</div>
          </div>

          <div className="d-flex justify-content-between align-items-end mb-1">
            <Form.Label className="mb-0">Tiền mặt đếm thực tế trong két</Form.Label>
            <Button size="sm" variant="outline-primary" onClick={() => setCash(String(expected))}>
              <i className="bi bi-magic me-1"></i>Khớp quỹ
            </Button>
          </div>
          <InputGroup>
            <Form.Control type="number" autoFocus value={cash} onChange={(e) => setCash(e.target.value)} />
            <InputGroup.Text>đ</InputGroup.Text>
          </InputGroup>
          <div className="small text-muted2 mt-1">Không cần đếm lại nếu khớp — bấm <b>Khớp quỹ</b> để dùng số dự kiến.</div>

          <div className="d-flex justify-content-between align-items-center mt-3">
            <span className="fw-semibold">Chênh lệch</span>
            {diff === 0
              ? <span className="pill pill-success"><i className="bi bi-check-circle-fill"></i>Khớp quỹ</span>
              : <span className={`fw-bold ${diff > 0 ? 'text-success' : 'text-danger'}`}>
                  {diff > 0 ? `Thừa +${formatMoney(diff)}` : `Thiếu ${formatMoney(diff)}`}
                </span>}
          </div>
        </Modal.Body>
        <Modal.Footer>
          <Button variant="light" onClick={onHide}>Hủy</Button>
          <Button type="submit" variant="danger" disabled={loading}>{loading ? <Spinner size="sm" /> : 'Đóng ca'}</Button>
        </Modal.Footer>
      </Form>
    </Modal>
  )
}
