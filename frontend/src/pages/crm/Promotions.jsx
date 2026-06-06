import { useEffect, useState } from 'react'
import { Button, Col, Form, Modal, Row, Table, Spinner } from 'react-bootstrap'
import PageHeader from '../../components/ui/PageHeader'
import InfoBanner from '../../components/ui/InfoBanner'
import StatusPill from '../../components/ui/StatusPill'
import EmptyState from '../../components/ui/EmptyState'
import { SkeletonRows } from '../../components/ui/Loading'
import ConfirmModal from '../../components/ui/ConfirmModal'
import { promotionApi } from '../../api/misc'
import { useToast } from '../../context/ToastContext'
import { errMsg } from '../../api/client'
import { formatMoney } from '../../utils/format'

const EMPTY = {
  code: '', name: '', discountType: 'PERCENT', discountValue: 0,
  minOrderAmount: 0, startDate: '', endDate: '', usageLimit: '', status: 'ACTIVE',
}

function validity(p) {
  const now = new Date()
  if (p.status !== 'ACTIVE') return { cls: 'pill-muted', label: 'Ngừng', icon: 'bi-slash-circle' }
  if (new Date(p.endDate) < now) return { cls: 'pill-danger', label: 'Hết hạn', icon: 'bi-clock-history' }
  if (new Date(p.startDate) > now) return { cls: 'pill-info', label: 'Sắp diễn ra', icon: 'bi-hourglass-top' }
  if (p.usageLimit && p.usedCount >= p.usageLimit) return { cls: 'pill-warning', label: 'Hết lượt', icon: 'bi-exclamation-triangle' }
  return { cls: 'pill-success', label: 'Đang áp dụng', icon: 'bi-check-circle-fill' }
}

