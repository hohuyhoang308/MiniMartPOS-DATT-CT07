import { Navigate, Route, Routes } from 'react-router-dom'
import PrivateRoute from './routes/PrivateRoute'
import Layout from './components/Layout'
import { useAuth } from './context/AuthContext'

import Login from './pages/auth/Login'
import Dashboard from './pages/dashboard/Dashboard'
import Pos from './pages/pos/Pos'
import Invoices from './pages/invoices/Invoices'
import Products from './pages/catalog/Products'
import Catalog from './pages/catalog/Catalog'
import Suppliers from './pages/catalog/Suppliers'
import GoodsReceipts from './pages/inventory/GoodsReceipts'
import Inventory from './pages/inventory/Inventory'
import AbcXyz from './pages/inventory/AbcXyz'
import Shelf from './pages/inventory/Shelf'
import ShelfManage from './pages/inventory/ShelfManage'
import Customers from './pages/crm/Customers'
import Promotions from './pages/crm/Promotions'
import Reports from './pages/reports/Reports'
import Shifts from './pages/shifts/Shifts'
import Users from './pages/admin/Users'
import Settings from './pages/admin/Settings'

/** Trang chủ theo vai trò: thu ngân → POS, còn lại → Dashboard. */
function RoleHome() {
  const { user } = useAuth()
  return <Navigate to={user?.role === 'CASHIER' ? '/pos' : '/dashboard'} replace />
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
          <Route path="shelf" element={<Shelf />} />
          <Route path="invoices" element={<Invoices />} />
          <Route path="customers" element={<Customers />} />

          {/* Admin + Manager */}
          <Route element={<PrivateRoute roles={['ADMIN', 'MANAGER']} />}>
            <Route path="dashboard" element={<Dashboard />} />
            <Route path="products" element={<Products />} />
            <Route path="catalog" element={<Catalog />} />
            <Route path="suppliers" element={<Suppliers />} />
            <Route path="receipts" element={<GoodsReceipts />} />
            <Route path="inventory" element={<Inventory />} />
            <Route path="abc-xyz" element={<AbcXyz />} />
            <Route path="shelves" element={<ShelfManage />} />
            <Route path="promotions" element={<Promotions />} />
            <Route path="reports" element={<Reports />} />
            <Route path="shifts" element={<Shifts />} />
          </Route>

          {/* Admin */}
          <Route element={<PrivateRoute roles={['ADMIN']} />}>
            <Route path="users" element={<Users />} />
            <Route path="settings" element={<Settings />} />
          </Route>
        </Route>
      </Route>

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
