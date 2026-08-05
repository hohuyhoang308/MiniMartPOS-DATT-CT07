import { useState } from 'react'
import { Button, Form, InputGroup, Modal, Spinner } from 'react-bootstrap'
import { cashMovementApi } from '../../api/sales'
import { useToast } from '../../context/ToastContext'
import { errMsg } from '../../api/client'
import MoneyInput from '../../components/ui/MoneyInput'

/* ---------------- Thu/Chi quỹ tiền mặt ---------------- */
export default function CashMovementModal({ show, onHide, onRecorded }) {
  const toast = useToast()
  const [type, setType] = useState('OUT')
  const [amount, setAmount] = useState('')
  const [reason, setReason] = useState('')
  const [loading, setLoading] = useState(false)

  function close() { setAmount(''); setReason(''); setType('OUT'); onHide() }

  async function submit(e) {
    e.preventDefault()
    if (Number(amount || 0) <= 0) { toast.warning('Nhập số tiền lớn hơn 0'); return }
    if (!reason.trim()) { toast.warning('Nhập lý do thu/chi'); return }
    setLoading(true)
    try {
      await cashMovementApi.create({ type, amount: Number(amount), reason: reason.trim() })
      toast.success(type === 'IN' ? 'Đã ghi khoản THU vào quỹ' : 'Đã ghi khoản CHI khỏi quỹ')
      onRecorded?.()
      close()
    } catch (e) { toast.error(errMsg(e, 'Không ghi được thu/chi quỹ')) } finally { setLoading(false) }
  }

  return (
    <Modal show={show} onHide={close} centered>
      <Form onSubmit={submit}>
        <Modal.Header closeButton>
          <Modal.Title><i className="bi bi-cash-coin me-2"></i>Thu / Chi quỹ tiền mặt</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <div className="small text-muted2 mb-3">
            Ghi các khoản tiền mặt <b>ngoài bán hàng</b> (trả tiền nhà cung cấp, đổi tiền lẻ, rút bớt tiền...).
            Số này sẽ được tính vào <b>đối soát quỹ cuối ca</b>.
          </div>
          <div className="d-flex gap-2 mb-3">
            <button type="button" className={`btn flex-grow-1 ${type === 'OUT' ? 'btn-danger' : 'btn-light'}`} onClick={() => setType('OUT')}>
              <i className="bi bi-box-arrow-up me-1"></i>Chi ra (−)
            </button>
            <button type="button" className={`btn flex-grow-1 ${type === 'IN' ? 'btn-success' : 'btn-light'}`} onClick={() => setType('IN')}>
              <i className="bi bi-box-arrow-in-down me-1"></i>Thu vào (+)
            </button>
          </div>
          <Form.Label>Số tiền</Form.Label>
          <InputGroup className="mb-3">
            <MoneyInput autoFocus value={amount} onChange={setAmount} />
            <InputGroup.Text>đ</InputGroup.Text>
          </InputGroup>
          <Form.Label>Lý do</Form.Label>
          <Form.Control as="textarea" rows={2} value={reason} onChange={(e) => setReason(e.target.value)}
            placeholder="Vd: Trả tiền thùng nước ngọt cho NCC / Đổi tiền lẻ vào quỹ..." maxLength={255} />
        </Modal.Body>
        <Modal.Footer>
          <Button variant="light" onClick={close}>Hủy</Button>
          <Button type="submit" variant={type === 'IN' ? 'success' : 'danger'} disabled={loading}>
            {loading ? <Spinner size="sm" /> : (type === 'IN' ? 'Ghi khoản thu' : 'Ghi khoản chi')}
          </Button>
        </Modal.Footer>
      </Form>
    </Modal>
  )
}
