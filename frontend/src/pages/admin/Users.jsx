import { useEffect, useState } from 'react'
import { Button, Form, Modal, Table, Spinner } from 'react-bootstrap'
import PageHeader from '../../components/ui/PageHeader'
import StatusPill from '../../components/ui/StatusPill'
import { SkeletonRows } from '../../components/ui/Loading'
import ConfirmModal from '../../components/ui/ConfirmModal'
import { userApi } from '../../api/misc'
import { useToast } from '../../context/ToastContext'
import { errMsg } from '../../api/client'
import { formatDateTime } from '../../utils/format'

export default function Users() {
  const toast = useToast()
  const [list, setList] = useState([])
  const [loading, setLoading] = useState(true)
  const [form, setForm] = useState(null)         // create/edit
  const [pwd, setPwd] = useState(null)            // reset password target
  const [newPwd, setNewPwd] = useState('')
  const [lockTarget, setLockTarget] = useState(null)
  const [saving, setSaving] = useState(false)

  async function load() {
    setLoading(true)
    try { setList(await userApi.list()) } catch (e) { toast.error(errMsg(e)) } finally { setLoading(false) }
  }
  useEffect(() => { load() }, [])

  async function save(e) {
    e.preventDefault(); setSaving(true)
    try {
      if (form.id) {
        await userApi.update(form.id, { fullName: form.fullName, role: form.role, status: form.status })
        toast.success('Đã cập nhật tài khoản')
      } else {
        await userApi.create({ username: form.username, password: form.password, fullName: form.fullName, role: form.role })
        toast.success('Đã tạo tài khoản')
      }
      setForm(null); load()
    } catch (e) { toast.error(errMsg(e)) } finally { setSaving(false) }
  }
  async function resetPwd(e) {
    e.preventDefault(); setSaving(true)
    try { await userApi.resetPassword(pwd.id, newPwd); toast.success('Đã đặt lại mật khẩu'); setPwd(null); setNewPwd('') }
    catch (e) { toast.error(errMsg(e)) } finally { setSaving(false) }
  }
  async function doLock() {
    try { await userApi.lock(lockTarget.id); toast.success('Đã khóa tài khoản'); setLockTarget(null); load() }
    catch (e) { toast.error(errMsg(e)); setLockTarget(null) }
  }
  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value })

  return (
    <div>
      <PageHeader title="Quản lý tài khoản" subtitle="Nhân viên & phân quyền hệ thống">
        <Button onClick={() => setForm({ username: '', password: '', fullName: '', role: 'CASHIER' })}>
          <i className="bi bi-person-plus me-1"></i>Thêm tài khoản
        </Button>
      </PageHeader>

      <div className="table-wrap fade-up">
        <Table hover className="mb-0">
          <thead><tr><th>Tài khoản</th><th>Họ tên</th><th>Vai trò</th><th>Trạng thái</th><th>Ngày tạo</th><th className="text-end">Thao tác</th></tr></thead>
          {loading ? <SkeletonRows cols={6} /> : (
            <tbody>
              {list.map((u) => (
                <tr key={u.id}>
                  <td className="fw-semibold">@{u.username}</td>
                  <td>{u.fullName}</td>
                  <td><StatusPill value={u.role} /></td>
                  <td><StatusPill value={u.status} /></td>
                  <td className="text-muted2 small">{formatDateTime(u.createdAt)}</td>
                  <td className="text-end">
                    <Button size="sm" variant="light" className="me-1" title="Sửa"
                      onClick={() => setForm({ id: u.id, fullName: u.fullName, role: u.role, status: u.status })}><i className="bi bi-pencil"></i></Button>
                    <Button size="sm" variant="light" className="me-1" title="Đặt lại mật khẩu" onClick={() => setPwd(u)}><i className="bi bi-key"></i></Button>
                    {u.status === 'ACTIVE' && <Button size="sm" variant="light" className="text-danger" title="Khóa" onClick={() => setLockTarget(u)}><i className="bi bi-lock"></i></Button>}
                  </td>
                </tr>
              ))}
            </tbody>
          )}
        </Table>
      </div>

      {/* Create / Edit */}
      <Modal show={!!form} onHide={() => setForm(null)} centered>
        <Form onSubmit={save}>
          <Modal.Header closeButton><Modal.Title>{form?.id ? 'Sửa tài khoản' : 'Thêm tài khoản'}</Modal.Title></Modal.Header>
          <Modal.Body>
            {!form?.id && (
              <>
                <Form.Group className="mb-3"><Form.Label>Tên đăng nhập *</Form.Label>
                  <Form.Control required value={form?.username || ''} onChange={set('username')} /></Form.Group>
                <Form.Group className="mb-3"><Form.Label>Mật khẩu *</Form.Label>
                  <Form.Control type="password" required minLength={6} value={form?.password || ''} onChange={set('password')} /></Form.Group>
              </>
            )}
            <Form.Group className="mb-3"><Form.Label>Họ tên *</Form.Label>
              <Form.Control required value={form?.fullName || ''} onChange={set('fullName')} /></Form.Group>
            <Form.Group className="mb-3"><Form.Label>Vai trò</Form.Label>
              <Form.Select value={form?.role} onChange={set('role')}>
                <option value="CASHIER">Thu ngân</option><option value="MANAGER">Quản lý</option><option value="ADMIN">Chủ cửa hàng</option>
              </Form.Select></Form.Group>
            {form?.id && (
              <Form.Group><Form.Label>Trạng thái</Form.Label>
                <Form.Select value={form?.status} onChange={set('status')}>
                  <option value="ACTIVE">Hoạt động</option><option value="LOCKED">Khóa</option>
                </Form.Select></Form.Group>
            )}
          </Modal.Body>
          <Modal.Footer>
            <Button variant="light" onClick={() => setForm(null)}>Hủy</Button>
            <Button type="submit" disabled={saving}>{saving ? <Spinner size="sm" /> : 'Lưu'}</Button>
          </Modal.Footer>
        </Form>
      </Modal>

      {/* Reset password */}
      <Modal show={!!pwd} onHide={() => setPwd(null)} centered size="sm">
        <Form onSubmit={resetPwd}>
          <Modal.Header closeButton><Modal.Title>Đặt lại mật khẩu</Modal.Title></Modal.Header>
          <Modal.Body>
            <p className="small text-muted2">Tài khoản <b>@{pwd?.username}</b></p>
            <Form.Control type="password" required minLength={6} placeholder="Mật khẩu mới" value={newPwd} onChange={(e) => setNewPwd(e.target.value)} />
          </Modal.Body>
          <Modal.Footer>
            <Button variant="light" onClick={() => setPwd(null)}>Hủy</Button>
            <Button type="submit" disabled={saving}>{saving ? <Spinner size="sm" /> : 'Đặt lại'}</Button>
          </Modal.Footer>
        </Form>
      </Modal>

      <ConfirmModal show={!!lockTarget} onHide={() => setLockTarget(null)} onConfirm={doLock}
        title="Khóa tài khoản" message={`Khóa tài khoản @${lockTarget?.username}? Họ sẽ không đăng nhập được.`}
        confirmText="Khóa" icon="bi-lock" />
    </div>
  )
}
