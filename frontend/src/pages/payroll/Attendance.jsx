import { useEffect, useMemo, useState } from 'react'
import { Badge, Button, Card, Col, Form, Modal, Row, Spinner, Table } from 'react-bootstrap'
import InfoBanner from '../../components/ui/InfoBanner'
import EmptyState from '../../components/ui/EmptyState'
import ConfirmModal from '../../components/ui/ConfirmModal'
import { payrollApi } from '../../api/misc'
import { useToast } from '../../context/ToastContext'
import { useStoreScope } from '../../context/StoreScopeContext'
import { useAuth } from '../../context/AuthContext'
import { errMsg } from '../../api/client'
import { currentMonth as thisMonth } from '../../utils/format'

const TYPE = {
  WORK: { label: 'Giờ làm (ngoài ca)', cls: 'pill-info', icon: 'bi-clock' },
  LEAVE_PAID: { label: 'Nghỉ phép có lương', cls: 'pill-violet', icon: 'bi-calendar-check' },
  LEAVE_UNPAID: { label: 'Nghỉ không lương', cls: 'pill-muted', icon: 'bi-calendar-x' },
}
const fmtDate = (d) => (d ? d.split('-').reverse().join('/') : '—')

/** Bảng công: chấm công thủ công & nghỉ phép — bổ sung công ngoài ca thu ngân, dùng để tính lương. */
export default function Attendance() {
  const toast = useToast()
  const { isChainScope } = useStoreScope()
  const { isAdmin } = useAuth()
  const [month, setMonth] = useState(thisMonth())
  const [entries, setEntries] = useState([])
  const [employees, setEmployees] = useState([])
  const [loading, setLoading] = useState(true)
  const [form, setForm] = useState(null)
  const [saving, setSaving] = useState(false)
  const [del, setDel] = useState(null)

  const needStore = isAdmin && isChainScope

  async function load() {
    if (needStore) { setEntries([]); setLoading(false); return }
    setLoading(true)
    try {
      const [es, emps] = await Promise.all([payrollApi.attendance(month), payrollApi.payProfiles()])
      setEntries(es); setEmployees(emps)
    } catch (e) { toast.error(errMsg(e)) } finally { setLoading(false) }
  }
  useEffect(() => { load() }, [month]) // eslint-disable-line react-hooks/exhaustive-deps

  async function save(e) {
    e.preventDefault(); setSaving(true)
    try {
      const saved = await payrollApi.addAttendance({
        userId: Number(form.userId), workDate: form.workDate,
        type: form.type, hours: Number(form.hours), reason: form.reason || null,
      })
      // Backend cảnh báo khi ngày đó nhân viên đã có ca (nguy cơ nhập trùng giờ với ca)
      if (saved?.warning) toast.warning(saved.warning)
      else toast.success('Đã chấm công')
      setForm(null); load()
    } catch (e) { toast.error(errMsg(e)) } finally { setSaving(false) }
  }
  async function doDelete() {
    try { await payrollApi.removeAttendance(del.id); toast.success('Đã xóa'); setDel(null); load() }
    catch (e) { toast.error(errMsg(e)); setDel(null) }
  }

  const totals = useMemo(() => {
    const paid = entries.filter((e) => e.type !== 'LEAVE_UNPAID').reduce((s, e) => s + Number(e.hours || 0), 0)
    const leave = entries.filter((e) => e.type === 'LEAVE_PAID').reduce((s, e) => s + Number(e.hours || 0), 0)
    return { paid, leave, count: entries.length }
  }, [entries])

  return (
    <div>
      <InfoBanner id="attendance" title="Bảng công thủ công & nghỉ phép">
        Dùng cho nhân viên <b>không mở ca thu ngân</b> (kho, bảo vệ…), <b>bổ sung/sửa công</b>, hoặc ghi nhận
        <b> nghỉ phép</b>. Giờ <b>Giờ làm</b> và <b>Nghỉ phép có lương</b> được cộng vào giờ công khi
        <b> tính lương</b>; <b>Nghỉ không lương</b> chỉ để theo dõi. Ca thu ngân vẫn tự tính riêng — đừng nhập trùng.
      </InfoBanner>

      <Card className="border-0 mb-3">
        <Card.Body className="d-flex flex-wrap align-items-end gap-3">
          <Form.Group>
            <Form.Label className="small text-muted2 mb-1">Tháng</Form.Label>
            <Form.Control type="month" value={month} onChange={(e) => setMonth(e.target.value)} style={{ width: 180 }} />
          </Form.Group>
          {!needStore && (
            <>
              <div className="small text-muted2">Tổng giờ hưởng lương: <b>{totals.paid}h</b> · nghỉ phép: <b>{totals.leave}h</b> · {totals.count} bản ghi</div>
              <Button className="ms-auto" onClick={() => setForm({ userId: '', workDate: `${month}-01`, type: 'WORK', hours: 8, reason: '' })}>
                <i className="bi bi-plus-lg me-1"></i>Thêm chấm công
              </Button>
            </>
          )}
        </Card.Body>
      </Card>

      {needStore ? (
        <Card className="border-0"><Card.Body><EmptyState icon="bi-shop" title="Hãy chọn một chi nhánh cụ thể"
          hint="Bảng công theo từng chi nhánh — chọn chi nhánh ở góc trên." /></Card.Body></Card>
      ) : (
        <Card className="border-0">
          <Card.Body className="pb-0"><Card.Title className="fs-6 mb-0">Bản ghi chấm công tháng {month}</Card.Title></Card.Body>
          <div className="table-responsive">
            <Table hover className="mb-0 align-middle">
              <thead><tr>
                <th>Nhân viên</th><th>Ngày</th><th>Loại</th><th className="text-end">Số giờ</th><th>Lý do</th><th></th>
              </tr></thead>
              <tbody>
                {loading ? <tr><td colSpan={6} className="text-center py-4"><Spinner size="sm" /></td></tr> : (<>
                  {entries.map((e) => {
                    const t = TYPE[e.type] || TYPE.WORK
                    return (
                      <tr key={e.id}>
                        <td className="fw-semibold">{e.fullName}</td>
                        <td className="num">{fmtDate(e.workDate)}</td>
                        <td><span className={`pill ${t.cls}`}><i className={`bi ${t.icon}`}></i>{t.label}</span></td>
                        <td className="text-end num">{Number(e.hours)}h</td>
                        <td className="small text-muted2">{e.reason || '—'}</td>
                        <td className="text-end"><Button size="sm" variant="light" className="text-danger" title="Xóa" onClick={() => setDel(e)}><i className="bi bi-trash"></i></Button></td>
                      </tr>
                    )
                  })}
                  {entries.length === 0 && <tr><td colSpan={6}><EmptyState icon="bi-calendar-week" title="Chưa có bản ghi chấm công" hint="Bấm Thêm chấm công để ghi giờ làm hoặc nghỉ phép." /></td></tr>}
                </>)}
              </tbody>
            </Table>
          </div>
        </Card>
      )}

      <Modal show={!!form} onHide={() => setForm(null)} centered>
        <Form onSubmit={save}>
          <Modal.Header closeButton><Modal.Title>Thêm chấm công</Modal.Title></Modal.Header>
          <Modal.Body>
            <Form.Group className="mb-3"><Form.Label>Nhân viên *</Form.Label>
              <Form.Select required value={form?.userId || ''} onChange={(e) => setForm({ ...form, userId: e.target.value })}>
                <option value="">— Chọn nhân viên —</option>
                {employees.map((p) => <option key={p.userId} value={p.userId}>{p.fullName} (@{p.username})</option>)}
              </Form.Select></Form.Group>
            <Row className="g-2">
              <Col xs={6}><Form.Group><Form.Label>Ngày *</Form.Label>
                <Form.Control type="date" required value={form?.workDate || ''} onChange={(e) => setForm({ ...form, workDate: e.target.value })} /></Form.Group></Col>
              <Col xs={6}><Form.Group><Form.Label>Số giờ *</Form.Label>
                <Form.Control type="number" min={0.25} max={24} step={0.25} required value={form?.hours ?? ''} onChange={(e) => setForm({ ...form, hours: e.target.value })} /></Form.Group></Col>
            </Row>
            <Form.Group className="my-3"><Form.Label>Loại</Form.Label>
              <Form.Select value={form?.type} onChange={(e) => setForm({ ...form, type: e.target.value })}>
                <option value="WORK">Giờ làm (ngoài ca) — tính lương</option>
                <option value="LEAVE_PAID">Nghỉ phép có lương — tính lương</option>
                <option value="LEAVE_UNPAID">Nghỉ không lương — chỉ ghi nhận</option>
              </Form.Select></Form.Group>
            <Form.Group><Form.Label>Lý do</Form.Label>
              <Form.Control value={form?.reason || ''} onChange={(e) => setForm({ ...form, reason: e.target.value })} placeholder="vd: Trực kho chủ nhật, Nghỉ phép năm…" /></Form.Group>
          </Modal.Body>
          <Modal.Footer>
            <Button variant="light" onClick={() => setForm(null)}>Hủy</Button>
            <Button type="submit" disabled={saving}>{saving ? <Spinner size="sm" /> : 'Lưu'}</Button>
          </Modal.Footer>
        </Form>
      </Modal>

      <ConfirmModal show={!!del} onHide={() => setDel(null)} onConfirm={doDelete}
        title="Xóa bản ghi chấm công" message={`Xóa chấm công của ${del?.fullName} ngày ${fmtDate(del?.workDate)}?`}
        confirmText="Xóa" icon="bi-trash" />
    </div>
  )
}
