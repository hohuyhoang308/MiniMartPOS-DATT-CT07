import { useEffect, useState } from 'react'
import { Button, Col, Form, Modal, Row, Spinner } from 'react-bootstrap'
import PageHeader from '../../components/ui/PageHeader'
import EmptyState from '../../components/ui/EmptyState'
import Loading from '../../components/ui/Loading'
import ConfirmModal from '../../components/ui/ConfirmModal'
import { unitApi } from '../../api/catalog'
import { useToast } from '../../context/ToastContext'
import { errMsg } from '../../api/client'

export default function Units() {
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
    <div>
      <PageHeader title="Đơn vị tính" subtitle="Lon, chai, gói, thùng, hộp…">
        <Button onClick={() => setForm({ name: '' })}><i className="bi bi-plus-lg me-1"></i>Thêm đơn vị</Button>
      </PageHeader>

      {loading ? <Loading /> : list.length === 0 ? (
        <div className="table-wrap"><EmptyState icon="bi-rulers" title="Chưa có đơn vị tính" /></div>
      ) : (
        <Row className="g-3 stagger">
          {list.map((u) => (
            <Col md={3} sm={4} xs={6} key={u.id}>
              <div className="soft-card p-3 d-flex align-items-center justify-content-between h-100">
                <div className="d-flex align-items-center gap-2">
                  <span className="stat-chip chip-sky" style={{ width: 40, height: 40, fontSize: '1rem' }}>
                    <i className="bi bi-rulers"></i>
                  </span>
                  <span className="fw-semibold">{u.name}</span>
                </div>
                <div>
                  <Button size="sm" variant="light" className="me-1" onClick={() => setForm({ ...u })}><i className="bi bi-pencil"></i></Button>
                  <Button size="sm" variant="light" className="text-danger" onClick={() => setDel(u)}><i className="bi bi-trash"></i></Button>
                </div>
              </div>
            </Col>
          ))}
        </Row>
      )}

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
    </div>
  )
}
