import { useState } from 'react'
import { Button, Card, Form, Modal, Spinner, Table } from 'react-bootstrap'
import PageHeader from '../../components/ui/PageHeader'
import InfoBanner from '../../components/ui/InfoBanner'
import EmptyState from '../../components/ui/EmptyState'
import Loading from '../../components/ui/Loading'
import { storeApi } from '../../api/misc'
import { useToast } from '../../context/ToastContext'
import { errMsg } from '../../api/client'
import { useList } from '../../hooks/useList'

const EMPTY = { code: '', name: '', address: '', phone: '', status: 'ACTIVE' }

/** Quản lý CHI NHÁNH trong chuỗi (đa chuỗi) — chỉ quản trị toàn chuỗi. */
export default function Stores() {
  const toast = useToast()
  const { data: list, loading, reload: load } = useList(storeApi.list)
  const [form, setForm] = useState(null)
  const [saving, setSaving] = useState(false)

  async function save(e) {
    e.preventDefault(); setSaving(true)
    try {
      if (form.id) { await storeApi.update(form.id, form); toast.success('Đã cập nhật chi nhánh') }
      else { await storeApi.create(form); toast.success('Đã tạo chi nhánh') }
      setForm(null); load()
    } catch (e) { toast.error(errMsg(e)) } finally { setSaving(false) }
  }

  const set = (k) => (e) => setForm((f) => ({ ...f, [k]: e.target.value }))

  if (loading) return <Loading />

  return (
    <div className="page-fill">
      <PageHeader title="Cửa hàng (chuỗi)" subtitle="Quản trị viên quản lý các cửa hàng trong chuỗi">
        <Button onClick={() => setForm({ ...EMPTY })}><i className="bi bi-plus-lg me-1"></i>Thêm chi nhánh</Button>
      </PageHeader>

      <InfoBanner id="stores" title="Trang này dùng để làm gì?">
        Mỗi <b>chi nhánh</b> có tồn kho, kệ, ca làm việc, hóa đơn và cấu hình (ngân hàng/VietQR, WEB2M, Telegram) <b>riêng</b>.
        Sản phẩm, khách hàng và khuyến mãi <b>dùng chung toàn chuỗi</b>. Bạn gán mỗi quản lý/nhân viên vào một cửa hàng
        ở mục <b>Tài khoản</b>, và cấu hình riêng từng cửa hàng ở mục <b>Cấu hình</b>.
      </InfoBanner>

      <Card className="border-0 fill-card">
        <div className="table-responsive fill-scroll">
          <Table hover className="mb-0 align-middle">
            <thead><tr>
              <th>Mã</th><th>Tên chi nhánh</th><th>Địa chỉ</th><th>Điện thoại</th>
              <th className="text-center">Trạng thái</th><th className="text-end">Thao tác</th>
            </tr></thead>
            <tbody>
              {list.map((s) => (
                <tr key={s.id}>
                  <td className="fw-bold">{s.code}</td>
                  <td>{s.name}</td>
                  <td className="text-muted2 small">{s.address || '—'}</td>
                  <td className="num">{s.phone || '—'}</td>
                  <td className="text-center">
                    <span className={`pill ${s.status === 'ACTIVE' ? 'pill-success' : 'pill-muted'}`}>
                      {s.status === 'ACTIVE' ? 'Đang hoạt động' : 'Ngừng'}
                    </span>
                  </td>
                  <td className="text-end">
                    <Button size="sm" variant="outline-primary" onClick={() => setForm({ ...s })}>
                      <i className="bi bi-pencil"></i>
                    </Button>
                  </td>
                </tr>
              ))}
              {list.length === 0 && <tr><td colSpan={6}><EmptyState icon="bi-shop" title="Chưa có chi nhánh nào" /></td></tr>}
            </tbody>
          </Table>
        </div>
      </Card>

      <Modal show={!!form} onHide={() => setForm(null)} centered>
        {form && (
          <Form onSubmit={save}>
            <Modal.Header closeButton><Modal.Title>{form.id ? 'Sửa chi nhánh' : 'Thêm chi nhánh'}</Modal.Title></Modal.Header>
            <Modal.Body>
              <Form.Group className="mb-3">
                <Form.Label>Mã chi nhánh</Form.Label>
                <Form.Control value={form.code} onChange={set('code')} placeholder="VD: CH01" required />
              </Form.Group>
              <Form.Group className="mb-3">
                <Form.Label>Tên chi nhánh</Form.Label>
                <Form.Control value={form.name} onChange={set('name')} required />
              </Form.Group>
              <Form.Group className="mb-3">
                <Form.Label>Địa chỉ</Form.Label>
                <Form.Control value={form.address || ''} onChange={set('address')} />
              </Form.Group>
              <Form.Group className="mb-3">
                <Form.Label>Điện thoại</Form.Label>
                <Form.Control value={form.phone || ''} onChange={set('phone')} />
              </Form.Group>
              {form.id && (
                <Form.Group>
                  <Form.Label>Trạng thái</Form.Label>
                  <Form.Select value={form.status} onChange={set('status')}>
                    <option value="ACTIVE">Đang hoạt động</option>
                    <option value="INACTIVE">Ngừng</option>
                  </Form.Select>
                </Form.Group>
              )}
            </Modal.Body>
            <Modal.Footer>
              <Button variant="light" onClick={() => setForm(null)}>Hủy</Button>
              <Button type="submit" disabled={saving}>{saving ? <Spinner size="sm" /> : 'Lưu'}</Button>
            </Modal.Footer>
          </Form>
        )}
      </Modal>
    </div>
  )
}
