import { createContext, useContext, useMemo, useState } from 'react'

const CartContext = createContext(null)

/** Giá trị quy đổi: 1 điểm tích lũy = 1.000đ khi dùng để giảm trừ. */
export const POINT_VALUE = 1000

/** Giỏ hàng POS: danh sách dòng + khách + mã giảm giá + đổi điểm. Giữ trong bộ nhớ phiên bán. */
export function CartProvider({ children }) {
  const [items, setItems] = useState([]) // { productId, barcode, name, salePrice, quantity, currentStock }
  const [customer, setCustomerState] = useState(null)
  const [promo, setPromo] = useState(null) // { code, discountAmount, name }
  const [redeemPoints, setRedeemPoints] = useState(0) // số điểm khách muốn dùng

  // Đổi khách → bỏ điểm đang đặt (điểm thuộc về khách cụ thể).
  function setCustomer(c) {
    setCustomerState(c)
    setRedeemPoints(0)
  }

  function addProduct(p) {
    setItems((prev) => {
      const existing = prev.find((i) => i.productId === p.id)
      if (existing) {
        if (existing.quantity + 1 > p.currentStock) return prev // không vượt tồn
        return prev.map((i) =>
          i.productId === p.id ? { ...i, quantity: i.quantity + 1 } : i,
        )
      }
      if (p.currentStock <= 0) return prev
      return [
        ...prev,
        {
          productId: p.id,
          barcode: p.barcode,
          name: p.name,
          salePrice: p.salePrice,
          quantity: 1,
          currentStock: p.currentStock,
        },
      ]
    })
    setPromo(null) // đổi giỏ → bỏ mã cũ, áp lại
  }

  function setQuantity(productId, qty) {
    setItems((prev) =>
      prev
        .map((i) =>
          i.productId === productId
            ? { ...i, quantity: Math.max(1, Math.min(qty, i.currentStock)) }
            : i,
        )
        .filter(Boolean),
    )
    setPromo(null)
  }

  function removeItem(productId) {
    setItems((prev) => prev.filter((i) => i.productId !== productId))
    setPromo(null)
  }

  function reset() {
    setItems([])
    setCustomerState(null)
    setPromo(null)
    setRedeemPoints(0)
  }

  const subtotal = useMemo(
    () => items.reduce((s, i) => s + i.salePrice * i.quantity, 0),
    [items],
  )
  const discount = promo?.discountAmount || 0

  // Đổi điểm: tối đa = min(số dư điểm của khách, số tiền còn lại / giá trị điểm).
  const afterPromo = Math.max(0, subtotal - discount)
  const maxRedeem = customer
    ? Math.min(customer.loyaltyPoints || 0, Math.floor(afterPromo / POINT_VALUE))
    : 0
  const effectiveRedeem = Math.max(0, Math.min(redeemPoints, maxRedeem))
  const redeemValue = effectiveRedeem * POINT_VALUE
  const total = Math.max(0, afterPromo - redeemValue)

  const value = {
    items, customer, promo,
    addProduct, setQuantity, removeItem, reset,
    setCustomer, setPromo,
    redeemPoints, setRedeemPoints, effectiveRedeem, redeemValue, maxRedeem, POINT_VALUE,
    subtotal, discount, total,
    count: items.reduce((s, i) => s + i.quantity, 0),
  }
  return <CartContext.Provider value={value}>{children}</CartContext.Provider>
}

export function useCart() {
  const ctx = useContext(CartContext)
  if (!ctx) throw new Error('useCart phải dùng trong CartProvider')
  return ctx
}
