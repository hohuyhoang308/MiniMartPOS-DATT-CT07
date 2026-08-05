import { useCart } from '../../context/CartContext'
import { formatMoney } from '../../utils/format'

/* ---------------- Các dòng trong giỏ (cột phải) ---------------- */
export default function CartLines() {
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
