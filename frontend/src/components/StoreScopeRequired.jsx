import { useAuth } from '../context/AuthContext'
import { useStoreScope } from '../context/StoreScopeContext'

/**
 * Chặn hiển thị trang CHỈ-THEO-CHI-NHÁNH khi ADMIN đang ở phạm vi "Toàn chuỗi".
 *
 * <p>Một số trang vận hành (Tồn kho, ABC/XYZ…) chỉ có nghĩa khi gắn với MỘT cửa hàng cụ thể nên backend
 * bắt buộc chọn chi nhánh. Thay vì để API báo lỗi, ở đây hiện lời nhắc thân thiện để ADMIN chọn chi nhánh
 * ở phù hiệu phạm vi (góc trên bên phải). Khi đã chọn, trang con mới được mount và gọi API.</p>
 *
 * <p>MANAGER/STAFF gắn cố định cửa hàng nên luôn đi thẳng vào nội dung.</p>
 */
export default function StoreScopeRequired({ children }) {
  const { isAdmin } = useAuth()
  const { isChainScope } = useStoreScope()

  if (isAdmin && isChainScope) {
    return (
      <div className="d-flex flex-column align-items-center justify-content-center text-center py-5 my-5">
        <i className="bi bi-shop" style={{ fontSize: '3rem', color: 'var(--brand-500)' }}></i>
        <h5 className="mt-3 mb-1">Hãy chọn một chi nhánh</h5>
        <p className="text-muted2" style={{ maxWidth: 460 }}>
          Trang này hiển thị dữ liệu theo từng cửa hàng. Bấm phù hiệu <b>“Toàn chuỗi”</b> ở góc trên bên phải
          rồi chọn một chi nhánh để xem.
        </p>
      </div>
    )
  }
  return children
}
