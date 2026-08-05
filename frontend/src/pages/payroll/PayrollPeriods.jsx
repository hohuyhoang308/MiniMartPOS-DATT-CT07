import { useMemo, useState } from 'react'
import { Badge, Button, Card, Col, Form, Modal, Row, Spinner, Table } from 'react-bootstrap'
import InfoBanner from '../../components/ui/InfoBanner'
import StatCard from '../../components/ui/StatCard'
import StatusPill from '../../components/ui/StatusPill'
import EmptyState from '../../components/ui/EmptyState'
import ConfirmModal from '../../components/ui/ConfirmModal'
import { payrollApi } from '../../api/misc'
import { useToast } from '../../context/ToastContext'
import { useStoreScope } from '../../context/StoreScopeContext'
import { useAuth } from '../../context/AuthContext'
import { errMsg } from '../../api/client'
import { formatMoney, formatDateTime, formatHours as fmtH, currentMonth as thisMonth } from '../../utils/format'
import { downloadBlob, openBlob } from '../../utils/download'
import { useList } from '../../hooks/useList'

export default function PayrollPeriods() {
  const toast = useToast()
  const { isChainScope } = useStoreScope() // ADMIN: true = đang xem toàn chuỗi (chưa chọn chi nhánh)
  const { isAdmin } = useAuth()
  const { data: periods, loading, reload: load } = useList(payrollApi.periods)
  const [month, setMonth] = useState(thisMonth())
  const [busy, setBusy] = useState(false)
  const [detail, setDetail] = useState(null)       // kỳ đang xem chi tiết (kèm payslips)
  const [adjFor, setAdjFor] = useState(null)        // phiếu lương đang thêm thưởng/phạt
  const [adjForm, setAdjForm] = useState({ type: 'BONUS', amount: '', reason: '' })
  const [confirm, setConfirm] = useState(null)      // { kind:'lock'|'pay', period }

  // ADMIN toàn chuỗi phải chọn một chi nhánh cụ thể mới tính được lương (ghi cần X-Store-Id);
  // MANAGER luôn gắn cửa hàng nên không cần chọn.
  const needStore = isAdmin && isChainScope

  async function compute(targetMonth = month) {
    setBusy(true)
    try {
      const d = await payrollApi.compute(targetMonth)
      toast.success(`Đã tính lương kỳ ${targetMonth} (${d.employeeCount} nhân viên)`)
      setDetail(d); load()
    } catch (e) { toast.error(errMsg(e)) } finally { setBusy(false) }
  }
  async function openDetail(id) {
    try { setDetail(await payrollApi.period(id)) } catch (e) { toast.error(errMsg(e)) }
  }
  // Hành động chuyển trạng thái bảng lương (trình/duyệt/trả lại/chi): gọi API trên kỳ đang xác nhận,
  // báo thành công rồi làm tươi chi tiết + danh sách. Đóng hộp xác nhận dù thành công hay lỗi.
  async function runAction(fn, successMsg) {
    try { const d = await fn(confirm.period.id); toast.success(successMsg); setDetail(d); load() }
    catch (e) { toast.error(errMsg(e)) } finally { setConfirm(null) }
  }
  async function addAdjustment(e) {
    e.preventDefault(); setBusy(true)
    try {
      await payrollApi.addAdjustment(adjFor.id, {
        type: adjForm.type, amount: Number(adjForm.amount || 0), reason: adjForm.reason,
      })
      toast.success('Đã thêm điều chỉnh')
      const d = await payrollApi.period(detail.id)
      setDetail(d)
      setAdjFor(d.payslips.find((s) => s.id === adjFor.id) || null)
      setAdjForm({ type: 'BONUS', amount: '', reason: '' })
    } catch (e) { toast.error(errMsg(e)) } finally { setBusy(false) }
  }
  async function removeAdjustment(adjId) {
    try {
      await payrollApi.removeAdjustment(adjId)
      const d = await payrollApi.period(detail.id)
      setDetail(d)
      setAdjFor(d.payslips.find((s) => s.id === adjFor.id) || null)
    } catch (e) { toast.error(errMsg(e)) }
  }

  async function exportExcel() {
    try {
      downloadBlob(await payrollApi.exportPeriod(detail.id), `bang-luong-${detail.periodMonth}.xlsx`)
    } catch (e) { toast.error(errMsg(e)) }
  }
  async function printPayslip(id) {
    try {
      openBlob(await payrollApi.payslipPdf(id))
    } catch (e) { toast.error(errMsg(e)) }
  }

  const totalNet = useMemo(
    () => (detail?.payslips || []).reduce((s, p) => s + Number(p.netPay || 0), 0),
    [detail],
  )
  const isDraft = detail?.status === 'DRAFT'

  return (
    <div>
      <InfoBanner id="payroll-periods" title="Cách tính lương theo kỳ">
        Chọn <b>tháng</b> rồi bấm <b>Tính lương</b>: hệ thống cộng <b>giờ công</b> (từ <b>ca đã đóng</b> + <b>bảng công</b>)
        của từng nhân viên, nhân <b>đơn giá lương</b>, cộng <b>tăng ca</b> và <b>phụ cấp</b>; có thể thêm <b>thưởng/phạt</b>.
        Quy trình duyệt 2 bước: người lập bấm <b>Trình duyệt</b> → <b>quản trị viên Duyệt</b> (hoặc Trả lại) → <b>Chi lương</b>.
        Khi duyệt/chi, hệ thống tự <b>thông báo Telegram</b> tới chi nhánh. Kỳ đã trình duyệt không tính lại được tới khi trả về bản nháp.
      </InfoBanner>

      <Card className="border-0 mb-3">
        <Card.Body className="d-flex flex-wrap align-items-end gap-2">
          <Form.Group>
            <Form.Label className="small text-muted2 mb-1">Tháng lương</Form.Label>
            <Form.Control type="month" value={month} onChange={(e) => setMonth(e.target.value)} style={{ width: 180 }} />
          </Form.Group>
          <Button onClick={() => compute()} disabled={busy || needStore}>
            {busy ? <Spinner size="sm" /> : <><i className="bi bi-calculator me-1"></i>Tính lương</>}
          </Button>
          {needStore && <span className="text-warning small align-self-center">
            <i className="bi bi-exclamation-triangle me-1"></i>Hãy chọn một chi nhánh cụ thể (góc trên) để tính lương.
          </span>}
          <Button variant="soft" className="ms-auto" onClick={load}><i className="bi bi-arrow-clockwise me-1"></i>Làm mới</Button>
        </Card.Body>
      </Card>

      <Card className="border-0 mb-3">
        <Card.Body className="pb-0"><Card.Title className="fs-6 mb-0">Các kỳ lương</Card.Title></Card.Body>
        <div className="table-responsive">
          <Table hover className="mb-0 align-middle">
            <thead><tr>
              <th>Tháng</th><th>Chi nhánh</th><th className="text-center">Số nhân viên</th>
              <th className="text-end">Tổng thực lĩnh</th><th className="text-center">Trạng thái</th>
              <th>Tạo lúc</th><th></th>
            </tr></thead>
            <tbody>
              {loading ? <tr><td colSpan={7} className="text-center py-4"><Spinner size="sm" /></td></tr> : (<>
                {periods.map((p) => (
                  <tr key={p.id} className={detail?.id === p.id ? 'table-active' : ''} style={{ cursor: 'pointer' }} onClick={() => openDetail(p.id)}>
                    <td className="fw-semibold">{p.periodMonth}</td>
                    <td className="text-muted2 small">{p.storeName}</td>
                    <td className="text-center">{p.employeeCount}</td>
                    <td className="text-end num fw-semibold">{formatMoney(p.totalNet)}</td>
                    <td className="text-center"><StatusPill value={p.status} /></td>
                    <td className="text-muted2 small">{formatDateTime(p.createdAt)}</td>
                    <td className="text-end"><i className="bi bi-chevron-right text-muted2"></i></td>
                  </tr>
                ))}
                {periods.length === 0 && <tr><td colSpan={7}><EmptyState icon="bi-cash-coin" title="Chưa có kỳ lương nào" hint="Chọn tháng và bấm Tính lương để tạo kỳ đầu tiên." /></td></tr>}
              </>)}
            </tbody>
          </Table>
        </div>
      </Card>

      {detail && (
        <Card className="border-0 fade-up">
          <Card.Body className="d-flex flex-wrap justify-content-between align-items-center gap-2">
            <div>
              <Card.Title className="fs-6 mb-1">
                Phiếu lương kỳ {detail.periodMonth} — {detail.storeName} <StatusPill value={detail.status} />
              </Card.Title>
              <span className="text-muted2 small">
                {detail.employeeCount} nhân viên · tổng thực lĩnh <b>{formatMoney(totalNet)}</b>
                {detail.submittedAt && <> · trình {formatDateTime(detail.submittedAt)}</>}
                {detail.approvedAt && <> · duyệt {formatDateTime(detail.approvedAt)}</>}
                {detail.paidAt && <> · chi {formatDateTime(detail.paidAt)}</>}
              </span>
            </div>
            <div className="d-flex gap-2">
              <Button size="sm" variant="soft" onClick={exportExcel}><i className="bi bi-file-earmark-excel me-1"></i>Xuất Excel</Button>
              {isDraft && <Button size="sm" onClick={() => compute(detail.periodMonth)} disabled={busy}><i className="bi bi-arrow-repeat me-1"></i>Tính lại</Button>}
              {isDraft && <Button size="sm" variant="warning" onClick={() => setConfirm({ kind: 'submit', period: detail })}><i className="bi bi-send me-1"></i>Trình duyệt</Button>}
              {detail.status === 'PENDING_APPROVAL' && isAdmin && <>
                <Button size="sm" variant="success" onClick={() => setConfirm({ kind: 'approve', period: detail })}><i className="bi bi-patch-check me-1"></i>Duyệt</Button>
                <Button size="sm" variant="light" className="text-danger" onClick={() => setConfirm({ kind: 'reject', period: detail })}><i className="bi bi-arrow-counterclockwise me-1"></i>Trả lại</Button>
              </>}
              {detail.status === 'PENDING_APPROVAL' && !isAdmin && <span className="pill pill-info align-self-center"><i className="bi bi-hourglass-split"></i>Chờ quản trị viên duyệt</span>}
              {detail.status === 'APPROVED' && <Button size="sm" variant="success" onClick={() => setConfirm({ kind: 'pay', period: detail })}><i className="bi bi-cash-stack me-1"></i>Chi lương</Button>}
              <Button size="sm" variant="light" onClick={() => setDetail(null)}><i className="bi bi-x-lg"></i></Button>
            </div>
          </Card.Body>

          {/* Cảnh báo bảng công (chỉ kỳ nháp): ca dài bất thường, chấm công trùng ngày có ca */}
          {(detail.warnings?.length > 0) && (
            <div className="alert alert-warning mx-3 mb-3 py-2 small">
              <div className="fw-semibold mb-1"><i className="bi bi-exclamation-triangle me-1"></i>
                Rà soát bảng công trước khi trình duyệt:</div>
              <ul className="mb-0 ps-3">
                {detail.warnings.map((w, i) => <li key={i}>{w}</li>)}
              </ul>
            </div>
          )}

          <Row className="g-3 px-3 pb-3">
            <Col md={3}><StatCard icon="bi-people-fill" chip="sky" label="Nhân viên" value={detail.employeeCount} /></Col>
            <Col md={3}><StatCard icon="bi-clock-history" chip="violet" label="Tổng giờ công"
              value={fmtH((detail.payslips || []).reduce((s, p) => s + Number(p.workedHours || 0), 0))} /></Col>
            <Col md={3}><StatCard icon="bi-cash-stack" chip="amber" label="Tổng lương gộp"
              value={formatMoney((detail.payslips || []).reduce((s, p) => s + Number(p.grossPay || 0), 0))} /></Col>
            <Col md={3}><StatCard icon="bi-wallet2" chip="emerald" label="Tổng thực lĩnh" value={formatMoney(totalNet)} /></Col>
          </Row>

          <div className="table-responsive">
            <Table hover className="mb-0 align-middle">
              <thead><tr>
                <th>Nhân viên</th><th className="text-center">Số ca</th><th className="text-end">Giờ công</th>
                <th className="text-end">Giờ thường</th><th className="text-end">Tăng ca</th>
                <th className="text-end">Lương gốc</th><th className="text-end">Tiền tăng ca</th>
                <th className="text-end">Phụ cấp</th><th className="text-end">Thưởng</th><th className="text-end">Phạt</th>
                <th className="text-end">Thực lĩnh</th><th></th>
              </tr></thead>
              <tbody>
                {(detail.payslips || []).map((p) => (
                  <tr key={p.id}>
                    <td className="fw-semibold">{p.fullName}
                      <div className="text-muted2 small">{p.payType === 'HOURLY' ? `${formatMoney(p.baseRate)}/giờ` : `${formatMoney(p.baseRate)}/tháng`}</div></td>
                    <td className="text-center">{p.shiftCount}</td>
                    <td className="text-end num">{fmtH(p.workedHours)}</td>
                    <td className="text-end num text-muted2">{fmtH(p.regularHours)}</td>
                    <td className="text-end num">{Number(p.otHours) > 0 ? <Badge bg="warning" text="dark">{fmtH(p.otHours)}</Badge> : <span className="text-muted2">—</span>}</td>
                    <td className="text-end num">{formatMoney(p.regularPay)}</td>
                    <td className="text-end num">{formatMoney(p.otPay)}</td>
                    <td className="text-end num text-muted2">{formatMoney(p.allowance)}</td>
                    <td className="text-end num text-success">{Number(p.totalBonus) > 0 ? `+${formatMoney(p.totalBonus)}` : '—'}</td>
                    <td className="text-end num text-danger">{Number(p.totalDeduction) > 0 ? `−${formatMoney(p.totalDeduction)}` : '—'}</td>
                    <td className="text-end num fw-bold">{formatMoney(p.netPay)}</td>
                    <td className="text-end text-nowrap">
                      <Button size="sm" variant="light" className="me-1" title="In phiếu lương (PDF)" onClick={() => printPayslip(p.id)}>
                        <i className="bi bi-printer"></i>
                      </Button>
                      <Button size="sm" variant="light" title="Thưởng / phạt"
                        onClick={() => { setAdjFor(p); setAdjForm({ type: 'BONUS', amount: '', reason: '' }) }}>
                        <i className="bi bi-plus-slash-minus"></i>
                        {(p.adjustments?.length > 0) && <span className="ms-1 small">{p.adjustments.length}</span>}
                      </Button>
                    </td>
                  </tr>
                ))}
                {(detail.payslips || []).length === 0 && <tr><td colSpan={12}><EmptyState icon="bi-receipt" title="Kỳ này chưa có phiếu lương" hint="Nhân viên cần có ca đã đóng trong tháng." /></td></tr>}
              </tbody>
            </Table>
          </div>
        </Card>
      )}

      {/* Thưởng / phạt cho một phiếu */}
      <Modal show={!!adjFor} onHide={() => setAdjFor(null)} centered>
        <Modal.Header closeButton><Modal.Title>Thưởng / phạt — {adjFor?.fullName}</Modal.Title></Modal.Header>
        <Modal.Body>
          {(adjFor?.adjustments?.length > 0) && (
            <Table size="sm" className="mb-3">
              <thead><tr><th>Loại</th><th className="text-end">Số tiền</th><th>Lý do</th><th></th></tr></thead>
              <tbody>
                {adjFor.adjustments.map((a) => (
                  <tr key={a.id}>
                    <td>{a.type === 'BONUS' ? <span className="text-success">Thưởng</span> : <span className="text-danger">Phạt</span>}</td>
                    <td className="text-end num">{formatMoney(a.amount)}</td>
                    <td className="small">{a.reason}</td>
                    <td className="text-end">{isDraft && <Button size="sm" variant="link" className="text-danger p-0" onClick={() => removeAdjustment(a.id)}><i className="bi bi-trash"></i></Button>}</td>
                  </tr>
                ))}
              </tbody>
            </Table>
          )}
          {isDraft ? (
            <Form onSubmit={addAdjustment}>
              <Row className="g-2">
                <Col xs={5}><Form.Select value={adjForm.type} onChange={(e) => setAdjForm({ ...adjForm, type: e.target.value })}>
                  <option value="BONUS">Thưởng (+)</option>
                  <option value="DEDUCTION">Phạt / tạm ứng (−)</option>
                </Form.Select></Col>
                <Col xs={7}><Form.Control type="number" min={1000} step={1000} required placeholder="Số tiền (đồng)" value={adjForm.amount} onChange={(e) => setAdjForm({ ...adjForm, amount: e.target.value })} /></Col>
                <Col xs={12}><Form.Control required placeholder="Lý do (vd: Thưởng doanh số, Tạm ứng…)" value={adjForm.reason} onChange={(e) => setAdjForm({ ...adjForm, reason: e.target.value })} /></Col>
                <Col xs={12} className="text-end"><Button type="submit" size="sm" disabled={busy}>{busy ? <Spinner size="sm" /> : 'Thêm'}</Button></Col>
              </Row>
            </Form>
          ) : <p className="text-muted2 small mb-0">Kỳ lương đã khóa — không thể chỉnh sửa thưởng/phạt.</p>}
        </Modal.Body>
      </Modal>

      <ConfirmModal show={confirm?.kind === 'submit'} onHide={() => setConfirm(null)} onConfirm={() => runAction(payrollApi.submit, 'Đã trình duyệt bảng lương')}
        title="Trình duyệt bảng lương" message={`Gửi bảng lương kỳ ${confirm?.period?.periodMonth} cho quản trị viên duyệt? Sau khi trình, số liệu đóng băng và không sửa được cho tới khi được duyệt hoặc trả lại.`}
        confirmText="Trình duyệt" icon="bi-send" variant="warning" />
      <ConfirmModal show={confirm?.kind === 'approve'} onHide={() => setConfirm(null)} onConfirm={() => runAction(payrollApi.approve, 'Đã duyệt bảng lương')}
        title="Duyệt bảng lương" message={`Duyệt (chốt) bảng lương kỳ ${confirm?.period?.periodMonth}? Hệ thống sẽ gửi thông báo tới chi nhánh.`}
        confirmText="Duyệt" icon="bi-patch-check" variant="success" />
      <ConfirmModal show={confirm?.kind === 'reject'} onHide={() => setConfirm(null)} onConfirm={() => runAction((id) => payrollApi.reject(id, 'Trả lại để chỉnh sửa'), 'Đã trả lại bản nháp')}
        title="Trả lại bản nháp" message={`Trả bảng lương kỳ ${confirm?.period?.periodMonth} về bản nháp để chỉnh sửa lại?`}
        confirmText="Trả lại" icon="bi-arrow-counterclockwise" />
      <ConfirmModal show={confirm?.kind === 'pay'} onHide={() => setConfirm(null)} onConfirm={() => runAction(payrollApi.pay, 'Đã ghi nhận chi lương')}
        title="Xác nhận chi lương" message={`Ghi nhận đã CHI LƯƠNG cho kỳ ${confirm?.period?.periodMonth}? Hệ thống sẽ gửi thông báo tới chi nhánh.`}
        confirmText="Đã chi lương" icon="bi-cash-stack" variant="success" />
    </div>
  )
}
