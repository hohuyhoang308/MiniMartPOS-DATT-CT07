import { useEffect, useState } from 'react'
import { Button, Card, Col, Form, Modal, Row, Spinner, Table } from 'react-bootstrap'
import PageHeader from '../../components/ui/PageHeader'
import StatusPill from '../../components/ui/StatusPill'
import EmptyState from '../../components/ui/EmptyState'
import Loading from '../../components/ui/Loading'
import ConfirmModal from '../../components/ui/ConfirmModal'
import { categoryApi, unitApi } from '../../api/catalog'
import { useToast } from '../../context/ToastContext'
import { errMsg } from '../../api/client'

const EMPTY_CAT = { name: '', description: '', status: 'ACTIVE' }

/** Gộp "Danh mục" + "Đơn vị tính" vào một màn hình cho gọn (đều là dữ liệu nền của sản phẩm). */
export default function Catalog() {
  return (
    <div>
      <PageHeader title="Danh mục & Đơn vị tính" subtitle="Dữ liệu nền phân loại sản phẩm — quản lý chung một nơi" />
      <Row className="g-3">
        <Col lg={7}><CategorySection /></Col>
        <Col lg={5}><UnitSection /></Col>
      </Row>
    </div>
  )
}

/* ---------------- Danh mục ---------------- */
function CategorySection() {
  const toast = useToast()
  const [list, setList] = useState([])
  const [loading, setLoading] = useState(true)
  const [q, setQ] = useState('')
  const [form, setForm] = useState(null)
  const [saving, setSaving] = useState(false)
  const [del, setDel] = useState(null)

  async function load() {
    setLoading(true)
    try { setList(await categoryApi.list()) } catch (e) { toast.error(errMsg(e)) } finally { setLoading(false) }
  }
  useEffect(() => { load() }, [])

  const filtered = list.filter((c) => c.name.toLowerCase().includes(q.toLowerCase()))

  async function save(e) {
    e.preventDefault(); setSaving(true)
    try {
      if (form.id) { await categoryApi.update(form.id, form); toast.success('Đã cập nhật danh mục') }
      else { await categoryApi.create(form); toast.success('Đã thêm danh mục') }
      setForm(null); load()
    } catch (e) { toast.error(errMsg(e)) } finally { setSaving(false) }
  }
  async function remove() {
    try { await categoryApi.remove(del.id); toast.success('Đã xóa danh mục'); setDel(null); load() }
    catch (e) { toast.error(errMsg(e)); setDel(null) }
  }

  return (
    <Card className="border-0 h-100">
      <Card.Body>
        <div className="d-flex justify-content-between align-items-center mb-3">
          <Card.Title className="fs-6 mb-0"><i className="bi bi-tags-fill me-2"></i>Danh mục sản phẩm</Card.Title>
          <Button size="sm" onClick={() => setForm({ ...EMPTY_CAT })}><i className="bi bi-plus-lg me-1"></i>Thêm</Button>
        </div>

        <div className="input-group mb-3">
          <span className="input-group-text"><i className="bi bi-search"></i></span>
          <Form.Control placeholder="Tìm danh mục…" value={q} onChange={(e) => setQ(e.target.value)} />
        </div>

        {loading ? <Loading /> : (
          <Table hover className="mb-0">
            <thead><tr><th>Tên</th><th>Mô tả</th><th>Trạng thái</th><th className="text-end">Thao tác</th></tr></thead>
            <tbody>
              {filtered.map((c) => (
                <tr key={c.id}>
                  <td className="fw-semibold">{c.name}</td>
                  <td className="text-muted2 small">{c.description || '—'}</td>
                  <td><StatusPill value={c.status} /></td>
                  <td className="text-end">
                    <Button size="sm" variant="light" className="me-1" onClick={() => setForm({ ...c })}><i className="bi bi-pencil"></i></Button>
                    <Button size="sm" variant="light" className="text-danger" onClick={() => setDel(c)}><i className="bi bi-trash"></i></Button>
                  </td>
                </tr>
              ))}
              {filtered.length === 0 && <tr><td colSpan={4}><EmptyState icon="bi-tags" title="Chưa có danh mục" /></td></tr>}
            </tbody>
          </Table>
        )}
      </Card.Body>

      <Modal show={!!form} onHide={() => setForm(null)} centered>
        <Form onSubmit={save}>
          <Modal.Header closeButton><Modal.Title>{form?.id ? 'Sửa danh mục' : 'Thêm danh mục'}</Modal.Title></Modal.Header>
          <Modal.Body>
            <Form.Group className="mb-3">
              <Form.Label>Tên danh mục *</Form.Label>
              <Form.Control required value={form?.name || ''} onChange={(e) => setForm({ ...form, name: e.target.value })} />
            </Form.Group>
            <Form.Group className="mb-3">
              <Form.Label>Mô tả</Form.Label>
              <Form.Control as="textarea" rows={2} value={form?.description || ''} onChange={(e) => setForm({ ...form, description: e.target.value })} />
            </Form.Group>
            <Form.Group>
              <Form.Label>Trạng thái</Form.Label>
              <Form.Select value={form?.status} onChange={(e) => setForm({ ...form, status: e.target.value })}>
                <option value="ACTIVE">Đang hoạt động</option>
                <option value="INACTIVE">Ngừng</option>
              </Form.Select>
            </Form.Group>
          </Modal.Body>
          <Modal.Footer>
            <Button variant="light" onClick={() => setForm(null)}>Hủy</Button>
            <Button type="submit" disabled={saving}>{saving ? <Spinner size="sm" /> : 'Lưu'}</Button>
          </Modal.Footer>
        </Form>
      </Modal>

      <ConfirmModal show={!!del} onHide={() => setDel(null)} onConfirm={remove}
        title="Xóa danh mục" message={`Xóa "${del?.name}"? Không thể xóa nếu còn sản phẩm thuộc danh mục.`} confirmText="Xóa" />
    </Card>
  )
}

