import { Navigate, Route, Routes } from 'react-router-dom'
import PrivateRoute from './routes/PrivateRoute'
import Layout from './components/Layout'
import StoreScopeRequired from './components/StoreScopeRequired'
import { useAuth } from './context/AuthContext'

import Login from './pages/auth/Login'
import Dashboard from './pages/dashboard/Dashboard'
import ChainOverview from './pages/dashboard/ChainOverview'
import Pos from './pages/pos/Pos'
import Invoices from './pages/invoices/Invoices'
import CatalogHub from './pages/catalog/CatalogHub'
import WarehouseHub from './pages/inventory/WarehouseHub'
import Shelf from './pages/inventory/Shelf'
import Customers from './pages/crm/Customers'
import Reports from './pages/reports/Reports'
import Shifts from './pages/shifts/Shifts'
import Users from './pages/admin/Users'
import Settings from './pages/admin/Settings'
import Audit from './pages/admin/Audit'
import Stores from './pages/admin/Stores'

/** Trang chủ theo vai trò: thu ngân → POS, quản trị chuỗi → So sánh chi nhánh, quản lý → Tổng quan. */
function RoleHome() {
  const { user } = useAuth()
  const home = user?.role === 'STAFF' ? '/pos' : user?.role === 'ADMIN' ? '/chain' : '/dashboard'
  return <Navigate to={home} replace />
}

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />

      <Route element={<PrivateRoute />}>
        <Route element={<Layout />}>
          <Route index element={<RoleHome />} />

          {/* Mọi vai trò bán hàng */}
          <Route path="pos" element={<Pos />} />
          <Route path="shelf" element={<StoreScopeRequired><Shelf /></StoreScopeRequired>} />
          <Route path="invoices" element={<Invoices />} />
          <Route path="customers" element={<Customers />} />

          {/* Admin + Manager: vận hành cửa hàng */}
          <Route element={<PrivateRoute roles={['ADMIN', 'MANAGER']} />}>
            <Route path="dashboard" element={<Dashboard />} />
            <Route path="inventory" element={<StoreScopeRequired><WarehouseHub /></StoreScopeRequired>} />
            <Route path="reports" element={<Reports />} />
            <Route path="shifts" element={<Shifts />} />
            {/* Quản lý nhân viên (STAFF cửa hàng mình) & nhật ký thao tác — backend chốt phạm vi theo cửa hàng */}
            <Route path="users" element={<Users />} />
            <Route path="audit" element={<Audit />} />
          </Route>

          {/* Admin: quản trị danh mục cấp CHUỖI (backend ghi = hasRole('ADMIN')) — tránh "mở trang rồi 403". */}
          <Route element={<PrivateRoute roles={['ADMIN']} />}>
            <Route path="products" element={<CatalogHub />} />
            <Route path="settings" element={<Settings />} />
            {/* Quản trị viên toàn chuỗi (đa cửa hàng) */}
            <Route path="stores" element={<Stores />} />
            <Route path="chain" element={<ChainOverview />} />
          </Route>
        </Route>
      </Route>

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
