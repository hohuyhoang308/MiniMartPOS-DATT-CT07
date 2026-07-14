import { useEffect, useMemo, useRef, useState } from 'react'
import { Button, Card, Col, Form, InputGroup, Modal, Row, Spinner } from 'react-bootstrap'
import { shiftApi, invoiceApi, cashMovementApi } from '../../api/sales'
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
import MoneyInput from '../../components/ui/MoneyInput'
import CloseShiftModal from '../../components/CloseShiftModal'
import PaymentResultModal from './PaymentResultModal'

const QUICK_CASH = [20000, 50000, 100000, 200000, 500000]

/** Sinh khóa chống trùng (UUID). Dùng crypto.randomUUID nếu có (HTTPS/localhost), không thì fallback. */
function genIdemKey() {
  if (typeof crypto !== 'undefined' && crypto.randomUUID) return crypto.randomUUID()
  return 'idem-' + Date.now().toString(36) + '-' + Math.random().toString(36).slice(2, 10)
}

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
            <p className="text-muted2">Tiền đầu ca đã được điền sẵn theo <b>tiền còn lại của ca trước</b>. Bạn chỉ cần xác nhận, không phải đếm lại tiền trong két.</p>
            <InputGroup className="mb-2">
              <InputGroup.Text>Tiền đầu ca</InputGroup.Text>
              <MoneyInput value={openingCash} onChange={setOpeningCash} />
              <InputGroup.Text>đ</InputGroup.Text>
            </InputGroup>
            {suggested != null && (
              <div className="small text-muted2 mb-3">
                {suggested > 0
                  ? <>Gợi ý từ ca trước: <b>{formatMoney(suggested)}</b>{Number(openingCash) !== suggested &&
                      <Button variant="link" size="sm" className="p-0 ms-1 align-baseline" onClick={() => setOpeningCash(String(suggested))}>dùng số này</Button>}</>
                  : <>Chưa có ca trước. Bạn hãy nhập số tiền lẻ đang có sẵn trong két (ví dụ 500.000đ).</>}
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
  const idemRef = useRef(null) // khóa chống tạo HĐ trùng (idempotency) cho lần thanh toán hiện tại
  const [result, setResult] = useState(null)
  const [closing, setClosing] = useState(false)
  const [categories, setCategories] = useState([])
  const [selectedCat, setSelectedCat] = useState('')
  const [showCalc, setShowCalc] = useState(false)
  const [shiftInfo, setShiftInfo] = useState(shift)
  const [related, setRelated] = useState([]) // gợi ý "mua kèm"
  const [showHeld, setShowHeld] = useState(false) // modal danh sách đơn treo
  const [showCash, setShowCash] = useState(false) // modal thu/chi quỹ

  async function loadProducts() {
    try { setProducts(await productApi.list()) } catch (e) { toast.error(errMsg(e)) }
  }
  // Đồng bộ lại thông tin ca hiện tại (doanh thu/số HĐ) sau khi bán, thu/chi quỹ, hoặc thu tiền QR.
  function refreshShift() {
    shiftApi.current().then((s) => { if (s) setShiftInfo(s) }).catch(() => {})
  }
  useEffect(() => {
    loadProducts()
    categoryApi.list().then(setCategories).catch(() => {})
    searchRef.current?.focus()
  }, [])
  // Giỏ trống → xóa gợi ý mua kèm
  useEffect(() => { if (cart.items.length === 0) setRelated([]) }, [cart.items.length])

  const filtered = useMemo(() => {
    const q = search.trim().toLowerCase()
    let active = products.filter((p) => p.status === 'ACTIVE')
    if (selectedCat) active = active.filter((p) => p.categoryId === Number(selectedCat))
    if (!q) return active
    return active.filter((p) => p.name.toLowerCase().includes(q) || p.barcode.includes(q))
  }, [products, search, selectedCat])

  function add(p) {
    if ((p.shelfStock ?? 0) <= 0) { toast.warning(`"${p.name}" đã hết hàng trên kệ. Cần lấy thêm hàng từ kho ra kệ.`); return }
    cart.addProduct({ ...p, currentStock: p.shelfStock }) // POS chỉ bán phần trên kệ
    refreshRelated(p.id)
    searchRef.current?.focus() // giữ con trỏ ở ô quét để quét liên tục (kể cả khi bấm chuột vào sản phẩm)
  }
  async function refreshRelated(productId) {
    try { setRelated(await productApi.related(productId)) } catch { /* bỏ qua gợi ý */ }
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
    catch (e) { toast.error(errMsg(e, 'Không tìm thấy khách với số điện thoại này')) }
  }
  async function applyPromo() {
    if (!promoCode.trim()) { cart.setPromo(null); return }
    try { const r = await promotionApi.validate(promoCode.trim(), cart.subtotal); cart.setPromo({ code: r.code, name: r.name, discountAmount: r.discountAmount }); toast.success(`Đã áp mã ${r.code}: giảm ${formatMoney(r.discountAmount)}`) }
    catch (e) { cart.setPromo(null); toast.error(errMsg(e, 'Mã giảm giá không dùng được')) }
  }

  const change = paymentMethod === 'CASH' ? Math.max(0, Number(customerPaid || 0) - cart.total) : 0
  const cashShort = paymentMethod === 'CASH' && Number(customerPaid || 0) < cart.total
  // Gợi ý mua kèm: bỏ món đã có trong giỏ, chỉ còn hàng
  const relatedShow = related.filter((r) => (r.shelfStock ?? 0) > 0 && !cart.items.some((i) => i.productId === r.id)).slice(0, 4)

  async function checkout() {
    if (cart.items.length === 0) return
    if (cashShort) { toast.warning('Tiền khách đưa chưa đủ để thanh toán'); return }
    // Khóa chống trùng: dùng LẠI cùng key khi thử lại đơn này (mất mạng) → server không tạo HĐ thứ 2.
    if (!idemRef.current) idemRef.current = genIdemKey()
    setProcessing(true)
    try {
      const inv = await invoiceApi.create({
        items: cart.items.map((i) => ({ productId: i.productId, quantity: i.quantity })),
        customerId: cart.customer?.id || null,
        promotionCode: cart.promo?.code || null,
        paymentMethod,
        customerPaid: paymentMethod === 'CASH' ? Number(customerPaid) : null,
        pointsToRedeem: cart.effectiveRedeem || 0,
        idempotencyKey: idemRef.current,
      })
      idemRef.current = null // thành công → đơn sau dùng key mới
      setResult(inv)
      cart.reset(); setPhone(''); setPromoCode(''); setCustomerPaid('')
      loadProducts()
      refreshShift()
      toast.success(`Đã tạo hóa đơn ${inv.code}`)
    } catch (e) { toast.error(errMsg(e, 'Thanh toán không thành công, vui lòng thử lại')) } // giữ idemRef để thử lại an toàn
    finally { setProcessing(false) }
  }

  // Treo đơn hiện tại (phục vụ khách khác rồi lấy lại). Nhãn tự đặt = tên khách hoặc món đầu tiên.
  function hold() {
    if (cart.items.length === 0) return
    const label = cart.customer?.fullName
      || (cart.items[0]?.name + (cart.items.length > 1 ? ` +${cart.items.length - 1} món` : ''))
    if (cart.holdCurrent(label)) {
      idemRef.current = null
      setPhone(''); setPromoCode(''); setCustomerPaid('')
      toast.success('Đã treo đơn — có thể phục vụ khách tiếp theo')
      searchRef.current?.focus()
    }
  }

  // Lấy lại đơn treo. Yêu cầu giỏ hiện tại trống để không lẫn đơn (an toàn, rõ ràng).
  function resume(id) {
    if (cart.items.length > 0) {
      toast.warning('Hãy thanh toán hoặc treo đơn hiện tại trước khi lấy đơn treo khác')
      return
    }
    if (cart.resumeHeld(id)) {
      setShowHeld(false)
      setPhone(''); setPromoCode(''); setCustomerPaid('')
      toast.success('Đã lấy lại đơn treo')
      searchRef.current?.focus()
    }
  }

  return (
    <div>
      <div className="page-header">
        <div><h1 className="ph-title">Bán hàng</h1><p className="ph-sub">Quét mã vạch hoặc bấm vào sản phẩm để thêm vào giỏ.</p></div>
        <div className="d-flex align-items-center gap-2">
          <span className="pill pill-success"><i className="bi bi-unlock-fill"></i>Ca #{shift.id}</span>
          <span className="pill pill-info"><i className="bi bi-cash-coin"></i>Doanh thu ca: {formatMoney(shiftInfo.totalSales)} · {shiftInfo.invoiceCount} hóa đơn</span>
          <Button size="sm" variant={cart.heldOrders.length > 0 ? 'warning' : 'light'} onClick={() => setShowHeld(true)} title="Đơn đang treo">
            <i className="bi bi-pause-circle me-1"></i>Đơn treo{cart.heldOrders.length > 0 ? ` (${cart.heldOrders.length})` : ''}
          </Button>
          <Button size="sm" variant="light" onClick={() => setShowCash(true)} title="Thu/Chi tiền mặt quỹ ca">
            <i className="bi bi-cash-coin me-1"></i>Thu/Chi quỹ
          </Button>
          <Button size="sm" variant="light" onClick={() => setShowCalc(true)} title="Máy tính"><i className="bi bi-calculator"></i></Button>
          <Button size="sm" variant="light" onClick={() => setClosing(true)}><i className="bi bi-door-closed me-1"></i>Đóng ca</Button>
        </div>
      </div>

      <InfoBanner id="pos" title="Cách bán hàng">
        <b>Quét mã vạch</b> hoặc <b>bấm vào sản phẩm</b> để thêm vào giỏ. Bạn có thể <b>nhập số điện thoại khách</b> để
        tích điểm và dùng điểm, và <b>nhập mã giảm giá</b> nếu có. Cuối cùng chọn trả <b>tiền mặt</b> hoặc <b>chuyển khoản (QR)</b>.
      </InfoBanner>

      <div className="pos-grid">
        {/* Trái: tìm + lưới sản phẩm */}
        <div>
          <Form onSubmit={onSearchEnter} className="mb-3">
            <InputGroup size="lg">
              <InputGroup.Text><i className="bi bi-upc-scan"></i></InputGroup.Text>
              <Form.Control ref={searchRef} placeholder="Quét mã vạch hoặc gõ tên sản phẩm để tìm…"
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
            <ProductGrid products={filtered} onAdd={add} />
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

              <div className="ticket-items mb-2"><CartLines /></div>

              {relatedShow.length > 0 && (
                <div className="mb-2">
                  <div className="small text-muted2 mb-1"><i className="bi bi-magic me-1"></i>Khách hay mua kèm</div>
                  <div className="d-flex flex-wrap gap-1">
                    {relatedShow.map((r) => (
                      <button key={r.id} type="button" className="btn btn-sm btn-soft d-flex align-items-center gap-1"
                        style={{ fontSize: '.78rem' }} onClick={() => add(r)} title={`Thêm ${r.name}`}>
                        <i className="bi bi-plus-circle"></i>
                        <span className="text-truncate" style={{ maxWidth: 120 }}>{r.name}</span>
                        <span className="text-muted2">{formatMoney(r.storeSalePrice ?? r.salePrice)}</span>
                      </button>
                    ))}
                  </div>
                </div>
              )}

              <InputGroup size="sm" className="mb-2">
                <InputGroup.Text><i className="bi bi-person"></i></InputGroup.Text>
                <Form.Control placeholder="Số điện thoại khách thân thiết" value={phone} onChange={(e) => setPhone(e.target.value)} />
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
                    <div className="small text-muted2">1 điểm = {formatMoney(cart.POINT_VALUE)}. Khách chưa đủ điểm hoặc đơn chưa đủ điều kiện để đổi.</div>
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
                    <i className={`bi ${m === 'CASH' ? 'bi-cash-stack' : 'bi-qr-code'} me-1`}></i>{m === 'CASH' ? 'Tiền mặt' : 'Chuyển khoản (QR)'}
                  </button>
                ))}
              </div>

              {paymentMethod === 'CASH' && (
                <>
                  <InputGroup size="sm" className="mb-2">
                    <InputGroup.Text>Khách đưa</InputGroup.Text>
                    <MoneyInput value={customerPaid} onChange={setCustomerPaid} />
                    <InputGroup.Text>đ</InputGroup.Text>
                  </InputGroup>
                  <div className="d-flex gap-1 flex-wrap mb-2">
                    <button type="button" className="btn btn-sm btn-soft flex-grow-1" onClick={() => setCustomerPaid(String(cart.total))}>Đủ tiền</button>
                    {QUICK_CASH.filter((v) => v >= cart.total).slice(0, 4).map((v) => (
                      <button key={v} type="button" className="btn btn-sm btn-light" onClick={() => setCustomerPaid(String(v))}>{(v / 1000)}k</button>
                    ))}
                  </div>
                  <div className="d-flex justify-content-between small mb-2"><span>Tiền thừa trả khách</span>
                    <span className={cashShort ? 'text-danger fw-semibold' : 'text-success fw-semibold'}>{cashShort ? 'Khách đưa chưa đủ' : formatMoney(change)}</span></div>
                </>
              )}

              <div className="d-flex gap-2">
                <Button variant="outline-warning" className="py-2" size="lg" onClick={hold}
                  disabled={processing || cart.items.length === 0} title="Treo đơn để phục vụ khách khác">
                  <i className="bi bi-pause-circle"></i>
                </Button>
                <Button className="flex-grow-1 py-2" size="lg" onClick={checkout} disabled={processing || cart.items.length === 0}>
                  {processing ? <Spinner size="sm" /> : <><i className="bi bi-check2-circle me-1"></i>Thanh toán {cart.total > 0 ? formatMoney(cart.total) : ''}</>}
                </Button>
              </div>
            </Card.Body>
          </Card>
        </div>
      </div>

      <HeldOrdersModal show={showHeld} onHide={() => setShowHeld(false)}
        held={cart.heldOrders} onResume={resume} onRemove={cart.removeHeld} blocked={cart.items.length > 0} />

      <CashMovementModal show={showCash} onHide={() => setShowCash(false)}
        onRecorded={refreshShift} />

      <PaymentResultModal invoice={result} onClose={() => setResult(null)}
        onPaid={() => { loadProducts(); refreshShift() }} />
      <CloseShiftModal shift={closing ? shiftInfo : null} refetchCurrent
        onHide={() => setClosing(false)} onClosed={onShiftClosed} />
      <Calculator show={showCalc} onHide={() => setShowCalc(false)} />
    </div>
  )
}

