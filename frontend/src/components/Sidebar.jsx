import { NavLink } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { NAV_SECTIONS } from './navConfig'

export default function Sidebar() {
  const { user } = useAuth()

  return (
    <aside className="sidebar">
      <div className="brand">
        <span className="logo-chip"><i className="bi bi-shop-window"></i></span>
        <span>MiniMart<span style={{ color: 'var(--brand-400)' }}>POS</span></span>
      </div>
      <nav>
        {NAV_SECTIONS.map((section) => {
          const items = section.items.filter((m) => m.roles.includes(user?.role))
          if (items.length === 0) return null
          return (
            <div key={section.title}>
              <div className="nav-section">{section.title}</div>
              {items.map((m) => (
                <NavLink key={m.to} to={m.to} end={m.to === '/dashboard'}>
                  <i className={`bi ${m.icon}`}></i>
                  <span>{m.label}</span>
                </NavLink>
              ))}
            </div>
          )
        })}
      </nav>
      <div className="side-foot">
        <i className="bi bi-c-circle me-1"></i>2026 · Đồ án POS · v1.0
      </div>
    </aside>
  )
}
