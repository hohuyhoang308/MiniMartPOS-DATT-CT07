import { Navigate, Outlet } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

/**
 * Chặn route chưa đăng nhập; nếu truyền `roles` thì chặn cả vai trò không phù hợp
 * (đồng bộ với @PreAuthorize ở backend).
 */
export default function PrivateRoute({ roles }) {
  const { isAuthenticated, user, loading } = useAuth()

  if (loading) return null
  if (!isAuthenticated) return <Navigate to="/login" replace />
  // ADMIN luôn có mặt trong mọi danh sách roles cần thiết (phân tầng ADMIN>MANAGER>STAFF).
  if (roles && roles.length > 0 && !roles.includes(user.role)) {
    return <Navigate to="/" replace />
  }
  return <Outlet />
}