/* ---------------- Lưới sản phẩm (cột trái) ---------------- */
function ProductGrid({ products, onAdd }) {
  return (
    <div className="pos-products">
      {products.map((p) => {
        const shelf = p.shelfStock ?? 0
        const out = shelf <= 0
        const low = !out && shelf <= (p.minStock ?? 0)
        return (
          <div key={p.id} className={`product-tile ${out ? 'disabled' : ''}`} onClick={() => onAdd(p)}
            title={`Vị trí kệ ${p.shelfCode ?? '—'} · Còn ${shelf} trên kệ (bán được ngay) · Còn ${p.warehouseStock ?? 0} trong kho`}>
            <div className="pt-thumb">
              {p.imageUrl ? <img src={p.imageUrl} alt="" /> : <i className="bi bi-box-seam"></i>}
              {p.shelfCode && !out && <span className="pt-shelf"><i className="bi bi-geo-alt-fill"></i>{p.shelfCode}</span>}
              <span className={`pt-stock ${out ? 'out' : low ? 'low' : ''}`}>
                {out ? 'Hết kệ' : <><span className="dot"></span>{shelf}</>}
              </span>
            </div>
            <div className="pt-name">{p.name}</div>
            <div className="pt-foot">
              <span className="pt-price">{formatMoney(p.storeSalePrice ?? p.salePrice)}</span>
              <span className="pt-add" aria-hidden="true"><i className="bi bi-plus-lg"></i></span>
            </div>
          </div>
        )
      })}
    </div>
  )
}

