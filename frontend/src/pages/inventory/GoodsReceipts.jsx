import { useEffect, useState } from 'react'
import { Button, Col, Form, Modal, Row, Table, Spinner } from 'react-bootstrap'
import PageHeader from '../../components/ui/PageHeader'
import EmptyState from '../../components/ui/EmptyState'
import { SkeletonRows } from '../../components/ui/Loading'
import { receiptApi, supplierApi, productApi } from '../../api/catalog'
import { useToast } from '../../context/ToastContext'
import { errMsg } from '../../api/client'
import { formatMoney, formatDateTime } from '../../utils/format'

const emptyLine = () => ({ productId: '', quantity: 1, importPrice: 0, expiryDate: '' })

export default function GoodsReceipts() {
  const toast = useToast()
  const [list, setList] = useState([])
  const [loading, setLoading] = useState(true)
  const [suppliers, setSuppliers] = useState([])
  const [products, setProducts] = useState([])
  const [creating, setCreating] = useState(false)
  const [detail, setDetail] = useState(null)
  const [saving, setSaving] = useState(false)
  const [form, setForm] = useState(null)

  async function load() {
    setLoading(true)
    try { setList(await receiptApi.list()) } catch (e) { toast.error(errMsg(e)) } finally { setLoading(false) }
  }
  useEffect(() => {
    load()
    supplierApi.list().then(setSuppliers).catch(() => {})
    productApi.list().then(setProducts).catch(() => {})
  }, [])

  function openCreate() {
    setForm({ supplierId: '', note: '', updateCostPrice: true, items: [emptyLine()] })
    setCreating(true)
  }
  const total = form ? form.items.reduce((s, it) => s + Number(it.quantity || 0) * Number(it.importPrice || 0), 0) : 0
  const setItem = (idx, k, v) => setForm((f) => ({ ...f, items: f.items.map((it, i) => i === idx ? { ...it, [k]: v } : it) }))

  async function submit(e) {
    e.preventDefault()
    if (!form.supplierId) { toast.warning('Chọn nhà cung cấp'); return }
    if (form.items.some((it) => !it.productId)) { toast.warning('Chọn sản phẩm cho mọi dòng'); return }
    setSaving(true)
    try {
      await receiptApi.create({
        supplierId: Number(form.supplierId),
        note: form.note, updateCostPrice: form.updateCostPrice,
        items: form.items.map((it) => ({
          productId: Number(it.productId), quantity: Number(it.quantity),
          importPrice: Number(it.importPrice), expiryDate: it.expiryDate || null,
        })),
      })
      toast.success('Đã lập phiếu nhập, tồn kho đã tăng')
      setCreating(false); load()
    } catch (e) { toast.error(errMsg(e)) } finally { setSaving(false) }
  }

  return (
    <div>
      <PageHeader title="Nhập kho" subtitle="Lập phiếu nhập theo lô — tự động cộng tồn & ghi HSD">
        <Button onClick={openCreate}><i className="bi bi-plus-lg me-1"></i>Lập phiếu nhập</Button>
      </PageHeader>

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
        {!loading && list.length === 0 && <EmptyState icon="bi-box-arrow-in-down" title="Chưa có phiếu nhập" />}
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
                <Form.Check label="Cập nhật giá vốn" checked={form?.updateCostPrice}
                  onChange={(e) => setForm({ ...form, updateCostPrice: e.target.checked })} />
              </Col>
            </Row>

            <Table size="sm" className="align-middle">
              <thead><tr><th style={{ width: '38%' }}>Sản phẩm</th><th>Số lượng</th><th>Giá nhập</th><th>HSD</th><th className="text-end">Thành tiền</th><th></th></tr></thead>
              <tbody>
                {form?.items.map((it, idx) => (
                  <tr key={idx}>
                    <td>
                      <Form.Select size="sm" value={it.productId} onChange={(e) => setItem(idx, 'productId', e.target.value)}>
                        <option value="">— chọn —</option>
                        {products.map((p) => <option key={p.id} value={p.id}>{p.name} ({p.barcode})</option>)}
                      </Form.Select>
                    </td>
                    <td><Form.Control size="sm" type="number" min={1} value={it.quantity} onChange={(e) => setItem(idx, 'quantity', e.target.value)} /></td>
                    <td><Form.Control size="sm" type="number" min={0} value={it.importPrice} onChange={(e) => setItem(idx, 'importPrice', e.target.value)} /></td>
                    <td><Form.Control size="sm" type="date" value={it.expiryDate} onChange={(e) => setItem(idx, 'expiryDate', e.target.value)} /></td>
                    <td className="text-end num">{formatMoney(Number(it.quantity || 0) * Number(it.importPrice || 0))}</td>
                    <td className="text-end">
                      <Button size="sm" variant="light" className="text-danger" disabled={form.items.length === 1}
                        onClick={() => setForm({ ...form, items: form.items.filter((_, i) => i !== idx) })}>
                        <i className="bi bi-x-lg"></i>
                      </Button>
                    </td>
                  </tr>
                ))}
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
