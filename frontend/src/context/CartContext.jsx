import { createContext, useContext, useMemo, useState } from 'react'

const CartContext = createContext(null)

/** Giỏ hàng POS: danh sách dòng + khách + mã giảm giá. Giữ trong bộ nhớ phiên bán. */
export function CartProvider({ children }) {
  const [items, setItems] = useState([]) // { productId, barcode, name, salePrice, quantity, currentStock }
  const [customer, setCustomer] = useState(null)
  const [promo, setPromo] = useState(null) // { code, discountAmount, name }

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
    setCustomer(null)
    setPromo(null)
  }

  const subtotal = useMemo(
    () => items.reduce((s, i) => s + i.salePrice * i.quantity, 0),
    [items],
  )
  const discount = promo?.discountAmount || 0
  const total = Math.max(0, subtotal - discount)

  const value = {
    items, customer, promo,
    addProduct, setQuantity, removeItem, reset,
    setCustomer, setPromo,
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
