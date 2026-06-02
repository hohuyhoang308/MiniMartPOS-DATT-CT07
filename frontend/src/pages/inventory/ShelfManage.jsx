import { useEffect, useState } from 'react'
import { Button, Form, Modal, Spinner, Table } from 'react-bootstrap'
import PageHeader from '../../components/ui/PageHeader'
import InfoBanner from '../../components/ui/InfoBanner'
import StatusPill from '../../components/ui/StatusPill'
import EmptyState from '../../components/ui/EmptyState'
import Loading from '../../components/ui/Loading'
import ConfirmModal from '../../components/ui/ConfirmModal'
import ShelfContentModal from '../../components/inventory/ShelfContentModal'
import { shelfApi } from '../../api/misc'
import { useToast } from '../../context/ToastContext'
import { errMsg } from '../../api/client'

const EMPTY = { code: '', name: '', capacity: 200, status: 'ACTIVE' }

export default function ShelfManage() {
  const toast = useToast()
  const [shelves, setShelves] = useState([])
  const [loading, setLoading] = useState(true)
  const [form, setForm] = useState(null)
  const [saving, setSaving] = useState(false)
  const [del, setDel] = useState(null)
  const [detail, setDetail] = useState(null)

  function load() {
    setLoading(true)
    shelfApi.list().then(setShelves).catch((e) => toast.error(errMsg(e))).finally(() => setLoading(false))
  }
  useEffect(() => { load() }, [])

  async function save(e) {
    e.preventDefault(); setSaving(true)
    try {
      if (form.id) { await shelfApi.update(form.id, form); toast.success('Đã cập nhật kệ') }
      else { await shelfApi.create(form); toast.success('Đã thêm kệ') }
      setForm(null); load()
    } catch (e) { toast.error(errMsg(e)) } finally { setSaving(false) }
  }
  async function remove() {
    try { await shelfApi.remove(del.id); toast.success('Đã xóa kệ'); setDel(null); load() }
    catch (e) { toast.error(errMsg(e)); setDel(null) }
  }

  if (loading) return <Loading />

  return (
    <div>
      <PageHeader title="Cấu hình kệ" subtitle="Thiết lập kệ trưng bày vật lý của cửa hàng (việc của quản lý)">
        <Button onClick={() => setForm({ ...EMPTY })}><i className="bi bi-plus-lg me-1"></i>Thêm kệ</Button>
      </PageHeader>

      <InfoBanner id="shelfmanage" title="Cấu hình kệ (quản lý)">
        Đây là nơi <b>quản lý</b> <b>thêm / sửa / xoá kệ</b> và đặt <b>sức chứa</b> (mã K01, khu vực…). Bấm vào một kệ để
        xem nó <b>chứa sản phẩm gì</b>, <b>lô/HSD</b> nào và lấy <b>"Về kho"</b> nếu cần. Còn <b>lên kệ / về kho hằng ngày</b>
        là thao tác của <b>thu ngân</b> ở trang <b>Kệ hàng (lên/về)</b>.
      </InfoBanner>

      <div className="table-wrap fade-up">
        <Table hover className="mb-0 align-middle">
          <thead><tr>
            <th>Mã kệ</th><th>Khu vực</th><th className="text-center">Số mặt hàng</th>
            <th className="text-center" style={{ minWidth: 160 }}>Đang chứa / Sức chứa</th><th className="text-center">Trạng thái</th><th className="text-end">Thao tác</th>
          </tr></thead>
          <tbody>
            {shelves.map((s) => {
              const cap = s.capacity ?? 0
              const pct = cap > 0 ? Math.min(100, Math.round((s.totalQuantity / cap) * 100)) : 0
              return (
              <tr key={s.id}>
                <td className="fw-semibold cursor-pointer" onClick={() => setDetail(s)}>
                  <span className="stat-chip chip-sky me-2" style={{ width: 32, height: 32, fontSize: '.8rem' }}><i className="bi bi-grid-3x3-gap"></i></span>{s.code}
                </td>
                <td className="text-muted2">{s.name || '—'}</td>
                <td className="text-center num">{s.productCount}</td>
                <td className="text-center">
                  <div className="num small">{s.totalQuantity} / {cap > 0 ? cap : '∞'}</div>
                  {cap > 0 && (
                    <div className="progress mt-1" style={{ height: 5, background: '#eef2f7' }}>
                      <div className="progress-bar" style={{ width: `${pct}%`, background: pct >= 100 ? '#f43f5e' : pct >= 80 ? '#f59e0b' : 'var(--brand-500)' }} />
                    </div>
                  )}
                </td>
                <td className="text-center"><StatusPill value={s.status} /></td>
                <td className="text-end">
                  <Button size="sm" variant="light" className="me-1" onClick={() => setDetail(s)} title="Xem kệ chứa gì"><i className="bi bi-eye"></i></Button>
                  <Button size="sm" variant="light" className="me-1" onClick={() => setForm({ ...s })}><i className="bi bi-pencil"></i></Button>
                  <Button size="sm" variant="light" className="text-danger" onClick={() => setDel(s)}><i className="bi bi-trash"></i></Button>
                </td>
              </tr>
            )})}
            {shelves.length === 0 && <tr><td colSpan={6}><EmptyState icon="bi-grid-3x3-gap" title="Chưa có kệ" /></td></tr>}
          </tbody>
        </Table>
      </div>

      {/* Thêm/sửa kệ */}
      <Modal show={!!form} onHide={() => setForm(null)} centered size="sm">
        <Form onSubmit={save}>
          <Modal.Header closeButton><Modal.Title>{form?.id ? 'Sửa kệ' : 'Thêm kệ'}</Modal.Title></Modal.Header>
          <Modal.Body>
            <Form.Group className="mb-3"><Form.Label>Mã kệ *</Form.Label>
              <Form.Control required value={form?.code || ''} onChange={(e) => setForm({ ...form, code: e.target.value })} placeholder="vd: K01, A1" /></Form.Group>
            <Form.Group className="mb-3"><Form.Label>Khu vực / tên</Form.Label>
              <Form.Control value={form?.name || ''} onChange={(e) => setForm({ ...form, name: e.target.value })} placeholder="vd: Nước giải khát" /></Form.Group>
            <Form.Group className="mb-3"><Form.Label>Sức chứa tối đa (0 = không giới hạn)</Form.Label>
              <Form.Control type="number" min={0} value={form?.capacity ?? 0} onChange={(e) => setForm({ ...form, capacity: Number(e.target.value) })} /></Form.Group>
            <Form.Group><Form.Label>Trạng thái</Form.Label>
              <Form.Select value={form?.status} onChange={(e) => setForm({ ...form, status: e.target.value })}>
                <option value="ACTIVE">Đang dùng</option><option value="INACTIVE">Ngừng</option>
              </Form.Select></Form.Group>
          </Modal.Body>
          <Modal.Footer>
            <Button variant="light" onClick={() => setForm(null)}>Hủy</Button>
            <Button type="submit" disabled={saving}>{saving ? <Spinner size="sm" /> : 'Lưu'}</Button>
          </Modal.Footer>
        </Form>
      </Modal>

      <ShelfContentModal shelf={detail} onHide={() => setDetail(null)} onChanged={load} />

      <ConfirmModal show={!!del} onHide={() => setDel(null)} onConfirm={remove}
        title="Xóa kệ" message={`Xóa kệ "${del?.code}"? Không xóa được nếu kệ còn hàng.`} confirmText="Xóa" />
    </div>
  )
}

