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
      if (form.id) { await promotionApi.update(form.id, body); toast.success('Đã cập nhật') }
      else { await promotionApi.create(body); toast.success('Đã thêm khuyến mãi') }
      setForm(null); load()
    } catch (e) { toast.error(errMsg(e)) } finally { setSaving(false) }
  }
  async function remove() {
    try { await promotionApi.remove(del.id); toast.success('Đã xóa'); setDel(null); load() }
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
      <PageHeader title="Khuyến mãi" subtitle="Mã giảm giá áp dụng tại quầy POS">
        <Button onClick={() => setForm({ ...EMPTY })}><i className="bi bi-plus-lg me-1"></i>Thêm khuyến mãi</Button>
      </PageHeader>

      <InfoBanner id="promotions" title="Tạo mã giảm giá">
        Chọn <b>loại giảm</b>: theo phần trăm (%) hoặc số tiền cố định. Đặt <b>đơn tối thiểu</b> để áp mã,
        <b> thời gian hiệu lực</b> và <b>giới hạn lượt dùng</b> (để trống = không giới hạn). Thu ngân nhập mã
        này ở màn hình POS để giảm giá. Cột <b>Hiệu lực</b> cho biết mã đang áp dụng / hết hạn / hết lượt.
      </InfoBanner>

      <div className="table-wrap fade-up">
        <Table hover className="mb-0">
          <thead>
            <tr><th>Mã / Tên</th><th>Giảm</th><th className="text-end">Đơn tối thiểu</th><th className="text-center">Lượt dùng</th><th>Hiệu lực</th><th className="text-end">Thao tác</th></tr>
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
              <Col md={4}><Form.Group className="mb-3"><Form.Label>Loại giảm</Form.Label>
                <Form.Select value={form?.discountType} onChange={set('discountType')}>
                  <option value="PERCENT">Phần trăm (%)</option><option value="AMOUNT">Số tiền (đ)</option>
                </Form.Select></Form.Group></Col>
              <Col md={4}><Form.Group className="mb-3"><Form.Label>Giá trị giảm *</Form.Label>
                <Form.Control type="number" min={0} required value={form?.discountValue} onChange={set('discountValue')} /></Form.Group></Col>
              <Col md={4}><Form.Group className="mb-3"><Form.Label>Đơn tối thiểu</Form.Label>
                <Form.Control type="number" min={0} value={form?.minOrderAmount} onChange={set('minOrderAmount')} /></Form.Group></Col>
            </Row>
            <Row>
              <Col md={4}><Form.Group className="mb-3"><Form.Label>Bắt đầu *</Form.Label>
                <Form.Control type="datetime-local" required value={form?.startDate || ''} onChange={set('startDate')} /></Form.Group></Col>
              <Col md={4}><Form.Group className="mb-3"><Form.Label>Kết thúc *</Form.Label>
                <Form.Control type="datetime-local" required value={form?.endDate || ''} onChange={set('endDate')} /></Form.Group></Col>
              <Col md={4}><Form.Group className="mb-3"><Form.Label>Giới hạn lượt</Form.Label>
                <Form.Control type="number" min={1} value={form?.usageLimit} onChange={set('usageLimit')} placeholder="Trống = không giới hạn" /></Form.Group></Col>
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
