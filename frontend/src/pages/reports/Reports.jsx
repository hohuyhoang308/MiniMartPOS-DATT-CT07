import { useState } from 'react'
import { Tab, Tabs } from 'react-bootstrap'
import PageHeader from '../../components/ui/PageHeader'
import RevenueReport from './RevenueReport'
import EmployeeReport from './EmployeeReport'
import ProductReport from './ProductReport'
import AbcXyz from '../inventory/AbcXyz'

/**
 * Trung tâm BÁO CÁO & PHÂN TÍCH — gom mọi báo cáo về MỘT trang nhiều tab (trước đây bị tách thành
 * nhiều mục menu rời rạc). Mỗi tab tự tải dữ liệu khi mở (mountOnEnter/unmountOnExit) nên không nặng.
 */
export default function Reports() {
  const [tab, setTab] = useState('revenue')
  return (
    <div>
      <PageHeader title="Báo cáo & phân tích" subtitle="Doanh thu, ca làm việc, hiệu suất nhân viên, lợi nhuận sản phẩm và phân loại hàng" />
      <Tabs activeKey={tab} onSelect={setTab} mountOnEnter unmountOnExit className="mb-3">
        <Tab eventKey="revenue" title={<span><i className="bi bi-bar-chart-line-fill me-1"></i>Doanh thu & ca</span>}>
          <RevenueReport embedded />
        </Tab>
        <Tab eventKey="employees" title={<span><i className="bi bi-person-badge me-1"></i>Hiệu suất nhân viên</span>}>
          <EmployeeReport embedded />
        </Tab>
        <Tab eventKey="products" title={<span><i className="bi bi-graph-up-arrow me-1"></i>Lợi nhuận sản phẩm</span>}>
          <ProductReport embedded />
        </Tab>
        <Tab eventKey="abcxyz" title={<span><i className="bi bi-bar-chart-steps me-1"></i>Phân loại hàng (ABC/XYZ)</span>}>
          <AbcXyz embedded />
        </Tab>
      </Tabs>
    </div>
  )
}
