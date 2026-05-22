import { useEffect, useState } from 'react'
import { Button, Col, Form, Modal, Row, Table, Spinner } from 'react-bootstrap'
import PageHeader from '../../components/ui/PageHeader'
import EmptyState from '../../components/ui/EmptyState'
import { SkeletonRows } from '../../components/ui/Loading'
import ConfirmModal from '../../components/ui/ConfirmModal'
import { customerApi } from '../../api/misc'
import { useToast } from '../../context/ToastContext'
import { useAuth } from '../../context/AuthContext'
import { errMsg } from '../../api/client'
import { formatMoney, formatDateTime } from '../../utils/format'

const EMPTY = { fullName: '', phone: '', email: '' }

export default function Customers() {
  const toast = useToast()
  const { hasRole } = useAuth()
  const canEdit = hasRole('ADMIN', 'MANAGER')
  const [list, setList] = useState([])
  const [loading, setLoading] = useState(true)
  const [q, setQ] = useState('')
  const [form, setForm] = useState(null)
  const [saving, setSaving] = useState(false)
  const [del, setDel] = useState(null)
  const [history, setHistory] = useState(null)

  async function load() {
    setLoading(true)
    try { setList(await customerApi.list(q.trim() || undefined)) }
    catch (e) { toast.error(errMsg(e)) } finally { setLoading(false) }
  }
  useEffect(() => { const t = setTimeout(load, 250); return () => clearTimeout(t) }, [q])

  async function save(e) {
    e.preventDefault(); setSaving(true)
    try {
      if (form.id) { await customerApi.update(form.id, form); toast.success('Đã cập nhật') }
      else { await customerApi.create(form); toast.success('Đã thêm khách hàng') }
      setForm(null); load()
    } catch (e) { toast.error(errMsg(e)) } finally { setSaving(false) }
  }
  async function remove() {
    try { await customerApi.remove(del.id); toast.success('Đã xóa'); setDel(null); load() }
    catch (e) { toast.error(errMsg(e)); setDel(null) }
  }
  async function openHistory(c) {
    try { setHistory(await customerApi.history(c.id)) } catch (e) { toast.error(errMsg(e)) }
  }
  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value })

  return (
    <div>
      <PageHeader title="Khách hàng thân thiết" subtitle="Tích điểm & theo dõi lịch sử mua hàng">
        <Button onClick={() => setForm({ ...EMPTY })}><i className="bi bi-person-plus me-1"></i>Thêm khách</Button>
      </PageHeader>

      <div className="mb-3" style={{ maxWidth: 340 }}>
        <div className="input-group">
          <span className="input-group-text"><i className="bi bi-search"></i></span>
          <Form.Control placeholder="Tìm theo tên / SĐT…" value={q} onChange={(e) => setQ(e.target.value)} />
        </div>
      </div>

      <div className="table-wrap fade-up">
        <Table hover className="mb-0">
          <thead>
            <tr><th>Khách hàng</th><th>Liên hệ</th><th className="text-center">Điểm tích lũy</th><th>Ngày tạo</th><th className="text-end">Thao tác</th></tr>
          </thead>
          {loading ? <SkeletonRows cols={5} /> : (
            <tbody>
              {list.map((c) => (
                <tr key={c.id}>
                  <td className="fw-semibold">{c.fullName}</td>
                  <td className="text-muted2">
                    <div><i className="bi bi-telephone me-1"></i>{c.phone}</div>
                    {c.email && <div className="small"><i className="bi bi-envelope me-1"></i>{c.email}</div>}
                  </td>
                  <td className="text-center"><span className="pill pill-warning"><i className="bi bi-star-fill"></i>{c.loyaltyPoints}</span></td>
                  <td className="text-muted2 small">{formatDateTime(c.createdAt)}</td>
                  <td className="text-end">
                    <Button size="sm" variant="light" className="me-1" onClick={() => openHistory(c)} title="Lịch sử">
                      <i className="bi bi-clock-history"></i>
                    </Button>
                    {canEdit && <>
                      <Button size="sm" variant="light" className="me-1" onClick={() => setForm({ id: c.id, fullName: c.fullName, phone: c.phone, email: c.email || '' })}><i className="bi bi-pencil"></i></Button>
                      <Button size="sm" variant="light" className="text-danger" onClick={() => setDel(c)}><i className="bi bi-trash"></i></Button>
                    </>}
                  </td>
                </tr>
              ))}
            </tbody>
          )}
        </Table>
        {!loading && list.length === 0 && <EmptyState icon="bi-people" title="Chưa có khách hàng" />}
      </div>

      {/* Form thêm/sửa */}
      <Modal show={!!form} onHide={() => setForm(null)} centered>
        <Form onSubmit={save}>
          <Modal.Header closeButton><Modal.Title>{form?.id ? 'Sửa khách hàng' : 'Thêm khách hàng'}</Modal.Title></Modal.Header>
          <Modal.Body>
            <Form.Group className="mb-3"><Form.Label>Họ tên *</Form.Label>
              <Form.Control required value={form?.fullName || ''} onChange={set('fullName')} /></Form.Group>
            <Form.Group className="mb-3"><Form.Label>Số điện thoại *</Form.Label>
              <Form.Control required value={form?.phone || ''} onChange={set('phone')} placeholder="09xxxxxxxx" /></Form.Group>
            <Form.Group><Form.Label>Email</Form.Label>
              <Form.Control type="email" value={form?.email || ''} onChange={set('email')} /></Form.Group>
          </Modal.Body>
          <Modal.Footer>
            <Button variant="light" onClick={() => setForm(null)}>Hủy</Button>
            <Button type="submit" disabled={saving}>{saving ? <Spinner size="sm" /> : 'Lưu'}</Button>
          </Modal.Footer>
        </Form>
      </Modal>

      {/* Lịch sử */}
      <Modal show={!!history} onHide={() => setHistory(null)} centered size="lg">
        <Modal.Header closeButton><Modal.Title>Lịch sử mua — {history?.fullName}</Modal.Title></Modal.Header>
        <Modal.Body>
          <Row className="g-3 mb-3">
            <Col xs={4}><div className="soft-card p-3 text-center"><div className="text-muted2 small">Tổng chi tiêu</div>
              <div className="num fw-bold fs-5 text-success">{formatMoney(history?.totalSpent)}</div></div></Col>
            <Col xs={4}><div className="soft-card p-3 text-center"><div className="text-muted2 small">Số hóa đơn</div>
              <div className="num fw-bold fs-5">{history?.invoiceCount}</div></div></Col>
            <Col xs={4}><div className="soft-card p-3 text-center"><div className="text-muted2 small">Điểm tích lũy</div>
              <div className="num fw-bold fs-5 text-warning">{history?.loyaltyPoints}</div></div></Col>
          </Row>
          <Table size="sm" hover>
            <thead><tr><th>Mã HĐ</th><th>Thời gian</th><th className="text-end">Tổng tiền</th></tr></thead>
            <tbody>
              {(history?.invoices || []).map((i) => (
                <tr key={i.id}><td className="fw-semibold">{i.code}</td><td className="text-muted2 small">{i.createdAt}</td>
                  <td className="text-end num">{formatMoney(i.totalAmount)}</td></tr>
              ))}
              {history?.invoices?.length === 0 && <tr><td colSpan={3} className="text-center text-muted2 py-3">Chưa có giao dịch</td></tr>}
            </tbody>
          </Table>
        </Modal.Body>
      </Modal>

      <ConfirmModal show={!!del} onHide={() => setDel(null)} onConfirm={remove}
        title="Xóa khách hàng" message={`Xóa "${del?.fullName}"?`} confirmText="Xóa" />
    </div>
  )
}
