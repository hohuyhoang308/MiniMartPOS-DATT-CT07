import { useState } from 'react'
import { Tab, Tabs } from 'react-bootstrap'
import PageHeader from '../../components/ui/PageHeader'
import Products from './Products'
import Catalog from './Catalog'
import Promotions from '../crm/Promotions'
import Suppliers from './Suppliers'

/**
 * Trung tâm HÀNG HÓA & DANH MỤC — gom các trang quản lý ở cấp toàn chuỗi (sản phẩm, danh mục/đơn vị,
 * khuyến mãi, nhà cung cấp) về MỘT trang nhiều tab. Mỗi tab tự tải dữ liệu khi mở (mountOnEnter/unmountOnExit).
 */
export default function CatalogHub() {
  const [tab, setTab] = useState('products')
  return (
    <div>
      <PageHeader title="Hàng hóa & danh mục" subtitle="Sản phẩm, danh mục, đơn vị, khuyến mãi và nhà cung cấp — cấp toàn chuỗi" />
      <Tabs activeKey={tab} onSelect={setTab} mountOnEnter unmountOnExit className="mb-3">
        <Tab eventKey="products" title={<span><i className="bi bi-box-seam-fill me-1"></i>Sản phẩm</span>}>
          <Products />
        </Tab>
        <Tab eventKey="catalog" title={<span><i className="bi bi-tags-fill me-1"></i>Danh mục & Đơn vị</span>}>
          <Catalog />
        </Tab>
        <Tab eventKey="promotions" title={<span><i className="bi bi-percent me-1"></i>Khuyến mãi</span>}>
          <Promotions />
        </Tab>
        <Tab eventKey="suppliers" title={<span><i className="bi bi-truck me-1"></i>Nhà cung cấp</span>}>
          <Suppliers />
        </Tab>
      </Tabs>
    </div>
  )
}
