import { formatMoney } from '../../utils/format'

/* ---------------- Lưới sản phẩm (cột trái) ---------------- */
export default function ProductGrid({ products, onAdd }) {
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
