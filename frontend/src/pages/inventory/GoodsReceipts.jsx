import { useEffect, useRef, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { Button, Col, Form, Modal, Row, Table, Spinner } from 'react-bootstrap'
import InfoBanner from '../../components/ui/InfoBanner'
import MoneyInput from '../../components/ui/MoneyInput'
import EmptyState from '../../components/ui/EmptyState'
import { SkeletonRows } from '../../components/ui/Loading'
import { receiptApi, supplierApi, productApi } from '../../api/catalog'
import { useToast } from '../../context/ToastContext'
import { errMsg } from '../../api/client'
import { formatMoney, formatDateTime } from '../../utils/format'
import { useList } from '../../hooks/useList'

const emptyLine = () => ({ id: crypto.randomUUID(), productId: '', quantity: 1, importPrice: 0, expiryDate: '', byPack: false })

export default function GoodsReceipts() {
  const toast = useToast()
  const location = useLocation()
  const navigate = useNavigate()
  const prefilledRef = useRef(false)
  const { data: list, loading, reload: load } = useList(receiptApi.list)
  const [suppliers, setSuppliers] = useState([])
  const [products, setProducts] = useState([])
  const [creating, setCreating] = useState(false)
  const [detail, setDetail] = useState(null)
  const [saving, setSaving] = useState(false)
  const [form, setForm] = useState(null)

  // Nhà cung cấp + sản phẩm phục vụ form tạo phiếu (tải nền).
  useEffect(() => {
    supplierApi.list().then(setSuppliers).catch(() => {})
    productApi.list().then(setProducts).catch(() => {})
  }, [])

  // Mở sẵn phiếu nhập từ trang "Đề xuất nhập hàng": điền sẵn mặt hàng + số lượng đề xuất.
  useEffect(() => {
    const prefill = location.state?.prefill
    if (!prefill || prefilledRef.current || products.length === 0) return
    prefilledRef.current = true
    const items = prefill.map((pf) => {
      const p = products.find((x) => x.id === pf.productId)
      return { ...emptyLine(), productId: String(pf.productId), quantity: pf.quantity || 1, importPrice: p?.costPrice ?? 0 }
    })
    setForm({ supplierId: '', note: 'Nhập hàng theo gợi ý của hệ thống', updateCostPrice: true, items: items.length ? items : [emptyLine()] })
    setCreating(true)
    navigate(location.pathname, { replace: true, state: null }) // tránh lặp khi back/refresh
  }, [products, location.state, location.pathname, navigate])

  function openCreate() {
    setForm({ supplierId: '', note: '', updateCostPrice: true, items: [emptyLine()] })
    setCreating(true)
  }
  const total = form ? form.items.reduce((s, it) => s + Number(it.quantity || 0) * Number(it.importPrice || 0), 0) : 0
  const setItem = (idx, k, v) => setForm((f) => ({ ...f, items: f.items.map((it, i) => i === idx ? { ...it, [k]: v } : it) }))

  async function submit(e) {
    e.preventDefault()
    if (!form.supplierId) { toast.warning('Vui lòng chọn nhà cung cấp'); return }
    if (form.items.some((it) => !it.productId)) { toast.warning('Vui lòng chọn sản phẩm cho tất cả các dòng'); return }
    setSaving(true)
    try {
      await receiptApi.create({
        supplierId: Number(form.supplierId),
        note: form.note, updateCostPrice: form.updateCostPrice,
        items: form.items.map((it) => {
          // Nhập theo THÙNG → quy đổi sang đơn vị bán cơ bản (lon) trước khi lưu (tồn luôn ở ĐV cơ bản).
          const p = products.find((x) => String(x.id) === String(it.productId))
          const k = it.byPack && p && p.packSize > 1 ? p.packSize : 1
          return {
            productId: Number(it.productId), quantity: Number(it.quantity) * k,
            importPrice: Number(it.importPrice) / k, expiryDate: it.expiryDate || null,
          }
        }),
      })
      toast.success('Đã lập phiếu nhập. Hàng trong kho đã được cộng thêm.')
      setCreating(false); load()
    } catch (e) { toast.error(errMsg(e)) } finally { setSaving(false) }
  }

  return (
    <div>
      <div className="d-flex justify-content-end mb-3">
        <Button onClick={openCreate}><i className="bi bi-plus-lg me-1"></i>Lập phiếu nhập</Button>
      </div>

      <InfoBanner id="receipts" title="Nhập kho hoạt động ra sao?">
        Mỗi lần nhận hàng, bạn lập một phiếu nhập. Mỗi phiếu sẽ tạo ra các <b>lô hàng</b>, mỗi lô có
        <b> giá nhập</b> và <b>hạn sử dụng</b> riêng. Khi bạn lưu phiếu, <b>hàng trong kho tự động tăng lên</b>.
        Nếu bật ô <b>"Cập nhật giá vốn"</b>, hệ thống sẽ lấy giá nhập lần này làm giá vốn mới cho sản phẩm,
        dùng để tính lãi sau này. Bấm vào một dòng để xem chi tiết phiếu.
      </InfoBanner>

      <div className="table-wrap fade-up">
        <Table hover className="mb-0">
          <thead><tr><th>Mã phiếu</th><th>Nhà cung cấp</th><th>Người lập</th><th className="text-center">Số dòng</th><th className="text-end">Tổng tiền</th><th>Thời gian</th><th></th></tr></thead>
          {loading ? <SkeletonRows cols={7} /> : (
            <tbody>
              {list.map((r) => (
                <tr key={r.id} className="cursor-pointer" onClick={() => setDetail(r)}>
                  <td className="fw-semibold">{r.code}</td>
                  <td>{r.supplierName}</td>
                  <td className="text-muted2">{r.createdByName}</td>
                  <td className="text-center">{r.items?.length || 0}</td>
                  <td className="text-end num fw-semibold">{formatMoney(r.totalAmount)}</td>
                  <td className="text-muted2 small">{formatDateTime(r.createdAt)}</td>
                  <td className="text-end"><i className="bi bi-chevron-right text-muted2"></i></td>
                </tr>
              ))}
            </tbody>
          )}
        </Table>
        {!loading && list.length === 0 && <EmptyState icon="bi-box-arrow-in-down" title="Chưa có phiếu nhập nào" />}
      </div>

      {/* Tạo phiếu */}
      <Modal show={creating} onHide={() => setCreating(false)} centered size="xl">
        <Form onSubmit={submit}>
          <Modal.Header closeButton><Modal.Title>Lập phiếu nhập kho</Modal.Title></Modal.Header>
          <Modal.Body>
            <Row className="mb-3">
              <Col md={5}><Form.Label>Nhà cung cấp *</Form.Label>
                <Form.Select value={form?.supplierId} onChange={(e) => setForm({ ...form, supplierId: e.target.value })}>
                  <option value="">— chọn —</option>
                  {suppliers.map((s) => <option key={s.id} value={s.id}>{s.name}</option>)}
                </Form.Select></Col>
              <Col md={5}><Form.Label>Ghi chú</Form.Label>
                <Form.Control value={form?.note} onChange={(e) => setForm({ ...form, note: e.target.value })} /></Col>
              <Col md={2} className="d-flex align-items-end">
                <Form.Check label="Cập nhật giá vốn theo giá nhập này" checked={form?.updateCostPrice}
                  onChange={(e) => setForm({ ...form, updateCostPrice: e.target.checked })} />
              </Col>
            </Row>

            <Table size="sm" className="align-middle">
              <thead><tr><th style={{ width: '34%' }}>Sản phẩm</th><th>Số lượng</th><th>Đơn vị nhập</th><th>Giá nhập</th><th>HSD</th><th className="text-end">Thành tiền</th><th></th></tr></thead>
              <tbody>
                {form?.items.map((it, idx) => {
                  const lp = products.find((x) => String(x.id) === String(it.productId))
                  const hasPack = lp && lp.packSize > 1 && lp.packUnitName
                  return (
                  <tr key={it.id}>
                    <td>
                      <Form.Select size="sm" value={it.productId} onChange={(e) => setItem(idx, 'productId', e.target.value)}>
                        <option value="">— chọn —</option>
                        {products.map((p) => <option key={p.id} value={p.id}>{p.name} ({p.barcode})</option>)}
                      </Form.Select>
                    </td>
                    <td>
                      <Form.Control size="sm" type="number" min={1} value={it.quantity} onChange={(e) => setItem(idx, 'quantity', e.target.value)} />
                      {hasPack && it.byPack && <div className="text-muted2" style={{ fontSize: '.72rem' }}>= {Number(it.quantity || 0) * lp.packSize} {lp.unitName}</div>}
                    </td>
                    <td>
                      {hasPack ? (
                        <Form.Select size="sm" value={it.byPack ? 'pack' : 'base'} onChange={(e) => setItem(idx, 'byPack', e.target.value === 'pack')}>
                          <option value="base">{lp.unitName} (lẻ)</option>
                          <option value="pack">{lp.packUnitName} (×{lp.packSize})</option>
                        </Form.Select>
                      ) : <span className="text-muted2 small">{lp?.unitName || '—'}</span>}
                    </td>
                    <td><MoneyInput size="sm" value={it.importPrice} onChange={(v) => setItem(idx, 'importPrice', v)} /></td>
                    <td><Form.Control size="sm" type="date" value={it.expiryDate} onChange={(e) => setItem(idx, 'expiryDate', e.target.value)} /></td>
                    <td className="text-end num">{formatMoney(Number(it.quantity || 0) * Number(it.importPrice || 0))}</td>
                    <td className="text-end">
                      <Button size="sm" variant="light" className="text-danger" disabled={form.items.length === 1}
                        onClick={() => setForm({ ...form, items: form.items.filter((_, i) => i !== idx) })}>
                        <i className="bi bi-x-lg"></i>
                      </Button>
                    </td>
                  </tr>
                )})}
              </tbody>
            </Table>
            <Button size="sm" variant="soft" onClick={() => setForm({ ...form, items: [...form.items, emptyLine()] })}>
              <i className="bi bi-plus-lg me-1"></i>Thêm dòng
            </Button>

            <div className="d-flex justify-content-end mt-3">
              <div className="soft-card px-4 py-2">
                <span className="text-muted2 me-3">Tổng tiền phiếu:</span>
                <span className="num fw-bold fs-5 text-success">{formatMoney(total)}</span>
              </div>
            </div>
          </Modal.Body>
          <Modal.Footer>
            <Button variant="light" onClick={() => setCreating(false)}>Hủy</Button>
            <Button type="submit" disabled={saving}>{saving ? <Spinner size="sm" /> : 'Lưu phiếu nhập'}</Button>
          </Modal.Footer>
        </Form>
      </Modal>

      {/* Chi tiết */}
      <Modal show={!!detail} onHide={() => setDetail(null)} centered size="lg">
        <Modal.Header closeButton><Modal.Title>Phiếu nhập {detail?.code}</Modal.Title></Modal.Header>
        <Modal.Body>
          <div className="d-flex justify-content-between mb-3 small text-muted2">
            <span><i className="bi bi-truck me-1"></i>{detail?.supplierName}</span>
            <span><i className="bi bi-person me-1"></i>{detail?.createdByName}</span>
            <span><i className="bi bi-clock me-1"></i>{formatDateTime(detail?.createdAt)}</span>
          </div>
          <Table size="sm" hover>
            <thead><tr><th>Sản phẩm</th><th className="text-center">SL</th><th className="text-end">Giá nhập</th><th>HSD</th><th className="text-end">Thành tiền</th></tr></thead>
            <tbody>
              {detail?.items?.map((it) => (
                <tr key={it.id}><td>{it.productName}</td><td className="text-center num">{it.quantity}</td>
                  <td className="text-end num">{formatMoney(it.importPrice)}</td><td>{it.expiryDate || '—'}</td>
                  <td className="text-end num">{formatMoney(it.quantity * it.importPrice)}</td></tr>
              ))}
            </tbody>
          </Table>
          <div className="text-end fw-bold">Tổng: <span className="text-success num">{formatMoney(detail?.totalAmount)}</span></div>
          {detail?.note && <div className="text-muted2 small mt-2"><i className="bi bi-sticky me-1"></i>{detail.note}</div>}
        </Modal.Body>
      </Modal>
    </div>
  )
}
