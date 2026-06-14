import { Outlet, useLocation, useNavigate } from 'react-router-dom'
import { Dropdown } from 'react-bootstrap'
import Sidebar from './Sidebar'
import { findNav } from './navConfig'
import { useAuth } from '../context/AuthContext'
import { useStoreScope } from '../context/StoreScopeContext'
import { useTheme } from '../context/ThemeContext'

const ROLE_LABEL = { ADMIN: 'Quản trị viên', MANAGER: 'Quản lý cửa hàng', STAFF: 'Nhân viên' }
const ROLE_COLOR = { ADMIN: 'pill-violet', MANAGER: 'pill-info', STAFF: 'pill-success' }

/**
 * Bộ chọn PHẠM VI (đa cửa hàng):
 *  - ADMIN: dropdown chọn "Toàn chuỗi" hoặc 1 chi nhánh để đi sâu xem số liệu/vận hành cửa hàng đó.
 *  - MANAGER/STAFF: phù hiệu tĩnh tên cửa hàng trực thuộc (cố định theo tài khoản).
 */
function StoreScopeSelector() {
  const { isAdmin, user } = useAuth()
  const { stores, scopeStoreId, setScopeStoreId, scopeStoreName } = useStoreScope()

  if (!isAdmin) {
    if (!user?.storeName) return null
    return <span className="pill pill-muted"><i className="bi bi-shop"></i>{user.storeName}</span>
  }

  return (
    <Dropdown align="end">
      <Dropdown.Toggle as="div" bsPrefix=" "
        className={`pill ${scopeStoreId ? 'pill-info' : 'pill-violet'} cursor-pointer`}
        title="Chọn chi nhánh để xem số liệu, hoặc Toàn chuỗi để gộp tất cả">
        <i className={`bi ${scopeStoreId ? 'bi-shop' : 'bi-diagram-3-fill'}`}></i>
        {scopeStoreId ? scopeStoreName || 'Chi nhánh' : 'Toàn chuỗi'}
        <i className="bi bi-chevron-down ms-1 small"></i>
      </Dropdown.Toggle>
      <Dropdown.Menu style={{ maxHeight: 320, overflowY: 'auto' }}>
        <Dropdown.Header>Phạm vi xem dữ liệu</Dropdown.Header>
        <Dropdown.Item active={!scopeStoreId} onClick={() => setScopeStoreId('')}>
          <i className="bi bi-diagram-3-fill me-2"></i>Toàn chuỗi (gộp tất cả)
        </Dropdown.Item>
        <Dropdown.Divider />
        {stores.map((s) => (
          <Dropdown.Item key={s.id} active={String(s.id) === String(scopeStoreId)}
            onClick={() => setScopeStoreId(String(s.id))}>
            <i className="bi bi-shop me-2"></i>{s.code} — {s.name}
            {s.status === 'INACTIVE' && <span className="pill pill-muted ms-2 small">Ngừng</span>}
          </Dropdown.Item>
        ))}
        {stores.length === 0 && <Dropdown.Item disabled>Chưa có chi nhánh</Dropdown.Item>}
      </Dropdown.Menu>
    </Dropdown>
  )
}

export default function Layout() {
  const { user, logout } = useAuth()
  const { scopeStoreId } = useStoreScope()
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
            <StoreScopeSelector />
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
        {/* Đổi chi nhánh → key đổi → toàn bộ trang remount & tải lại dữ liệu theo phạm vi mới. */}
        <main className="content" key={scopeStoreId || 'chain'}><Outlet /></main>
      </div>
    </div>
  )
}