export default function Promotions() {
  const toast = useToast()
  const [list, setList] = useState([])
  const [loading, setLoading] = useState(true)
  const [form, setForm] = useState(null)
  const [saving, setSaving] = useState(false)
  const [del, setDel] = useState(null)

  async function load() {
    setLoading(true)
    try { setList(await promotionApi.list()) } catch (e) { toast.error(errMsg(e)) } finally { setLoading(false) }
  }
  useEffect(() => { load() }, [])

  async function save(e) {
    e.preventDefault(); setSaving(true)
    try {
      const body = {
        ...form,
        discountValue: Number(form.discountValue),
        minOrderAmount: Number(form.minOrderAmount || 0),
        usageLimit: form.usageLimit === '' ? null : Number(form.usageLimit),
      }
      if (form.id) { await promotionApi.update(form.id, body); toast.success('Đã lưu khuyến mãi') }
      else { await promotionApi.create(body); toast.success('Đã thêm khuyến mãi mới') }
      setForm(null); load()
    } catch (e) { toast.error(errMsg(e)) } finally { setSaving(false) }
  }
  async function remove() {
    try { await promotionApi.remove(del.id); toast.success('Đã xóa khuyến mãi'); setDel(null); load() }
    catch (e) { toast.error(errMsg(e)); setDel(null) }
  }
  function openEdit(p) {
    setForm({
      id: p.id, code: p.code, name: p.name, discountType: p.discountType,
      discountValue: p.discountValue, minOrderAmount: p.minOrderAmount,
      startDate: p.startDate?.slice(0, 16), endDate: p.endDate?.slice(0, 16),
      usageLimit: p.usageLimit ?? '', status: p.status,
    })
  }
  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value })

  return (
    <div>
      <PageHeader title="Khuyến mãi" subtitle="Mã giảm giá dùng khi tính tiền cho khách">
        <Button onClick={() => setForm({ ...EMPTY })}><i className="bi bi-plus-lg me-1"></i>Thêm khuyến mãi</Button>
      </PageHeader>

      <InfoBanner id="promotions" title="Tạo mã giảm giá">
        Chọn <b>cách giảm</b>: giảm theo phần trăm (%) hoặc giảm một số tiền cố định. Bạn có thể đặt <b>mức mua tối thiểu</b> mới được dùng mã,
        chọn <b>thời gian áp dụng</b> và <b>số lần được dùng</b> (để trống là dùng không giới hạn). Khi tính tiền, thu ngân nhập mã
        này để giảm cho khách. Cột <b>Hiệu lực</b> cho biết mã đang dùng được, đã hết hạn hay đã hết lượt.
      </InfoBanner>

      <div className="table-wrap fade-up">
        <Table hover className="mb-0">
          <thead>
            <tr><th>Mã / Tên</th><th>Mức giảm</th><th className="text-end">Mua tối thiểu</th><th className="text-center">Số lần đã dùng</th><th>Hiệu lực</th><th className="text-end">Thao tác</th></tr>
          </thead>
          {loading ? <SkeletonRows cols={6} /> : (
            <tbody>
              {list.map((p) => {
                const v = validity(p)
                return (
                  <tr key={p.id}>
                    <td><div className="fw-semibold"><span className="pill pill-violet me-1"><i className="bi bi-ticket-perforated"></i>{p.code}</span></div>
                      <small className="text-muted2">{p.name}</small></td>
                    <td className="fw-semibold text-success">
                      {p.discountType === 'PERCENT' ? `${p.discountValue}%` : formatMoney(p.discountValue)}</td>
                    <td className="text-end num">{formatMoney(p.minOrderAmount)}</td>
                    <td className="text-center num">{p.usedCount}{p.usageLimit ? ` / ${p.usageLimit}` : ''}</td>
                    <td><span className={`pill ${v.cls}`}><i className={`bi ${v.icon}`}></i>{v.label}</span></td>
                    <td className="text-end">
                      <Button size="sm" variant="light" className="me-1" onClick={() => openEdit(p)}><i className="bi bi-pencil"></i></Button>
                      <Button size="sm" variant="light" className="text-danger" onClick={() => setDel(p)}><i className="bi bi-trash"></i></Button>
                    </td>
                  </tr>
                )
              })}
            </tbody>
          )}
        </Table>
        {!loading && list.length === 0 && <EmptyState icon="bi-percent" title="Chưa có khuyến mãi" />}
      </div>

      <Modal show={!!form} onHide={() => setForm(null)} centered size="lg">
        <Form onSubmit={save}>
          <Modal.Header closeButton><Modal.Title>{form?.id ? 'Sửa khuyến mãi' : 'Thêm khuyến mãi'}</Modal.Title></Modal.Header>
          <Modal.Body>
            <Row>
              <Col md={4}><Form.Group className="mb-3"><Form.Label>Mã *</Form.Label>
                <Form.Control required value={form?.code || ''} onChange={set('code')} placeholder="SALE10" /></Form.Group></Col>
              <Col md={8}><Form.Group className="mb-3"><Form.Label>Tên chương trình *</Form.Label>
                <Form.Control required value={form?.name || ''} onChange={set('name')} /></Form.Group></Col>
            </Row>
            <Row>
              <Col md={4}><Form.Group className="mb-3"><Form.Label>Cách giảm</Form.Label>
                <Form.Select value={form?.discountType} onChange={set('discountType')}>
                  <option value="PERCENT">Giảm theo phần trăm (%)</option><option value="AMOUNT">Giảm số tiền (đ)</option>
                </Form.Select></Form.Group></Col>
              <Col md={4}><Form.Group className="mb-3"><Form.Label>Mức giảm *</Form.Label>
                <Form.Control type="number" min={0} required value={form?.discountValue} onChange={set('discountValue')} /></Form.Group></Col>
              <Col md={4}><Form.Group className="mb-3"><Form.Label>Mua tối thiểu</Form.Label>
                <Form.Control type="number" min={0} value={form?.minOrderAmount} onChange={set('minOrderAmount')} /></Form.Group></Col>
            </Row>
            <Row>
              <Col md={4}><Form.Group className="mb-3"><Form.Label>Bắt đầu *</Form.Label>
                <Form.Control type="datetime-local" required value={form?.startDate || ''} onChange={set('startDate')} /></Form.Group></Col>
              <Col md={4}><Form.Group className="mb-3"><Form.Label>Kết thúc *</Form.Label>
                <Form.Control type="datetime-local" required value={form?.endDate || ''} onChange={set('endDate')} /></Form.Group></Col>
              <Col md={4}><Form.Group className="mb-3"><Form.Label>Số lần được dùng</Form.Label>
                <Form.Control type="number" min={1} value={form?.usageLimit} onChange={set('usageLimit')} placeholder="Để trống là không giới hạn" /></Form.Group></Col>
            </Row>
            <Form.Group><Form.Label>Trạng thái</Form.Label>
              <Form.Select value={form?.status} onChange={set('status')}>
                <option value="ACTIVE">Đang hoạt động</option><option value="INACTIVE">Ngừng</option>
              </Form.Select></Form.Group>
          </Modal.Body>
          <Modal.Footer>
            <Button variant="light" onClick={() => setForm(null)}>Hủy</Button>
            <Button type="submit" disabled={saving}>{saving ? <Spinner size="sm" /> : 'Lưu'}</Button>
          </Modal.Footer>
        </Form>
      </Modal>

      <ConfirmModal show={!!del} onHide={() => setDel(null)} onConfirm={remove}
        title="Xóa khuyến mãi" message={`Xóa mã "${del?.code}"?`} confirmText="Xóa" />
    </div>
  )
}
