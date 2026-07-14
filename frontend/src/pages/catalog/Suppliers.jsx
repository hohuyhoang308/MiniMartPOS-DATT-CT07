import { useState } from 'react'
import { Button, Col, Form, Modal, Row, Table, Spinner } from 'react-bootstrap'
import PageHeader from '../../components/ui/PageHeader'
import StatusPill from '../../components/ui/StatusPill'
import EmptyState from '../../components/ui/EmptyState'
import { SkeletonRows } from '../../components/ui/Loading'
import ConfirmModal from '../../components/ui/ConfirmModal'
import { supplierApi } from '../../api/catalog'
import { useToast } from '../../context/ToastContext'
import { errMsg } from '../../api/client'
import { useList } from '../../hooks/useList'

const EMPTY = { name: '', phone: '', email: '', address: '', status: 'ACTIVE' }

export default function Suppliers({ embedded = false }) {
  const toast = useToast()
  const { data: list, loading, reload: load } = useList(supplierApi.list)
  const [q, setQ] = useState('')
  const [form, setForm] = useState(null)
  const [saving, setSaving] = useState(false)
  const [del, setDel] = useState(null)

  const filtered = list.filter((s) =>
    s.name.toLowerCase().includes(q.toLowerCase()) || (s.phone || '').includes(q))

  async function save(e) {
    e.preventDefault(); setSaving(true)
    try {
      if (form.id) { await supplierApi.update(form.id, form); toast.success('Đã cập nhật') }
      else { await supplierApi.create(form); toast.success('Đã thêm nhà cung cấp') }
      setForm(null); load()
    } catch (e) { toast.error(errMsg(e)) } finally { setSaving(false) }
  }
  async function remove() {
    try { await supplierApi.remove(del.id); toast.success('Đã xóa'); setDel(null); load() }
    catch (e) { toast.error(errMsg(e)); setDel(null) }
  }
  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value })

  return (
    <div>
      {embedded ? (
        <div className="d-flex justify-content-end mb-3">
          <Button onClick={() => setForm({ ...EMPTY })}><i className="bi bi-plus-lg me-1"></i>Thêm nhà cung cấp</Button>
        </div>
      ) : (
        <PageHeader title="Nhà cung cấp" subtitle="Nơi cửa hàng lấy hàng về bán">
          <Button onClick={() => setForm({ ...EMPTY })}><i className="bi bi-plus-lg me-1"></i>Thêm nhà cung cấp</Button>
        </PageHeader>
      )}

      <div className="mb-3" style={{ maxWidth: 340 }}>
        <div className="input-group">
          <span className="input-group-text"><i className="bi bi-search"></i></span>
          <Form.Control placeholder="Tìm theo tên hoặc số điện thoại…" value={q} onChange={(e) => setQ(e.target.value)} />
        </div>
      </div>

      <div className="table-wrap fade-up">
        <Table hover className="mb-0">
          <thead>
            <tr><th>Nhà cung cấp</th><th>Liên hệ</th><th>Địa chỉ</th><th>Trạng thái</th><th className="text-end">Thao tác</th></tr>
          </thead>
          {loading ? <SkeletonRows cols={5} /> : (
            <tbody>
              {filtered.map((s) => (
                <tr key={s.id}>
                  <td className="fw-semibold">{s.name}</td>
                  <td className="text-muted2">
                    {s.phone && <div><i className="bi bi-telephone me-1"></i>{s.phone}</div>}
                    {s.email && <div className="small"><i className="bi bi-envelope me-1"></i>{s.email}</div>}
                    {!s.phone && !s.email && '—'}
                  </td>
                  <td className="text-muted2">{s.address || '—'}</td>
                  <td><StatusPill value={s.status} /></td>
                  <td className="text-end">
                    <Button size="sm" variant="light" className="me-1" onClick={() => setForm({ ...s })}><i className="bi bi-pencil"></i></Button>
                    <Button size="sm" variant="light" className="text-danger" onClick={() => setDel(s)}><i className="bi bi-trash"></i></Button>
                  </td>
                </tr>
              ))}
            </tbody>
          )}
        </Table>
        {!loading && filtered.length === 0 && <EmptyState icon="bi-truck" title="Chưa có nhà cung cấp nào" />}
      </div>

      <Modal show={!!form} onHide={() => setForm(null)} centered>
        <Form onSubmit={save}>
          <Modal.Header closeButton><Modal.Title>{form?.id ? 'Sửa nhà cung cấp' : 'Thêm nhà cung cấp'}</Modal.Title></Modal.Header>
          <Modal.Body>
            <Form.Group className="mb-3">
              <Form.Label>Tên nhà cung cấp *</Form.Label>
              <Form.Control required value={form?.name || ''} onChange={set('name')} />
            </Form.Group>
            <Row>
              <Col md={6}><Form.Group className="mb-3"><Form.Label>Điện thoại</Form.Label>
                <Form.Control value={form?.phone || ''} onChange={set('phone')} /></Form.Group></Col>
              <Col md={6}><Form.Group className="mb-3"><Form.Label>Email</Form.Label>
                <Form.Control type="email" value={form?.email || ''} onChange={set('email')} /></Form.Group></Col>
            </Row>
            <Form.Group className="mb-3"><Form.Label>Địa chỉ</Form.Label>
              <Form.Control value={form?.address || ''} onChange={set('address')} /></Form.Group>
            <Form.Group><Form.Label>Trạng thái</Form.Label>
              <Form.Select value={form?.status} onChange={set('status')}>
                <option value="ACTIVE">Đang hoạt động</option><option value="INACTIVE">Ngừng</option>
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
        title="Xóa nhà cung cấp" message={`Xóa "${del?.name}"?`} confirmText="Xóa" />
    </div>
  )
}