/* ---------------- Các dòng trong giỏ (cột phải) ---------------- */
function CartLines() {
  const cart = useCart()
  if (cart.items.length === 0) {
    return <div className="text-center text-muted2 py-4"><i className="bi bi-cart3 fs-3 d-block mb-1 opacity-50"></i>Giỏ hàng trống</div>
  }
  return cart.items.map((i) => (
    <div className="ticket-line" key={i.productId}>
      <div className="flex-grow-1 min-w-0">
        <div className="fw-semibold text-truncate" style={{ fontSize: '.88rem' }}>{i.name}</div>
        <small className="text-muted2">{formatMoney(i.salePrice)}</small>
      </div>
      <div className="qty-stepper">
        <button type="button" onClick={() => cart.setQuantity(i.productId, i.quantity - 1)}>−</button>
        <input className="q" type="number" min={1} value={i.quantity}
          style={{ width: 44, textAlign: 'center', border: 'none', background: 'transparent', fontWeight: 600 }}
          onChange={(e) => { const v = parseInt(e.target.value, 10); if (!Number.isNaN(v)) cart.setQuantity(i.productId, Math.max(1, v)) }} />
        <button type="button" onClick={() => cart.setQuantity(i.productId, i.quantity + 1)}>+</button>
      </div>
      <div className="num fw-semibold text-end" style={{ width: 78, fontSize: '.85rem' }}>{formatMoney(i.salePrice * i.quantity)}</div>
      <button type="button" className="btn btn-sm btn-link text-danger p-0" onClick={() => cart.removeItem(i.productId)}><i className="bi bi-x-lg"></i></button>
    </div>
  ))
}

