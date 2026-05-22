import { useEffect, useState } from 'react'
import { Button, Form, Modal, Table, Spinner } from 'react-bootstrap'
import PageHeader from '../../components/ui/PageHeader'
import StatusPill from '../../components/ui/StatusPill'
import EmptyState from '../../components/ui/EmptyState'
import { SkeletonRows } from '../../components/ui/Loading'
import ConfirmModal from '../../components/ui/ConfirmModal'
import { categoryApi } from '../../api/catalog'
import { useToast } from '../../context/ToastContext'
import { errMsg } from '../../api/client'

const EMPTY = { name: '', description: '', status: 'ACTIVE' }

export default function Categories() {
  const toast = useToast()
  const [list, setList] = useState([])
  const [loading, setLoading] = useState(true)
  const [q, setQ] = useState('')
  const [form, setForm] = useState(null) // null=ẩn; {id?,...}=hiện
  const [saving, setSaving] = useState(false)
  const [del, setDel] = useState(null)

  async function load() {
    setLoading(true)
    try { setList(await categoryApi.list()) }
    catch (e) { toast.error(errMsg(e)) }
    finally { setLoading(false) }
  }
  useEffect(() => { load() }, [])

  const filtered = list.filter((c) => c.name.toLowerCase().includes(q.toLowerCase()))

  async function save(e) {
    e.preventDefault()
    setSaving(true)
    try {
      if (form.id) { await categoryApi.update(form.id, form); toast.success('Đã cập nhật danh mục') }
      else { await categoryApi.create(form); toast.success('Đã thêm danh mục') }
      setForm(null); load()
    } catch (e) { toast.error(errMsg(e)) }
    finally { setSaving(false) }
  }

  async function remove() {
    try { await categoryApi.remove(del.id); toast.success('Đã xóa danh mục'); setDel(null); load() }
    catch (e) { toast.error(errMsg(e)); setDel(null) }
  }

  return (
    <div>
      <PageHeader title="Danh mục sản phẩm" subtitle="Phân loại nhóm hàng trong cửa hàng">
        <Button onClick={() => setForm({ ...EMPTY })}><i className="bi bi-plus-lg me-1"></i>Thêm danh mục</Button>
      </PageHeader>

      <div className="mb-3" style={{ maxWidth: 320 }}>
        <div className="input-group">
          <span className="input-group-text"><i className="bi bi-search"></i></span>
          <Form.Control placeholder="Tìm danh mục…" value={q} onChange={(e) => setQ(e.target.value)} />
        </div>
      </div>

      <div className="table-wrap fade-up">
        <Table hover className="mb-0">
          <thead>
            <tr><th>Tên danh mục</th><th>Mô tả</th><th>Trạng thái</th><th className="text-end">Thao tác</th></tr>
          </thead>
          {loading ? <SkeletonRows cols={4} /> : (
            <tbody>
              {filtered.map((c) => (
                <tr key={c.id}>
                  <td className="fw-semibold">{c.name}</td>
                  <td className="text-muted2">{c.description || '—'}</td>
                  <td><StatusPill value={c.status} /></td>
                  <td className="text-end">
                    <Button size="sm" variant="light" className="me-1" onClick={() => setForm({ ...c })}>
                      <i className="bi bi-pencil"></i>
                    </Button>
                    <Button size="sm" variant="light" className="text-danger" onClick={() => setDel(c)}>
                      <i className="bi bi-trash"></i>
                    </Button>
                  </td>
                </tr>
              ))}
            </tbody>
          )}
        </Table>
        {!loading && filtered.length === 0 && <EmptyState icon="bi-tags" title="Chưa có danh mục" />}
      </div>

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
        title="Xóa danh mục" message={`Xóa "${del?.name}"? Không thể xóa nếu còn sản phẩm thuộc danh mục.`}
        confirmText="Xóa" />
    </div>
  )
}
