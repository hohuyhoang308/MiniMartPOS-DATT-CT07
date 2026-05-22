import { Button, Modal, Spinner } from 'react-bootstrap'

/** Hộp xác nhận hành động (xóa/hủy…). show, onHide, onConfirm. */
export default function ConfirmModal({
  show, onHide, onConfirm, loading,
  title = 'Xác nhận', message = 'Bạn có chắc chắn?',
  confirmText = 'Đồng ý', variant = 'danger', icon = 'bi-exclamation-triangle',
}) {
  return (
    <Modal show={show} onHide={onHide} centered size="sm">
      <Modal.Body className="text-center p-4">
        <div className={`mb-3 text-${variant}`}><i className={`bi ${icon}`} style={{ fontSize: '2.4rem' }}></i></div>
        <h6 className="fw-bold mb-1">{title}</h6>
        <p className="text-muted2 small mb-4">{message}</p>
        <div className="d-flex gap-2">
          <Button variant="light" className="flex-grow-1" onClick={onHide} disabled={loading}>Hủy</Button>
          <Button variant={variant} className="flex-grow-1" onClick={onConfirm} disabled={loading}>
            {loading ? <Spinner size="sm" /> : confirmText}
          </Button>
        </div>
      </Modal.Body>
    </Modal>
  )
}
