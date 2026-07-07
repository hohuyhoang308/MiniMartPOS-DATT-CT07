import { useEffect, useState } from 'react'
import { Button, Form, Modal, Spinner, Table } from 'react-bootstrap'
import PageHeader from '../../components/ui/PageHeader'
import InfoBanner from '../../components/ui/InfoBanner'
import EmptyState from '../../components/ui/EmptyState'
import ConfirmModal from '../../components/ui/ConfirmModal'
import MoneyInput from '../../components/ui/MoneyInput'
import { SkeletonRows } from '../../components/ui/Loading'
import { storePriceApi } from '../../api/misc'
import { productApi } from '../../api/catalog'
import { useToast } from '../../context/ToastContext'
import { errMsg } from '../../api/client'
import { formatMoney } from '../../utils/format'

/**
 * Giá bán RIÊNG theo chi nhánh — đặt giá khác giá gốc cho cửa hàng đang xem.
 * Backend tự lọc theo header X-Store-Id; ADMIN phải chọn chi nhánh (StoreScopeRequired) trước.
 */
export default function StorePrices({ embedded = false }) {
  const toast = useToast()
  const [rows, setRows] = useState([])
  const [loading, setLoading] = useState(true)
  const [editing, setEditing] = useState(null) // override đang sửa (null = không), {} = thêm mới
  const [clearTarget, setClearTarget] = useState(null)
  const [clearing, setClearing] = useState(false)

  async function load() {
    setLoading(true)
    try { setRows(await storePriceApi.list()) } catch (e) { toast.error(errMsg(e)) } finally { setLoading(false) }
  }
  useEffect(() => { load() }, [])

  async function doClear() {
    setClearing(true)
    try {
      await storePriceApi.clear(clearTarget.productId)
      toast.success(`Đã bỏ giá riêng cho "${clearTarget.productName}" — quay về giá gốc`)
      setClearTarget(null); load()
    } catch (e) { toast.error(errMsg(e)) } finally { setClearing(false) }
  }

  return (
    <div>
      {embedded ? (
        <div className="d-flex justify-content-end mb-3">
          <Button onClick={() => setEditing({})}><i className="bi bi-plus-lg me-1"></i>Thêm giá riêng</Button>
        </div>
      ) : (
        <PageHeader title="Giá theo chi nhánh" subtitle="Đặt giá bán riêng cho cửa hàng này, khác với giá gốc của toàn chuỗi.">
          <Button onClick={() => setEditing({})}><i className="bi bi-plus-lg me-1"></i>Thêm giá riêng</Button>
        </PageHeader>
      )}

      <InfoBanner id="store-prices" title="Giá theo chi nhánh là gì?">
        Mỗi sản phẩm có một <b>giá gốc</b> dùng chung cho toàn chuỗi. Ở đây bạn có thể đặt một
        <b> giá bán riêng</b> chỉ áp dụng cho <b>cửa hàng này</b> (vd: vùng có giá thuê cao hơn).
        Khi có giá riêng, POS của cửa hàng sẽ bán theo giá đó. Bấm <b>"Bỏ giá riêng"</b> để quay về giá gốc.
      </InfoBanner>

      <div className="table-wrap fade-up">
        <Table hover className="mb-0 align-middle">
          <thead><tr>
            <th>Sản phẩm</th><th className="text-end">Giá gốc</th><th className="text-end">Giá tại chi nhánh</th><th className="text-end">Thao tác</th>
          </tr></thead>
          {loading ? <SkeletonRows cols={4} /> : (
            <tbody>
              {rows.map((r) => {
                const diff = Number(r.storePrice || 0) - Number(r.basePrice || 0)
                return (
                  <tr key={r.productId}>
                    <td className="fw-semibold">{r.productName}</td>
                    <td className="text-end num text-muted2">{formatMoney(r.basePrice)}</td>
                    <td className="text-end num fw-semibold text-primary">
                      {formatMoney(r.storePrice)}
                      {diff !== 0 && (
                        <span className={`small ms-2 ${diff > 0 ? 'text-danger' : 'text-success'}`}>
                          ({diff > 0 ? '+' : ''}{formatMoney(diff)})
                        </span>
                      )}
                    </td>
                    <td className="text-end">
                      <div className="d-inline-flex gap-2">
                        <Button size="sm" variant="outline-secondary" onClick={() => setEditing(r)}>
                          <i className="bi bi-pencil me-1"></i>Sửa
                        </Button>
                        <Button size="sm" variant="outline-danger" onClick={() => setClearTarget(r)}>
                          <i className="bi bi-arrow-counterclockwise me-1"></i>Bỏ giá riêng
                        </Button>
                      </div>
                    </td>
                  </tr>
                )
              })}
            </tbody>
          )}
        </Table>
        {!loading && rows.length === 0 && <EmptyState icon="bi-tag" title="Chưa có giá riêng nào" hint="Mọi sản phẩm đang bán theo giá gốc của chuỗi." />}
      </div>

      <PriceModal override={editing} onHide={() => setEditing(null)} onDone={() => { setEditing(null); load() }} />

      <ConfirmModal show={!!clearTarget} onHide={() => setClearTarget(null)} loading={clearing}
        title="Bỏ giá riêng" confirmText="Bỏ giá riêng" icon="bi-arrow-counterclockwise"
        message={`Bỏ giá riêng cho "${clearTarget?.productName}"? Cửa hàng sẽ bán theo giá gốc.`}
        onConfirm={doClear} />
    </div>
  )
}

