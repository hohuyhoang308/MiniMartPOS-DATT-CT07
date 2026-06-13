import { Outlet, useLocation, useNavigate } from 'react-router-dom'
import { Dropdown } from 'react-bootstrap'
import Sidebar from './Sidebar'
import { findNav } from './navConfig'
import { useAuth } from '../context/AuthContext'
import { useTheme } from '../context/ThemeContext'

const ROLE_LABEL = { ADMIN: 'Quản trị viên', MANAGER: 'Quản lý cửa hàng', STAFF: 'Nhân viên' }
const ROLE_COLOR = { ADMIN: 'pill-violet', MANAGER: 'pill-info', STAFF: 'pill-success' }

/**
 * Phù hiệu PHẠM VI (đa cửa hàng) — chỉ hiển thị, KHÔNG phải bộ chọn:
 *  - ADMIN: "Toàn chuỗi" (quản trị mọi cửa hàng; cấu hình từng cửa hàng ở trang Cấu hình).
 *  - MANAGER/STAFF: tên cửa hàng trực thuộc (cố định theo tài khoản).
 */
function StoreBadge() {
  const { isAdmin, user } = useAuth()
  if (isAdmin) return <span className="pill pill-violet"><i className="bi bi-diagram-3-fill"></i>Toàn chuỗi</span>
  if (!user?.storeName) return null
  return <span className="pill pill-muted"><i className="bi bi-shop"></i>{user.storeName}</span>
}

export default function Layout() {
  const { user, logout } = useAuth()
  const { theme, toggleTheme } = useTheme()
  const navigate = useNavigate()
  const location = useLocation()
  const current = findNav(location.pathname)

  function handleLogout() {
    logout()
    navigate('/login', { replace: true })
  }

  const initials = (user?.fullName || '?')
    .split(' ').slice(-2).map((s) => s[0]).join('').toUpperCase()

  return (
    <div className="app-shell">
      <Sidebar />
      <div className="main-area">
        <header className="topbar">
          <div className="page-trail">
            {current?.label || 'Trang chủ'}
            <br />
            <small>Hệ thống POS cửa hàng tiện lợi</small>
          </div>
          <div className="d-flex align-items-center gap-3">
            <StoreBadge />
            <button type="button" className="theme-toggle" onClick={toggleTheme}
              title={theme === 'dark' ? 'Chuyển sang giao diện Sáng' : 'Chuyển sang giao diện Tối'}>
              <i className={`bi ${theme === 'dark' ? 'bi-sun-fill' : 'bi-moon-stars-fill'}`}></i>
            </button>
            <span className={`pill ${ROLE_COLOR[user?.role] || 'pill-muted'}`}>
              <i className="bi bi-person-fill"></i>{ROLE_LABEL[user?.role] || user?.role}
            </span>
            <Dropdown align="end">
              <Dropdown.Toggle as="div" className="d-flex align-items-center gap-2 cursor-pointer" bsPrefix=" ">
                <div className="rounded-circle d-flex align-items-center justify-content-center fw-bold text-white"
                     style={{ width: 38, height: 38, background: 'linear-gradient(135deg,var(--brand-400),var(--brand-700))', fontSize: '.85rem' }}>
                  {initials}
                </div>
                <div className="d-none d-md-block lh-sm">
                  <div className="fw-semibold" style={{ fontSize: '.88rem' }}>{user?.fullName}</div>
                  <div className="text-muted2" style={{ fontSize: '.74rem' }}>@{user?.username}</div>
                </div>
                <i className="bi bi-chevron-down text-muted2 small"></i>
              </Dropdown.Toggle>
              <Dropdown.Menu>
                <Dropdown.Item onClick={handleLogout}>
                  <i className="bi bi-box-arrow-right me-2"></i>Đăng xuất
                </Dropdown.Item>
              </Dropdown.Menu>
            </Dropdown>
          </div>
        </header>
        <main className="content"><Outlet /></main>
      </div>
    </div>
  )
}