/* ---------------- Thu/Chi quỹ tiền mặt ---------------- */
function CashMovementModal({ show, onHide, onRecorded }) {
  const toast = useToast()
  const [type, setType] = useState('OUT')
  const [amount, setAmount] = useState('')
  const [reason, setReason] = useState('')
  const [loading, setLoading] = useState(false)

  function close() { setAmount(''); setReason(''); setType('OUT'); onHide() }

  async function submit(e) {
    e.preventDefault()
    if (Number(amount || 0) <= 0) { toast.warning('Nhập số tiền lớn hơn 0'); return }
    if (!reason.trim()) { toast.warning('Nhập lý do thu/chi'); return }
    setLoading(true)
    try {
      await cashMovementApi.create({ type, amount: Number(amount), reason: reason.trim() })
      toast.success(type === 'IN' ? 'Đã ghi khoản THU vào quỹ' : 'Đã ghi khoản CHI khỏi quỹ')
      onRecorded?.()
      close()
    } catch (e) { toast.error(errMsg(e, 'Không ghi được thu/chi quỹ')) } finally { setLoading(false) }
  }

  return (
    <Modal show={show} onHide={close} centered>
      <Form onSubmit={submit}>
        <Modal.Header closeButton>
          <Modal.Title><i className="bi bi-cash-coin me-2"></i>Thu / Chi quỹ tiền mặt</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <div className="small text-muted2 mb-3">
            Ghi các khoản tiền mặt <b>ngoài bán hàng</b> (trả tiền nhà cung cấp, đổi tiền lẻ, rút bớt tiền...).
            Số này sẽ được tính vào <b>đối soát quỹ cuối ca</b>.
          </div>
          <div className="d-flex gap-2 mb-3">
            <button type="button" className={`btn flex-grow-1 ${type === 'OUT' ? 'btn-danger' : 'btn-light'}`} onClick={() => setType('OUT')}>
              <i className="bi bi-box-arrow-up me-1"></i>Chi ra (−)
            </button>
            <button type="button" className={`btn flex-grow-1 ${type === 'IN' ? 'btn-success' : 'btn-light'}`} onClick={() => setType('IN')}>
              <i className="bi bi-box-arrow-in-down me-1"></i>Thu vào (+)
            </button>
          </div>
          <Form.Label>Số tiền</Form.Label>
          <InputGroup className="mb-3">
            <MoneyInput autoFocus value={amount} onChange={setAmount} />
            <InputGroup.Text>đ</InputGroup.Text>
          </InputGroup>
          <Form.Label>Lý do</Form.Label>
          <Form.Control as="textarea" rows={2} value={reason} onChange={(e) => setReason(e.target.value)}
            placeholder="Vd: Trả tiền thùng nước ngọt cho NCC / Đổi tiền lẻ vào quỹ..." maxLength={255} />
        </Modal.Body>
        <Modal.Footer>
          <Button variant="light" onClick={close}>Hủy</Button>
          <Button type="submit" variant={type === 'IN' ? 'success' : 'danger'} disabled={loading}>
            {loading ? <Spinner size="sm" /> : (type === 'IN' ? 'Ghi khoản thu' : 'Ghi khoản chi')}
          </Button>
        </Modal.Footer>
      </Form>
    </Modal>
  )
}