/** Modal đặt/sửa giá riêng: thêm mới thì tìm chọn sản phẩm; sửa thì khóa sản phẩm. */
function PriceModal({ override, onHide, onDone }) {
  const toast = useToast()
  const isEdit = !!override?.productId
  const [products, setProducts] = useState([])
  const [productId, setProductId] = useState('')
  const [salePrice, setSalePrice] = useState(0)
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    if (!override) return
    if (isEdit) {
      setProductId(String(override.productId))
      setSalePrice(Number(override.storePrice || 0))
    } else {
      setProductId(''); setSalePrice(0)
      productApi.list().then(setProducts).catch(() => setProducts([]))
    }
  }, [override, isEdit])

  if (!override) return null
  const chosen = products.find((p) => String(p.id) === String(productId))
  const basePrice = isEdit ? override.basePrice : chosen?.salePrice

  async function submit(e) {
    e.preventDefault()
    if (!productId) { toast.warning('Vui lòng chọn sản phẩm'); return }
    if (Number(salePrice) <= 0) { toast.warning('Giá bán phải lớn hơn 0'); return }
    setSaving(true)
    try {
      await storePriceApi.set(Number(productId), Number(salePrice))
      toast.success('Đã lưu giá riêng cho chi nhánh này')
      onDone()
    } catch (e) { toast.error(errMsg(e)) } finally { setSaving(false) }
  }

  return (
    <Modal show={!!override} onHide={onHide} centered>
      <Form onSubmit={submit}>
        <Modal.Header closeButton><Modal.Title>{isEdit ? `Sửa giá · ${override.productName}` : 'Thêm giá riêng'}</Modal.Title></Modal.Header>
        <Modal.Body>
          {!isEdit && (
            <Form.Group className="mb-3">
              <Form.Label>Sản phẩm *</Form.Label>
              <Form.Select value={productId} onChange={(e) => setProductId(e.target.value)}>
                <option value="">— chọn sản phẩm —</option>
                {products.map((p) => <option key={p.id} value={p.id}>{p.name} ({p.barcode})</option>)}
              </Form.Select>
            </Form.Group>
          )}

          {basePrice != null && (
            <div className="text-muted2 small mb-3"><i className="bi bi-info-circle me-1"></i>Giá gốc: <b>{formatMoney(basePrice)}</b></div>
          )}

          <Form.Group>
            <Form.Label>Giá bán tại chi nhánh *</Form.Label>
            <MoneyInput value={salePrice} onChange={setSalePrice} />
          </Form.Group>
        </Modal.Body>
        <Modal.Footer>
          <Button variant="light" onClick={onHide}>Hủy</Button>
          <Button type="submit" disabled={saving}>{saving ? <Spinner size="sm" /> : 'Lưu giá riêng'}</Button>
        </Modal.Footer>
      </Form>
    </Modal>
  )
}
