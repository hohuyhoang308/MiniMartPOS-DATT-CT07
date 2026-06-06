import { useEffect, useState } from 'react'
import { Button, Card, Col, Form, Row, Spinner, Table } from 'react-bootstrap'
import PageHeader from '../../components/ui/PageHeader'
import InfoBanner from '../../components/ui/InfoBanner'
import Loading from '../../components/ui/Loading'
import { storeConfigApi, integrationApi } from '../../api/misc'
import { useToast } from '../../context/ToastContext'
import { errMsg } from '../../api/client'

function SectionCard({ icon, title, desc, children, chip = 'emerald' }) {
  return (
    <Card className="border-0 mb-3">
      <Card.Body>
        <div className="d-flex align-items-center gap-3 mb-3">
          <span className={`stat-chip chip-${chip}`} style={{ width: 44, height: 44, fontSize: '1.1rem' }}><i className={`bi ${icon}`}></i></span>
          <div><div className="fw-bold">{title}</div><div className="small text-muted2">{desc}</div></div>
        </div>
        {children}
      </Card.Body>
    </Card>
  )
}

export default function Settings() {
  const toast = useToast()
  const [cfg, setCfg] = useState(null)
  const [recipients, setRecipients] = useState([])
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [testChat, setTestChat] = useState('')
  const [newRcp, setNewRcp] = useState({ chatId: '', label: '' })
  const [busy, setBusy] = useState('')

  async function loadAll() {
    setLoading(true)
    try {
      const c = await storeConfigApi.get()
      setCfg({ ...c, web2mApiUrl: '', telegramBotToken: '' })
      setRecipients(await integrationApi.recipients())
    } catch (e) { toast.error(errMsg(e)) } finally { setLoading(false) }
  }
  useEffect(() => { loadAll() }, [])

  const set = (k) => (e) => setCfg({ ...cfg, [k]: e.target.type === 'checkbox' ? e.target.checked : e.target.value })

  async function save() {
    setSaving(true)
    try { await storeConfigApi.update(cfg); toast.success('Đã lưu cấu hình'); loadAll() }
    catch (e) { toast.error(errMsg(e)) } finally { setSaving(false) }
  }
  async function testWeb2m() {
    setBusy('web2m')
    try { const r = await integrationApi.web2mTest(cfg.web2mApiUrl || undefined); r.connected ? toast.success('Kết nối WEB2M thành công') : toast.error('Chưa kết nối được WEB2M') }
    catch (e) { toast.error(errMsg(e)) } finally { setBusy('') }
  }
  async function testTelegram() {
    if (!testChat.trim()) { toast.warning('Nhập Chat ID để gửi thử'); return }
    setBusy('tele')
    try { const r = await integrationApi.telegramTest(testChat.trim(), '🔔 Tin nhắn thử từ POS'); r.sent ? toast.success('Đã gửi tin nhắn thử') : toast.error('Gửi không thành công. Hãy kiểm tra lại Bot Token và Chat ID') }
    catch (e) { toast.error(errMsg(e)) } finally { setBusy('') }
  }
  async function addRecipient() {
    if (!newRcp.chatId.trim()) { toast.warning('Nhập Chat ID'); return }
    try { await integrationApi.addRecipient(newRcp); toast.success('Đã thêm người nhận'); setNewRcp({ chatId: '', label: '' }); setRecipients(await integrationApi.recipients()) }
    catch (e) { toast.error(errMsg(e)) }
  }
  async function removeRecipient(id) {
    try { await integrationApi.removeRecipient(id); setRecipients(await integrationApi.recipients()) }
    catch (e) { toast.error(errMsg(e)) }
  }

  if (loading) return <Loading />

  return (
    <div>
      <PageHeader title="Cấu hình cửa hàng" subtitle="Thông tin in trên hóa đơn, tài khoản ngân hàng và các kết nối">
        <Button onClick={save} disabled={saving}>{saving ? <Spinner size="sm" /> : <><i className="bi bi-check-lg me-1"></i>Lưu cấu hình</>}</Button>
      </PageHeader>

      <InfoBanner id="settings" title="Trang này dùng để làm gì?">
        <b>Thông tin cửa hàng</b> sẽ in lên hóa đơn. <b>Ngân hàng / VietQR</b> dùng để tạo mã QR cho khách quét chuyển khoản.
        <b> WEB2M</b> giúp máy tự kiểm tra xem tiền chuyển khoản đã về tài khoản chưa. <b>Telegram</b> dùng để gửi thông báo
        cho bạn (khi có tiền về, khi hàng sắp hết…). Nhập đầy đủ thông tin rồi bấm <b>Lưu cấu hình</b>. Có nút để kiểm tra kết nối và gửi thử.
      </InfoBanner>

      <Row>
        <Col lg={6}>
          <SectionCard icon="bi-shop" title="Thông tin cửa hàng" desc="Sẽ in lên hóa đơn cho khách" chip="emerald">
            <Form.Group className="mb-3"><Form.Label>Tên cửa hàng *</Form.Label><Form.Control value={cfg.name || ''} onChange={set('name')} /></Form.Group>
            <Form.Group className="mb-3"><Form.Label>Địa chỉ</Form.Label><Form.Control value={cfg.address || ''} onChange={set('address')} /></Form.Group>
            <Row>
              <Col><Form.Group className="mb-3"><Form.Label>Điện thoại</Form.Label><Form.Control value={cfg.phone || ''} onChange={set('phone')} /></Form.Group></Col>
              <Col><Form.Group className="mb-3"><Form.Label>Mã số thuế</Form.Label><Form.Control value={cfg.taxCode || ''} onChange={set('taxCode')} /></Form.Group></Col>
            </Row>
            <Form.Group><Form.Label>Logo (URL)</Form.Label><Form.Control value={cfg.logoUrl || ''} onChange={set('logoUrl')} /></Form.Group>
          </SectionCard>

          <SectionCard icon="bi-bank" title="Ngân hàng / VietQR" desc="Tạo mã QR cho khách quét chuyển khoản" chip="sky">
            <Row>
              <Col md={6}><Form.Group className="mb-3"><Form.Label>Tên ngân hàng</Form.Label><Form.Control value={cfg.bankName || ''} onChange={set('bankName')} placeholder="MB Bank" /></Form.Group></Col>
              <Col md={6}><Form.Group className="mb-3"><Form.Label>Mã BIN</Form.Label><Form.Control value={cfg.bankBin || ''} onChange={set('bankBin')} placeholder="970422" /></Form.Group></Col>
            </Row>
            <Form.Group className="mb-3"><Form.Label>Số tài khoản</Form.Label><Form.Control value={cfg.bankAccountNo || ''} onChange={set('bankAccountNo')} /></Form.Group>
            <Row>
              <Col md={7}><Form.Group className="mb-3"><Form.Label>Chủ tài khoản</Form.Label><Form.Control value={cfg.bankAccountName || ''} onChange={set('bankAccountName')} /></Form.Group></Col>
              <Col md={5}><Form.Group className="mb-3"><Form.Label>Mã ghi trong nội dung chuyển khoản</Form.Label><Form.Control value={cfg.transferPrefix || ''} onChange={set('transferPrefix')} placeholder="POS" /></Form.Group></Col>
            </Row>
          </SectionCard>
        </Col>

        <Col lg={6}>
          <SectionCard icon="bi-arrow-repeat" title="WEB2M — tự kiểm tra tiền về" desc="Máy tự kiểm tra xem khách đã chuyển khoản chưa" chip="violet">
            <Form.Group className="mb-2">
              <Form.Label>Đường dẫn API WEB2M {cfg.web2mConfigured && <span className="pill pill-success ms-1"><i className="bi bi-check-circle"></i>Đã thiết lập</span>}</Form.Label>
              <Form.Control type="password" value={cfg.web2mApiUrl} onChange={set('web2mApiUrl')} placeholder={cfg.web2mConfigured ? '•••• (để trống nếu giữ nguyên)' : 'https://api.web2m.com/...'} />
            </Form.Group>
            <Button size="sm" variant="light" onClick={testWeb2m} disabled={busy === 'web2m'}>
              {busy === 'web2m' ? <Spinner size="sm" /> : <><i className="bi bi-wifi me-1"></i>Kiểm tra kết nối</>}
            </Button>
          </SectionCard>

          <SectionCard icon="bi-telegram" title="Thông báo qua Telegram" desc="Báo cho bạn khi có tiền về, khi hàng sắp hết…" chip="sky">
            <Form.Group className="mb-3">
              <Form.Label>Mã Bot Token {cfg.telegramConfigured && <span className="pill pill-success ms-1"><i className="bi bi-check-circle"></i>Đã thiết lập</span>}</Form.Label>
              <Form.Control type="password" value={cfg.telegramBotToken} onChange={set('telegramBotToken')} placeholder={cfg.telegramConfigured ? '•••• (để trống nếu giữ nguyên)' : 'Bot token'} />
            </Form.Group>
            <div className="d-flex flex-wrap gap-3 mb-3">
              <Form.Check type="switch" label="Bật thông báo" checked={!!cfg.telegramEnabled} onChange={set('telegramEnabled')} />
              <Form.Check type="switch" label="Báo khi có tiền về" checked={!!cfg.notifyPayment} onChange={set('notifyPayment')} />
              <Form.Check type="switch" label="Báo khi hàng sắp hết" checked={!!cfg.notifyLowStock} onChange={set('notifyLowStock')} />
              <Form.Check type="switch" label="Báo khi có hóa đơn mới" checked={!!cfg.notifyNewInvoice} onChange={set('notifyNewInvoice')} />
            </div>

            <div className="soft-card p-2 mb-3">
              <Table size="sm" className="mb-2">
                <tbody>
                  {recipients.map((r) => (
                    <tr key={r.id}>
                      <td className="fw-semibold">{r.chatId}</td>
                      <td className="text-muted2">{r.label || '—'}</td>
                      <td className="text-end"><Button size="sm" variant="light" className="text-danger" onClick={() => removeRecipient(r.id)}><i className="bi bi-trash"></i></Button></td>
                    </tr>
                  ))}
                  {recipients.length === 0 && <tr><td className="text-muted2 small text-center py-2" colSpan={3}>Chưa có người nhận thông báo</td></tr>}
                </tbody>
              </Table>
              <div className="d-flex gap-2">
                <Form.Control size="sm" placeholder="Chat ID" value={newRcp.chatId} onChange={(e) => setNewRcp({ ...newRcp, chatId: e.target.value })} />
                <Form.Control size="sm" placeholder="Ghi chú" value={newRcp.label} onChange={(e) => setNewRcp({ ...newRcp, label: e.target.value })} />
                <Button size="sm" variant="soft" onClick={addRecipient}><i className="bi bi-plus-lg"></i></Button>
              </div>
            </div>

            <div className="d-flex gap-2">
              <Form.Control size="sm" placeholder="Chat ID gửi thử" value={testChat} onChange={(e) => setTestChat(e.target.value)} />
              <Button size="sm" variant="light" onClick={testTelegram} disabled={busy === 'tele'}>
                {busy === 'tele' ? <Spinner size="sm" /> : <><i className="bi bi-send me-1"></i>Gửi thử</>}
              </Button>
            </div>
          </SectionCard>
        </Col>
      </Row>
    </div>
  )
}
