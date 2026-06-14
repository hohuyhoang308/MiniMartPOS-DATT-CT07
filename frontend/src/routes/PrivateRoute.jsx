import { Navigate, Outlet } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

/** Thứ bậc vai trò: ADMIN ⊇ MANAGER ⊇ STAFF. Số lớn hơn = quyền cao hơn. */
const ROLE_RANK = { STAFF: 1, MANAGER: 2, ADMIN: 3 }

/**
 * Chặn route chưa đăng nhập; nếu truyền `roles` thì chặn cả vai trò không đủ quyền
 * (đồng bộ với @PreAuthorize ở backend).
 *
 * <p>Phân cấp THỰC SỰ: một vai trò được vào nếu nó nằm trong `roles`, HOẶC thứ bậc của nó
 * cao hơn/bằng vai trò thấp nhất trong `roles`. Nhờ vậy ADMIN luôn vượt qua mọi cổng kể cả
 * khi danh sách chỉ ghi `['MANAGER']` — không còn phụ thuộc vào việc liệt kê 'ADMIN' thủ công.</p>
 */
export default function PrivateRoute({ roles }) {
  const { isAuthenticated, user, loading } = useAuth()

  if (loading) return null
  if (!isAuthenticated) return <Navigate to="/login" replace />
  if (roles && roles.length > 0) {
    const need = Math.min(...roles.map((r) => ROLE_RANK[r] ?? Infinity))
    const have = ROLE_RANK[user.role] ?? 0
    if (have < need) return <Navigate to="/" replace />
  }
  return <Outlet />
}
