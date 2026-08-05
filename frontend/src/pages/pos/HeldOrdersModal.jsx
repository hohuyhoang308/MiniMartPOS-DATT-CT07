import { Button, Modal } from 'react-bootstrap'
import { formatMoney } from '../../utils/format'

/* ---------------- Đơn đang treo ---------------- */
export default function HeldOrdersModal({ show, onHide, held, onResume, onRemove, blocked }) {
  return (
    <Modal show={show} onHide={onHide} centered>
      <Modal.Header closeButton>
        <Modal.Title><i className="bi bi-pause-circle me-2"></i>Đơn đang treo</Modal.Title>
      </Modal.Header>
      <Modal.Body>
        {blocked && (
          <div className="alert alert-warning py-2 small mb-3">
            <i className="bi bi-exclamation-triangle me-1"></i>
            Giỏ hiện tại đang có hàng. Hãy thanh toán hoặc treo đơn hiện tại trước khi lấy lại đơn treo.
          </div>
        )}
        {held.length === 0 ? (
          <div className="text-center text-muted2 py-4">
            <i className="bi bi-inbox fs-3 d-block mb-1 opacity-50"></i>Chưa có đơn nào đang treo
          </div>
        ) : (
          <div className="d-flex flex-column gap-2">
            {held.map((h) => (
              <div key={h.id} className="soft-card d-flex align-items-center justify-content-between p-2">
                <div className="min-w-0">
                  <div className="fw-semibold text-truncate">{h.label}</div>
                  <small className="text-muted2">
                    {h.count} món · {formatMoney(h.subtotal)} ·{' '}
                    {new Date(h.savedAt).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })}
                  </small>
                </div>
                <div className="d-flex gap-1 flex-shrink-0">
                  <Button size="sm" variant="primary" onClick={() => onResume(h.id)} disabled={blocked}>
                    <i className="bi bi-arrow-up-circle me-1"></i>Lấy lại
                  </Button>
                  <Button size="sm" variant="outline-danger" onClick={() => onRemove(h.id)} title="Xóa đơn treo">
                    <i className="bi bi-trash"></i>
                  </Button>
                </div>
              </div>
            ))}
          </div>
        )}
      </Modal.Body>
    </Modal>
  )
}