/* ---------------- Đơn đang treo ---------------- */
function HeldOrdersModal({ show, onHide, held, onResume, onRemove, blocked }) {
  return (
    <Modal show={show} onHide={onHide} centered>
      <Modal.Header closeButton>
        <Modal.Title><i className="bi bi-pause-circle me-2"></i>Đơn đang treo</Modal.Title>
      </Modal.Header>
      <Modal.Body>
        {blocked && (
          <div className="alert alert-warning py-2 small mb-3">
            <i className="bi bi-exclamation-triangle me-1"></i>
            Giỏ hiện tại đang có hàng. Hãy thanh toán hoặc treo đơn hiện tại trước khi lấy lại đơn treo.
          </div>
        )}
        {held.length === 0 ? (
          <div className="text-center text-muted2 py-4">
            <i className="bi bi-inbox fs-3 d-block mb-1 opacity-50"></i>Chưa có đơn nào đang treo
          </div>
        ) : (
          <div className="d-flex flex-column gap-2">
            {held.map((h) => (
              <div key={h.id} className="soft-card d-flex align-items-center justify-content-between p-2">
                <div className="min-w-0">
                  <div className="fw-semibold text-truncate">{h.label}</div>
                  <small className="text-muted2">
                    {h.count} món · {formatMoney(h.subtotal)} ·{' '}
                    {new Date(h.savedAt).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })}
                  </small>
                </div>
                <div className="d-flex gap-1 flex-shrink-0">
                  <Button size="sm" variant="primary" onClick={() => onResume(h.id)} disabled={blocked}>
                    <i className="bi bi-arrow-up-circle me-1"></i>Lấy lại
                  </Button>
                  <Button size="sm" variant="outline-danger" onClick={() => onRemove(h.id)} title="Xóa đơn treo">
                    <i className="bi bi-trash"></i>
                  </Button>
                </div>
              </div>
            ))}
          </div>
        )}
      </Modal.Body>
    </Modal>
  )
}
