import { useEffect, useState } from 'react'
import { Button, Col, Form, Modal, Row, Table, Spinner } from 'react-bootstrap'
import PageHeader from '../../components/ui/PageHeader'
import InfoBanner from '../../components/ui/InfoBanner'
import StatusPill from '../../components/ui/StatusPill'
import EmptyState from '../../components/ui/EmptyState'
import { SkeletonRows } from '../../components/ui/Loading'
import ConfirmModal from '../../components/ui/ConfirmModal'
import { productApi, categoryApi, unitApi } from '../../api/catalog'
import { useToast } from '../../context/ToastContext'
import { errMsg } from '../../api/client'
import { formatMoney } from '../../utils/format'

const EMPTY = {
  barcode: '', name: '', categoryId: '', unitId: '',
  costPrice: 0, salePrice: 0, taxRate: 8, packSize: 1, packUnitId: '', imageUrl: '', minStock: 0, status: 'ACTIVE',
}

function StockBadge({ stock, min }) {
  if (stock <= 0) return <span className="pill pill-danger"><i className="bi bi-x-octagon-fill"></i>Hết hàng</span>
  if (stock <= min) return <span className="pill pill-warning"><i className="bi bi-exclamation-triangle-fill"></i>{stock} · sắp hết</span>
  return <span className="pill pill-success"><i className="bi bi-check-circle-fill"></i>{stock}</span>
}

