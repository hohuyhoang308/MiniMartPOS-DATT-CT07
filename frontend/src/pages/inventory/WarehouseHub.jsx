import { useEffect, useState } from 'react'
import { useLocation } from 'react-router-dom'
import { Tab, Tabs } from 'react-bootstrap'
import PageHeader from '../../components/ui/PageHeader'
import Inventory from './Inventory'
import GoodsReceipts from './GoodsReceipts'
import Transfers from './Transfers'
import StorePrices from './StorePrices'
import ShelfManage from './ShelfManage'

/**
 * Trung tâm KHO HÀNG — gom các trang vận hành kho theo từng chi nhánh (tồn kho/xuất hủy, nhập kho,
 * điều chuyển, giá theo chi nhánh, cấu hình kệ) về MỘT trang nhiều tab. Mỗi tab tự tải dữ liệu khi mở.
 *
 * <p>Mở sẵn tab theo {@code location.state.tab} (vd từ "gợi ý nhập hàng" ở tab Tồn kho điều hướng sang
 * tab Nhập kho kèm {@code prefill}) — GoodsReceipts đọc {@code state.prefill} khi được mount.</p>
 */
export default function WarehouseHub() {
  const location = useLocation()
  // dùng key RIÊNG `hubTab` cho tab ngoài để không đụng `state.tab` (tab NỘI BỘ của trang Tồn kho từ ABC/XYZ).
  const [tab, setTab] = useState(location.state?.hubTab || 'stock')
  // Điều hướng tới /inventory với hubTab MỚI khi hub ĐÃ mount (vd "Gợi ý nhập hàng" → "Lập phiếu nhập"):
  // useState init chỉ chạy 1 lần nên phải đồng bộ tab qua effect, nếu không tab Nhập kho không mở và prefill không chạy.
  useEffect(() => {
    if (location.state?.hubTab) setTab(location.state.hubTab)
  }, [location.state])
  return (
    <div>
      <PageHeader title="Kho hàng" subtitle="Tồn kho, nhập kho, điều chuyển, giá theo chi nhánh và cấu hình kệ — theo từng chi nhánh" />
      <Tabs activeKey={tab} onSelect={setTab} mountOnEnter unmountOnExit className="mb-3">
        <Tab eventKey="stock" title={<span><i className="bi bi-clipboard2-pulse-fill me-1"></i>Tồn kho & xuất hủy</span>}>
          <Inventory embedded />
        </Tab>
        <Tab eventKey="receipts" title={<span><i className="bi bi-box-arrow-in-down me-1"></i>Nhập kho</span>}>
          <GoodsReceipts embedded />
        </Tab>
        <Tab eventKey="transfers" title={<span><i className="bi bi-arrow-left-right me-1"></i>Điều chuyển kho</span>}>
          <Transfers embedded />
        </Tab>
        <Tab eventKey="prices" title={<span><i className="bi bi-tag me-1"></i>Giá theo chi nhánh</span>}>
          <StorePrices embedded />
        </Tab>
        <Tab eventKey="shelves" title={<span><i className="bi bi-grid-3x3-gap-fill me-1"></i>Cấu hình kệ</span>}>
          <ShelfManage embedded />
        </Tab>
      </Tabs>
    </div>
  )
}
