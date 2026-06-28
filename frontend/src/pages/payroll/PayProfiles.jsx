import { useEffect, useState } from 'react'
import { Button, Form, Modal, Spinner, Table } from 'react-bootstrap'
import InfoBanner from '../../components/ui/InfoBanner'
import StatusPill from '../../components/ui/StatusPill'
import { SkeletonRows } from '../../components/ui/Loading'
import EmptyState from '../../components/ui/EmptyState'
import { payrollApi } from '../../api/misc'
import { useToast } from '../../context/ToastContext'
import { errMsg } from '../../api/client'
import { formatMoney } from '../../utils/format'

/** Cấu hình lương theo từng nhân viên: loại lương (giờ/tháng), đơn giá, công chuẩn, tăng ca, phụ cấp. */
export default function PayProfiles() {
  const toast = useToast()
  const [list, setList] = useState([])
  const [loading, setLoading] = useState(true)
  const [form, setForm] = useState(null)
  const [saving, setSaving] = useState(false)

  async function load() {
    setLoading(true)
    try { setList(await payrollApi.payProfiles()) } catch (e) { toast.error(errMsg(e)) } finally { setLoading(false) }
  }
  useEffect(() => { load() }, [])

  function edit(p) {
    setForm({
      userId: p.userId, fullName: p.fullName, payType: p.payType,
      baseRate: p.baseRate, standardMonthlyHours: p.standardMonthlyHours,
      otMultiplier: p.otMultiplier, monthlyAllowance: p.monthlyAllowance,
    })
  }
  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value })

  async function save(e) {
    e.preventDefault(); setSaving(true)
    try {
      await payrollApi.setPayProfile(form.userId, {
        payType: form.payType,
        baseRate: Number(form.baseRate || 0),
        standardMonthlyHours: Number(form.standardMonthlyHours || 208),
        otMultiplier: Number(form.otMultiplier || 1.5),
        monthlyAllowance: Number(form.monthlyAllowance || 0),
      })
      toast.success('Đã lưu cấu hình lương')
      setForm(null); load()
    } catch (e) { toast.error(errMsg(e)) } finally { setSaving(false) }
  }

  return (
    <div>
      <InfoBanner id="pay-profiles" title="Cấu hình lương nhân viên">
        Mỗi nhân viên có một mức lương: <b>theo giờ</b> (đồng/giờ) hoặc <b>theo tháng</b> (đồng/tháng, trả theo
        công đã làm). <b>Công chuẩn/tháng</b> là mốc đủ công (mặc định 208 giờ = 26 ngày × 8h); làm vượt mốc này
        tính <b>tăng ca</b> theo hệ số. <b>Phụ cấp</b> là khoản cố định cộng mỗi tháng. Số giờ công lấy tự động
        từ <b>ca đã đóng</b> nên không cần chấm công thủ công.
      </InfoBanner>

      <div className="table-wrap fade-up">
        <Table hover className="mb-0 align-middle">
          <thead><tr>
            <th>Nhân viên</th><th>Vai trò</th><th>Chi nhánh</th><th>Loại lương</th>
            <th className="text-end">Đơn giá</th><th className="text-end">Công chuẩn/tháng</th>
            <th className="text-end">Hệ số tăng ca</th><th className="text-end">Phụ cấp/tháng</th>
            <th className="text-center">Trạng thái</th><th className="text-end">Thao tác</th>
          </tr></thead>
          {loading ? <SkeletonRows cols={10} /> : (
            <tbody>
              {list.map((p) => (
                <tr key={p.userId}>
                  <td className="fw-semibold">{p.fullName}<div className="text-muted2 small">@{p.username}</div></td>
                  <td><StatusPill value={p.role} /></td>
                  <td className="text-muted2 small">{p.storeName || '—'}</td>
                  <td>{p.payType === 'HOURLY'
                    ? <span className="pill pill-info"><i className="bi bi-clock"></i>Theo giờ</span>
                    : <span className="pill pill-violet"><i className="bi bi-calendar-month"></i>Theo tháng</span>}</td>
                  <td className="text-end num">{formatMoney(p.baseRate)}{p.payType === 'HOURLY' ? '/giờ' : '/tháng'}</td>
                  <td className="text-end num">{Number(p.standardMonthlyHours)}h</td>
                  <td className="text-end num">×{Number(p.otMultiplier)}</td>
                  <td className="text-end num">{formatMoney(p.monthlyAllowance)}</td>
                  <td className="text-center">{p.configured
                    ? <span className="pill pill-success"><i className="bi bi-check-circle-fill"></i>Đã cấu hình</span>
                    : <span className="pill pill-warning"><i className="bi bi-exclamation-triangle"></i>Chưa cấu hình</span>}</td>
                  <td className="text-end">
                    <Button size="sm" variant="light" title="Cấu hình lương" onClick={() => edit(p)}>
                      <i className="bi bi-pencil"></i>
                    </Button>
                  </td>
                </tr>
              ))}
              {list.length === 0 && <tr><td colSpan={10}><EmptyState icon="bi-person-badge" title="Chưa có nhân viên trong phạm vi" /></td></tr>}
            </tbody>
          )}
        </Table>
      </div>

      <Modal show={!!form} onHide={() => setForm(null)} centered>
        <Form onSubmit={save}>
          <Modal.Header closeButton><Modal.Title>Cấu hình lương — {form?.fullName}</Modal.Title></Modal.Header>
          <Modal.Body>
            <Form.Group className="mb-3"><Form.Label>Loại lương</Form.Label>
              <Form.Select value={form?.payType} onChange={set('payType')}>
                <option value="MONTHLY">Theo tháng (trả theo công)</option>
                <option value="HOURLY">Theo giờ</option>
              </Form.Select></Form.Group>
            <Form.Group className="mb-3">
              <Form.Label>Đơn giá lương ({form?.payType === 'HOURLY' ? 'đồng/giờ' : 'đồng/tháng'}) *</Form.Label>
              <Form.Control type="number" min={0} step={1000} required value={form?.baseRate ?? ''} onChange={set('baseRate')} />
            </Form.Group>
            <Form.Group className="mb-3"><Form.Label>Công chuẩn/tháng (giờ)</Form.Label>
              <Form.Control type="number" min={1} step={1} required value={form?.standardMonthlyHours ?? ''} onChange={set('standardMonthlyHours')} />
              <Form.Text className="text-muted2">Mặc định 208h (26 ngày × 8h). Làm vượt mốc này tính tăng ca.</Form.Text>
            </Form.Group>
            <Form.Group className="mb-3"><Form.Label>Hệ số tăng ca</Form.Label>
              <Form.Control type="number" min={1} step={0.1} required value={form?.otMultiplier ?? ''} onChange={set('otMultiplier')} />
            </Form.Group>
            <Form.Group><Form.Label>Phụ cấp cố định/tháng (đồng)</Form.Label>
              <Form.Control type="number" min={0} step={1000} required value={form?.monthlyAllowance ?? ''} onChange={set('monthlyAllowance')} />
            </Form.Group>
          </Modal.Body>
          <Modal.Footer>
            <Button variant="light" onClick={() => setForm(null)}>Hủy</Button>
            <Button type="submit" disabled={saving}>{saving ? <Spinner size="sm" /> : 'Lưu cấu hình'}</Button>
          </Modal.Footer>
        </Form>
      </Modal>
    </div>
  )
}
