import { useState } from 'react'
import { Tab, Tabs } from 'react-bootstrap'
import PageHeader from '../../components/ui/PageHeader'
import PayrollPeriods from './PayrollPeriods'
import PayProfiles from './PayProfiles'
import Attendance from './Attendance'

/**
 * Trung tâm LƯƠNG & CÔNG (ADMIN/MANAGER) — gom 2 việc về một trang nhiều tab:
 *   1) Kỳ lương: tính lương từ ca làm việc, chốt & chi lương.
 *   2) Cấu hình lương: đặt loại lương/đơn giá/phụ cấp cho từng nhân viên.
 * Mỗi tab tự tải dữ liệu khi mở (mountOnEnter/unmountOnExit).
 */
export default function Payroll() {
  const [tab, setTab] = useState('periods')
  return (
    <div>
      <PageHeader title="Lương & công"
        subtitle="Tính lương theo công làm việc (từ ca đã đóng), tăng ca, phụ cấp, thưởng/phạt và chốt kỳ lương." />
      <Tabs activeKey={tab} onSelect={setTab} mountOnEnter unmountOnExit className="mb-3">
        <Tab eventKey="periods" title={<span><i className="bi bi-cash-coin me-1"></i>Kỳ lương</span>}>
          <PayrollPeriods />
        </Tab>
        <Tab eventKey="attendance" title={<span><i className="bi bi-calendar-week me-1"></i>Bảng công</span>}>
          <Attendance />
        </Tab>
        <Tab eventKey="profiles" title={<span><i className="bi bi-person-badge me-1"></i>Cấu hình lương</span>}>
          <PayProfiles />
        </Tab>
      </Tabs>
    </div>
  )
}