/* ---------------- Đơn vị tính ---------------- */
function UnitSection() {
  const toast = useToast()
  const [list, setList] = useState([])
  const [loading, setLoading] = useState(true)
  const [form, setForm] = useState(null)
  const [saving, setSaving] = useState(false)
  const [del, setDel] = useState(null)

  async function load() {
    setLoading(true)
    try { setList(await unitApi.list()) } catch (e) { toast.error(errMsg(e)) } finally { setLoading(false) }
  }
  useEffect(() => { load() }, [])

  async function save(e) {
    e.preventDefault(); setSaving(true)
    try {
      if (form.id) { await unitApi.update(form.id, { name: form.name }); toast.success('Đã cập nhật') }
      else { await unitApi.create({ name: form.name }); toast.success('Đã thêm đơn vị') }
      setForm(null); load()
    } catch (e) { toast.error(errMsg(e)) } finally { setSaving(false) }
  }
  async function remove() {
    try { await unitApi.remove(del.id); toast.success('Đã xóa'); setDel(null); load() }
    catch (e) { toast.error(errMsg(e)); setDel(null) }
  }

  return (
    <Card className="border-0 h-100">
      <Card.Body>
        <div className="d-flex justify-content-between align-items-center mb-3">
          <Card.Title className="fs-6 mb-0"><i className="bi bi-rulers me-2"></i>Đơn vị tính</Card.Title>
          <Button size="sm" onClick={() => setForm({ name: '' })}><i className="bi bi-plus-lg me-1"></i>Thêm</Button>
        </div>

        {loading ? <Loading /> : list.length === 0 ? (
          <EmptyState icon="bi-rulers" title="Chưa có đơn vị tính" />
        ) : (
          <Row className="g-2">
            {list.map((u) => (
              <Col xs={6} key={u.id}>
                <div className="soft-card p-2 d-flex align-items-center justify-content-between">
                  <span className="d-flex align-items-center gap-2">
                    <span className="stat-chip chip-sky" style={{ width: 32, height: 32, fontSize: '.85rem' }}><i className="bi bi-rulers"></i></span>
                    <span className="fw-semibold">{u.name}</span>
                  </span>
                  <span>
                    <Button size="sm" variant="light" className="me-1 px-2" onClick={() => setForm({ ...u })}><i className="bi bi-pencil"></i></Button>
                    <Button size="sm" variant="light" className="text-danger px-2" onClick={() => setDel(u)}><i className="bi bi-trash"></i></Button>
                  </span>
                </div>
              </Col>
            ))}
          </Row>
        )}
      </Card.Body>

      <Modal show={!!form} onHide={() => setForm(null)} centered size="sm">
        <Form onSubmit={save}>
          <Modal.Header closeButton><Modal.Title>{form?.id ? 'Sửa đơn vị' : 'Thêm đơn vị'}</Modal.Title></Modal.Header>
          <Modal.Body>
            <Form.Label>Tên đơn vị *</Form.Label>
            <Form.Control required autoFocus value={form?.name || ''} onChange={(e) => setForm({ ...form, name: e.target.value })} placeholder="vd: Lon" />
          </Modal.Body>
          <Modal.Footer>
            <Button variant="light" onClick={() => setForm(null)}>Hủy</Button>
            <Button type="submit" disabled={saving}>{saving ? <Spinner size="sm" /> : 'Lưu'}</Button>
          </Modal.Footer>
        </Form>
      </Modal>

      <ConfirmModal show={!!del} onHide={() => setDel(null)} onConfirm={remove}
        title="Xóa đơn vị" message={`Xóa đơn vị "${del?.name}"?`} confirmText="Xóa" />
    </Card>
  )
}