export default function Products({ embedded = false }) {
  const toast = useToast()
  const [list, setList] = useState([])
  const [cats, setCats] = useState([])
  const [units, setUnits] = useState([])
  const [loading, setLoading] = useState(true)
  const [keyword, setKeyword] = useState('')
  const [catFilter, setCatFilter] = useState('')
  const [form, setForm] = useState(null)
  const [saving, setSaving] = useState(false)
  const [del, setDel] = useState(null)

  async function load() {
    setLoading(true)
    try {
      const params = {}
      if (keyword.trim()) params.keyword = keyword.trim()
      if (catFilter) params.categoryId = catFilter
      setList(await productApi.list(params))
    } catch (e) { toast.error(errMsg(e)) } finally { setLoading(false) }
  }
  useEffect(() => {
    categoryApi.list().then(setCats).catch(() => {})
    unitApi.list().then(setUnits).catch(() => {})
  }, [])
  // tải lại khi đổi bộ lọc (debounce nhẹ cho keyword)
  useEffect(() => { const t = setTimeout(load, 250); return () => clearTimeout(t) }, [keyword, catFilter])

  function openCreate() {
    setForm({ ...EMPTY, categoryId: cats[0]?.id || '', unitId: units[0]?.id || '' })
  }
  function openEdit(p) {
    setForm({
      id: p.id, barcode: p.barcode, name: p.name, categoryId: p.categoryId, unitId: p.unitId,
      costPrice: p.costPrice, salePrice: p.salePrice, taxRate: p.taxRate ?? 8,
      packSize: p.packSize ?? 1, packUnitId: p.packUnitId || '', imageUrl: p.imageUrl || '',
      minStock: p.minStock, status: p.status,
    })
  }

  async function save(e) {
    e.preventDefault(); setSaving(true)
    try {
      const body = {
        ...form,
        categoryId: Number(form.categoryId), unitId: Number(form.unitId),
        costPrice: Number(form.costPrice), salePrice: Number(form.salePrice),
        taxRate: Number(form.taxRate), minStock: Number(form.minStock),
        packSize: Number(form.packSize) || 1, packUnitId: form.packUnitId ? Number(form.packUnitId) : null,
      }
      if (form.id) { await productApi.update(form.id, body); toast.success('Đã cập nhật sản phẩm') }
      else { await productApi.create(body); toast.success('Đã thêm sản phẩm') }
      setForm(null); load()
    } catch (e) { toast.error(errMsg(e)) } finally { setSaving(false) }
  }
  async function remove() {
    try { await productApi.remove(del.id); toast.success('Đã xóa sản phẩm'); setDel(null); load() }
    catch (e) { toast.error(errMsg(e)); setDel(null) }
  }
  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value })

  const margin = form ? Number(form.salePrice) - Number(form.costPrice) : 0

  return (
    <div>
      {embedded ? (
        <div className="d-flex justify-content-end mb-3">
          <Button onClick={openCreate}><i className="bi bi-plus-lg me-1"></i>Thêm sản phẩm</Button>
        </div>
      ) : (
        <PageHeader title="Sản phẩm" subtitle="Quản lý mặt hàng, giá bán và mức báo sắp hết">
          <Button onClick={openCreate}><i className="bi bi-plus-lg me-1"></i>Thêm sản phẩm</Button>
        </PageHeader>
      )}

      <InfoBanner id="products" title="Cách quản lý sản phẩm">
        Mỗi mặt hàng cần có <b>mã vạch</b> (để quét nhanh khi bán), thuộc một <b>danh mục</b> và một <b>đơn vị tính</b>.
        Hãy đặt <b>mức tồn tối thiểu</b> để máy tự báo cho bạn khi hàng sắp hết. Cột <b>Tồn kho</b> được máy tự tính
        từ các lần nhập hàng. Muốn tăng tồn kho, bạn vào mục <b>Nhập kho</b>.
      </InfoBanner>

      <Row className="g-2 mb-3">
        <Col md={5}>
          <div className="input-group">
            <span className="input-group-text"><i className="bi bi-upc-scan"></i></span>
            <Form.Control placeholder="Tìm theo tên hàng hoặc mã vạch…" value={keyword} onChange={(e) => setKeyword(e.target.value)} />
          </div>
        </Col>
        <Col md={3}>
          <Form.Select value={catFilter} onChange={(e) => setCatFilter(e.target.value)}>
            <option value="">Tất cả danh mục</option>
            {cats.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
          </Form.Select>
        </Col>
      </Row>

      <div className="table-wrap fade-up">
        <Table hover className="mb-0">
          <thead>
            <tr>
              <th>Sản phẩm</th><th>Danh mục</th><th className="text-end">Giá vốn</th>
              <th className="text-end">Giá bán</th><th className="text-center">Tồn kho</th>
              <th>Trạng thái</th><th className="text-end">Thao tác</th>
            </tr>
          </thead>
          {loading ? <SkeletonRows cols={7} /> : (
            <tbody>
              {list.map((p) => (
                <tr key={p.id}>
                  <td>
                    <div className="d-flex align-items-center gap-2">
                      <div className="rounded d-flex align-items-center justify-content-center flex-shrink-0"
                           style={{ width: 40, height: 40, background: 'var(--surface-2)', overflow: 'hidden', border: '1px solid var(--border)' }}>
                        {p.imageUrl ? <img src={p.imageUrl} alt="" style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
                          : <i className="bi bi-box text-muted2"></i>}
                      </div>
                      <div>
                        <div className="fw-semibold">{p.name}</div>
                        <small className="text-muted2">{p.barcode} · {p.unitName}</small>
                      </div>
                    </div>
                  </td>
                  <td className="text-muted2">{p.categoryName}</td>
                  <td className="text-end num">{formatMoney(p.costPrice)}</td>
                  <td className="text-end num fw-semibold">{formatMoney(p.salePrice)}</td>
                  <td className="text-center"><StockBadge stock={p.currentStock} min={p.minStock} /></td>
                  <td><StatusPill value={p.status} /></td>
                  <td className="text-end">
                    <Button size="sm" variant="light" className="me-1" onClick={() => openEdit(p)}><i className="bi bi-pencil"></i></Button>
                    <Button size="sm" variant="light" className="text-danger" onClick={() => setDel(p)}><i className="bi bi-trash"></i></Button>
                  </td>
                </tr>
              ))}
            </tbody>
          )}
        </Table>
        {!loading && list.length === 0 && <EmptyState icon="bi-box-seam" title="Chưa có sản phẩm nào" hint="Thử tìm từ khác hoặc bấm Thêm sản phẩm" />}
      </div>

      <Modal show={!!form} onHide={() => setForm(null)} centered size="lg">
        <Form onSubmit={save}>
          <Modal.Header closeButton><Modal.Title>{form?.id ? 'Sửa sản phẩm' : 'Thêm sản phẩm'}</Modal.Title></Modal.Header>
          <Modal.Body>
            <Row>
              <Col md={4}><Form.Group className="mb-3"><Form.Label>Mã vạch *</Form.Label>
                <Form.Control required value={form?.barcode || ''} onChange={set('barcode')} /></Form.Group></Col>
              <Col md={8}><Form.Group className="mb-3"><Form.Label>Tên sản phẩm *</Form.Label>
                <Form.Control required value={form?.name || ''} onChange={set('name')} /></Form.Group></Col>
            </Row>
            <Row>
              <Col md={6}><Form.Group className="mb-3"><Form.Label>Danh mục *</Form.Label>
                <Form.Select required value={form?.categoryId} onChange={set('categoryId')}>
                  <option value="">— chọn —</option>
                  {cats.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
                </Form.Select></Form.Group></Col>
              <Col md={6}><Form.Group className="mb-3"><Form.Label>Đơn vị tính *</Form.Label>
                <Form.Select required value={form?.unitId} onChange={set('unitId')}>
                  <option value="">— chọn —</option>
                  {units.map((u) => <option key={u.id} value={u.id}>{u.name}</option>)}
                </Form.Select></Form.Group></Col>
            </Row>
            <Row>
              <Col md={3}><Form.Group className="mb-3"><Form.Label>Giá vốn</Form.Label>
                <Form.Control type="number" min={0} value={form?.costPrice} onChange={set('costPrice')} /></Form.Group></Col>
              <Col md={3}><Form.Group className="mb-3"><Form.Label>Giá bán * <span className="text-muted2 small">(đã gồm VAT)</span></Form.Label>
                <Form.Control type="number" min={0} required value={form?.salePrice} onChange={set('salePrice')} /></Form.Group></Col>
              <Col md={3}><Form.Group className="mb-3"><Form.Label>Thuế VAT %</Form.Label>
                <Form.Select value={form?.taxRate} onChange={set('taxRate')}>
                  <option value={0}>0%</option><option value={8}>8%</option><option value={10}>10%</option>
                </Form.Select></Form.Group></Col>
              <Col md={3}><Form.Group className="mb-3"><Form.Label>Tồn tối thiểu</Form.Label>
                <Form.Control type="number" min={0} value={form?.minStock} onChange={set('minStock')} /></Form.Group></Col>
            </Row>
            <Row>
              <Col md={6}><Form.Group className="mb-3"><Form.Label>Đơn vị mua vào (thùng/lốc)</Form.Label>
                <Form.Select value={form?.packUnitId} onChange={set('packUnitId')}>
                  <option value="">— chỉ bán lẻ —</option>
                  {units.map((u) => <option key={u.id} value={u.id}>{u.name}</option>)}
                </Form.Select></Form.Group></Col>
              <Col md={6}><Form.Group className="mb-3"><Form.Label>Quy đổi đơn vị: 1 thùng = mấy lon (cái)?</Form.Label>
                <Form.Control type="number" min={1} value={form?.packSize} onChange={set('packSize')}
                  disabled={!form?.packUnitId} placeholder="vd: 24 (1 thùng = 24 lon)" /></Form.Group></Col>
            </Row>
            <Row className="align-items-end">
              <Col md={8}><Form.Group className="mb-3"><Form.Label>Đường dẫn ảnh sản phẩm</Form.Label>
                <Form.Control value={form?.imageUrl || ''} onChange={set('imageUrl')} placeholder="https://…" /></Form.Group></Col>
              <Col md={4}><Form.Group className="mb-3"><Form.Label>Trạng thái</Form.Label>
                <Form.Select value={form?.status} onChange={set('status')}>
                  <option value="ACTIVE">Đang bán</option><option value="INACTIVE">Ngừng bán</option>
                </Form.Select></Form.Group></Col>
            </Row>
            {form && (
              <div className="soft-card p-2 px-3 small text-muted2 d-flex justify-content-between">
                <span>Lãi mỗi sản phẩm:</span>
                <span className={margin >= 0 ? 'text-success fw-semibold' : 'text-danger fw-semibold'}>{formatMoney(margin)}</span>
              </div>
            )}
          </Modal.Body>
          <Modal.Footer>
            <Button variant="light" onClick={() => setForm(null)}>Hủy</Button>
            <Button type="submit" disabled={saving}>{saving ? <Spinner size="sm" /> : 'Lưu sản phẩm'}</Button>
          </Modal.Footer>
        </Form>
      </Modal>

      <ConfirmModal show={!!del} onHide={() => setDel(null)} onConfirm={remove}
        title="Xóa sản phẩm" message={`Xóa "${del?.name}"? Không xóa được nếu sản phẩm đã từng được bán.`}
        confirmText="Xóa" />
    </div>
  )
}
