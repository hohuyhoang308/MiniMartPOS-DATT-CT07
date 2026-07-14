import { useEffect, useState } from 'react'
import { Button, Form, Modal, Table, Spinner } from 'react-bootstrap'
import PageHeader from '../../components/ui/PageHeader'
import InfoBanner from '../../components/ui/InfoBanner'
import StatusPill from '../../components/ui/StatusPill'
import { SkeletonRows } from '../../components/ui/Loading'
import ConfirmModal from '../../components/ui/ConfirmModal'
import { userApi, storeApi } from '../../api/misc'
import { useToast } from '../../context/ToastContext'
import { useAuth } from '../../context/AuthContext'
import { errMsg } from '../../api/client'
import { formatDateTime } from '../../utils/format'
import { useList } from '../../hooks/useList'

export default function Users() {
  const toast = useToast()
  const { isAdmin, isManager } = useAuth()
  const { data: list, loading, reload: load } = useList(userApi.list)
  const [stores, setStores] = useState([])
  const [form, setForm] = useState(null)         // create/edit
  const [pwd, setPwd] = useState(null)            // reset password target
  const [newPwd, setNewPwd] = useState('')
  const [lockTarget, setLockTarget] = useState(null)
  const [saving, setSaving] = useState(false)

  // ADMIN toàn chuỗi cần danh sách cửa hàng để gán khi tạo MANAGER/STAFF (gọi /stores).
  useEffect(() => {
    if (isAdmin) storeApi.list().then(setStores).catch(() => {})
  }, [isAdmin])

  async function save(e) {
    e.preventDefault(); setSaving(true)
    try {
      // ADMIN không gắn cửa hàng; MANAGER/STAFF phải có cửa hàng.
      const storeId = form.role !== 'ADMIN' ? (form.storeId ? Number(form.storeId) : null) : null
      if (form.id) {
        await userApi.update(form.id, { fullName: form.fullName, role: form.role, storeId, status: form.status })
        toast.success('Đã cập nhật tài khoản')
      } else {
        await userApi.create({ username: form.username, password: form.password, fullName: form.fullName, role: form.role, storeId })
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
      <PageHeader title="Quản lý tài khoản"
        subtitle={isManager ? 'Tài khoản nhân viên (thu ngân) của cửa hàng bạn' : 'Tài khoản nhân viên và quyền của từng người'}>
        <Button onClick={() => setForm({ username: '', password: '', fullName: '', role: 'STAFF', storeId: '' })}>
          <i className="bi bi-person-plus me-1"></i>Thêm tài khoản
        </Button>
      </PageHeader>

      <InfoBanner id="users" title={isManager ? 'Quản lý nhân viên cửa hàng' : 'Các vai trò và quyền'}>
        {isManager ? (
          <>Bạn có thể tạo và quản lý tài khoản <b>nhân viên (thu ngân)</b> của <b>chính cửa hàng mình</b>.</>
        ) : (
          <><b>Quản trị viên</b>: toàn quyền TOÀN CHUỖI (mọi cửa hàng), quản lý chi nhánh & tài khoản. <b>Quản lý cửa hàng</b>: điều hành một cửa hàng (sản phẩm, kho, báo cáo, ca). <b>Nhân viên</b>: bán hàng tại quầy.</>
        )}
        {' '}Bấm <i className="bi bi-key"></i> để đặt lại mật khẩu, bấm <i className="bi bi-lock"></i> để khóa tài khoản
        (bạn không thể tự khóa chính mình). Tài khoản đã khóa sẽ không đăng nhập được.
      </InfoBanner>

      <div className="table-wrap fade-up">
        <Table hover className="mb-0">
          <thead><tr><th>Tài khoản</th><th>Họ tên</th><th>Vai trò</th><th>Chi nhánh</th><th>Trạng thái</th><th>Ngày tạo</th><th className="text-end">Thao tác</th></tr></thead>
          {loading ? <SkeletonRows cols={7} /> : (
            <tbody>
              {list.map((u) => (
                <tr key={u.id}>
                  <td className="fw-semibold">@{u.username}</td>
                  <td>{u.fullName}</td>
                  <td><StatusPill value={u.role} /></td>
                  <td className="text-muted2 small">{u.storeName || (u.role === 'ADMIN' ? 'Toàn chuỗi' : '—')}</td>
                  <td><StatusPill value={u.status} /></td>
                  <td className="text-muted2 small">{formatDateTime(u.createdAt)}</td>
                  <td className="text-end">
                    {/* MANAGER chỉ thao tác được với STAFF của cửa hàng mình; backend cũng chặn. */}
                    {(!isManager || u.role === 'STAFF') ? (
                      <>
                        <Button size="sm" variant="light" className="me-1" title="Sửa"
                          onClick={() => setForm({ id: u.id, fullName: u.fullName, role: u.role, storeId: u.storeId || '', status: u.status })}><i className="bi bi-pencil"></i></Button>
                        <Button size="sm" variant="light" className="me-1" title="Đặt lại mật khẩu" onClick={() => setPwd(u)}><i className="bi bi-key"></i></Button>
                        {u.status === 'ACTIVE' && <Button size="sm" variant="light" className="text-danger" title="Khóa" onClick={() => setLockTarget(u)}><i className="bi bi-lock"></i></Button>}
                      </>
                    ) : <span className="text-muted2 small">—</span>}
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
            {/* MANAGER chỉ tạo/sửa được nhân viên (STAFF); ADMIN chọn mọi vai trò. */}
            <Form.Group className="mb-3"><Form.Label>Vai trò</Form.Label>
              {isManager ? (
                <Form.Select value="STAFF" disabled>
                  <option value="STAFF">Nhân viên</option>
                </Form.Select>
              ) : (
                <Form.Select value={form?.role} onChange={set('role')}>
                  <option value="STAFF">Nhân viên</option>
                  <option value="MANAGER">Quản lý cửa hàng</option>
                  <option value="ADMIN">Quản trị viên (toàn chuỗi)</option>
                </Form.Select>
              )}</Form.Group>
            {/* MANAGER/STAFF phải gắn cửa hàng; ADMIN quản trị toàn chuỗi nên không gắn.
                Với MANAGER đang đăng nhập: ẩn ô này — backend tự gán cửa hàng của họ. */}
            {!isManager && form?.role !== 'ADMIN' && (
              <Form.Group className="mb-3"><Form.Label>Cửa hàng trực thuộc *</Form.Label>
                <Form.Select required value={form?.storeId || ''} onChange={set('storeId')}>
                  <option value="">— Chọn cửa hàng —</option>
                  {stores.map((s) => <option key={s.id} value={s.id}>{s.code} — {s.name}</option>)}
                </Form.Select></Form.Group>
            )}
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
