import { useEffect, useMemo, useState } from 'react'
import { useNavigate, useLocation } from 'react-router-dom'
import { Button, Card, Col, Modal, Nav, Row, Table } from 'react-bootstrap'
import PageHeader from '../../components/ui/PageHeader'
import InfoBanner from '../../components/ui/InfoBanner'
import StatCard from '../../components/ui/StatCard'
import EmptyState from '../../components/ui/EmptyState'
import Loading from '../../components/ui/Loading'
import ExpiryPill from '../../components/ui/ExpiryPill'
import { inventoryApi } from '../../api/misc'
import { useToast } from '../../context/ToastContext'
import { errMsg } from '../../api/client'

const URGENCY = {
  OUT: { cls: 'pill-danger', icon: 'bi-x-octagon-fill', label: 'Hết hàng' },
  URGENT: { cls: 'pill-warning', icon: 'bi-exclamation-triangle-fill', label: 'Khẩn cấp' },
  REORDER: { cls: 'pill-info', icon: 'bi-arrow-repeat', label: 'Nên nhập' },
}

// Màu nhóm ABC (theo doanh thu): A chủ lực → đậm, C đuôi → mờ.
const ABC_CLS = { A: 'pill-violet', B: 'pill-info', C: 'pill-muted' }

function ShelfCell({ s }) {
  const shelf = s.shelfStock ?? 0
  const cls = shelf <= 0 ? 'text-danger' : shelf <= s.minStock ? 'text-warning' : 'text-success'
  return <span className={`fw-semibold ${cls}`}>{shelf}</span>
}

export default function Inventory() {
  const toast = useToast()
  const navigate = useNavigate()
  const location = useLocation()
  const [stock, setStock] = useState([])
  const [expiring, setExpiring] = useState([])
  const [suggestions, setSuggestions] = useState([])
  const [loading, setLoading] = useState(true)
  const [tab, setTab] = useState(location.state?.tab || 'all') // mở sẵn tab khi điều hướng từ ABC/XYZ
  const [batchTarget, setBatchTarget] = useState(null)

  useEffect(() => {
    Promise.all([inventoryApi.stock(), inventoryApi.expiring(), inventoryApi.suggestions()])
      .then(([s, e, sg]) => { setStock(s); setExpiring(e); setSuggestions(sg) })
      .catch((e) => toast.error(errMsg(e)))
      .finally(() => setLoading(false))
  }, [])

  const shelfLow = useMemo(() => stock.filter((s) => s.shelfLow).length, [stock])
  if (loading) return <Loading />

  return (
    <div className="page-fill">
      <PageHeader title="Tồn kho · Kho & Kệ" subtitle="Tồn theo lô/HSD, đề xuất nhập (EOQ) — đưa hàng lên kệ ở trang Lên kệ" />

      <InfoBanner id="inventory" title="Đọc bảng tồn kho">
        Mỗi sản phẩm có tồn <b>KỆ</b> (bán được) và <b>KHO</b> (chưa lên kệ). Bấm <b>tên sản phẩm</b> để xem
        các <b>lô + HSD</b>. <b>Cận HSD</b>: lô sắp/đã hết hạn — ưu tiên bán. <b>Đề xuất nhập</b> dùng luôn
        kết quả <b>ABC/XYZ</b> (cột <b>Nhóm</b>): hàng <b>A</b> giữ kỹ hơn (mức phục vụ cao), hàng <b>C·Z</b>
        đặt thưa & ít tồn; kèm <b>điểm tái đặt</b> và <b>EOQ</b>. Dòng có dấu <i className="bi bi-calendar-x-fill text-warning"></i>
        là đang có lô <b>cận HSD</b> — nên đẩy bán trước khi nhập thêm. Việc <b>lên kệ</b> làm ở trang <b>"Lên kệ"</b>.
      </InfoBanner>

      <Row className="g-3 mb-3 stagger">
        <Col md={3}><StatCard icon="bi-shop" chip="sky" label="Tổng mặt hàng" value={stock.length} /></Col>
        <Col md={3}><StatCard icon="bi-arrow-up-square-fill" chip="emerald" label="Cần lên kệ" value={shelfLow} /></Col>
        <Col md={3}><StatCard icon="bi-cart-plus" chip="violet" label="Cần nhập hàng" value={suggestions.length} /></Col>
        <Col md={3}><StatCard icon="bi-calendar-x-fill" chip="rose" label="Lô cận/quá HSD (30 ngày)" value={expiring.length} /></Col>
      </Row>

      <Card className="border-0 fill-card">
        <Card.Body className="pb-0 d-flex justify-content-between align-items-start flex-wrap gap-2">
          <Nav variant="pills" activeKey={tab} onSelect={setTab} className="mb-3 gap-2">
            <Nav.Item><Nav.Link eventKey="all">Tất cả ({stock.length})</Nav.Link></Nav.Item>
            <Nav.Item><Nav.Link eventKey="suggest">Đề xuất nhập ({suggestions.length})</Nav.Link></Nav.Item>
            <Nav.Item><Nav.Link eventKey="expiring">Cận HSD ({expiring.length})</Nav.Link></Nav.Item>
          </Nav>
          {tab === 'all' && shelfLow > 0 && (
            <Button size="sm" variant="soft" onClick={() => navigate('/shelf')}><i className="bi bi-arrow-up me-1"></i>Lên kệ ({shelfLow})</Button>
          )}
          {tab === 'suggest' && suggestions.length > 0 && (
            <Button size="sm" onClick={() => navigate('/receipts', {
              state: { prefill: suggestions.map((s) => ({ productId: s.productId, quantity: s.suggestedQty })) },
            })}><i className="bi bi-box-arrow-in-down me-1"></i>Lập phiếu nhập ({suggestions.length})</Button>
          )}
        </Card.Body>
        <div className="table-responsive fill-scroll">
          {tab === 'expiring' ? (
            <Table hover className="mb-0">
              <thead><tr><th>Sản phẩm</th><th className="text-center">Tồn lô</th><th>HSD</th><th className="text-center">Còn lại</th></tr></thead>
              <tbody>
                {expiring.map((b) => (
                  <tr key={b.batchId}>
                    <td className="fw-semibold">{b.productName}</td>
                    <td className="text-center num">{b.quantityRemaining}</td>
                    <td>{b.expiryDate}</td>
                    <td className="text-center">
                      <span className={`pill ${b.daysLeft < 0 ? 'pill-danger' : b.daysLeft <= 7 ? 'pill-warning' : 'pill-info'}`}>
                        {b.daysLeft < 0 ? `Quá ${-b.daysLeft} ngày` : `${b.daysLeft} ngày`}
                      </span>
                    </td>
                  </tr>
                ))}
                {expiring.length === 0 && <tr><td colSpan={4}><EmptyState icon="bi-calendar-check" title="Không có lô cận hạn" /></td></tr>}
              </tbody>
            </Table>
          ) : tab === 'suggest' ? (
            <Table hover className="mb-0 align-middle">
              <thead><tr>
                <th>Sản phẩm</th><th className="text-center">Nhóm</th><th className="text-center">Tồn / Min</th><th className="text-center">Bán 30 ngày</th>
                <th className="text-center">Tái đặt</th><th className="text-center">EOQ</th><th className="text-center">Đề xuất</th><th className="text-center">Độ khẩn</th>
              </tr></thead>
              <tbody>
                {suggestions.map((s) => {
                  const u = URGENCY[s.urgency] || URGENCY.REORDER
                  return (
                    <tr key={s.productId}>
                      <td className="fw-semibold">{s.name}
                        {s.hasExpiringStock && <i className="bi bi-calendar-x-fill text-warning ms-1" title="Có lô cận/quá HSD — nên đẩy bán trước khi nhập thêm"></i>}
                        <div className="text-muted2 small">{s.soldLast30} bán · {s.avgDailySold}/ngày</div>
                      </td>
                      <td className="text-center">
                        <span className={`pill ${ABC_CLS[s.abcClass] || 'pill-muted'}`}
                          title="ABC theo doanh thu · XYZ theo độ ổn định nhu cầu — quyết định mức giữ hàng">
                          {s.abcClass}·{s.xyzClass}
                        </span>
                      </td>
                      <td className="text-center num">{s.currentStock} / {s.minStock}</td>
                      <td className="text-center num">{s.daysUntilStockout != null ? `${s.daysUntilStockout} ngày` : '—'}</td>
                      <td className="text-center num" title="Điểm tái đặt hàng (đã tính theo mức phục vụ của nhóm ABC)">{s.reorderPoint}</td>
                      <td className="text-center num text-primary fw-semibold" title="Lượng đặt kinh tế (EOQ)">{s.eoq}</td>
                      <td className="text-center num fw-bold text-success">+{s.suggestedQty}</td>
                      <td className="text-center"><span className={`pill ${u.cls}`}><i className={`bi ${u.icon}`}></i>{u.label}</span></td>
                    </tr>
                  )
                })}
                {suggestions.length === 0 && <tr><td colSpan={8}><EmptyState icon="bi-check2-circle" title="Tồn kho ổn — chưa cần nhập thêm" /></td></tr>}
              </tbody>
            </Table>
          ) : (
            <Table hover className="mb-0 align-middle">
              <thead><tr>
                <th>Sản phẩm</th><th className="text-center">Kệ</th><th className="text-center">Kho</th>
                <th className="text-center">Tổng / Min</th><th className="text-center">Trạng thái</th>
              </tr></thead>
              <tbody>
                {stock.map((s) => (
                  <tr key={s.productId}>
                    <td className="fw-semibold cursor-pointer" onClick={() => setBatchTarget(s)} title="Xem các lô & HSD">
                      {s.name} <i className="bi bi-card-list text-muted2"></i>
                      <div className="text-muted2 small">{s.barcode}</div>
                    </td>
                    <td className="text-center num"><ShelfCell s={s} /></td>
                    <td className="text-center num">{s.warehouseStock ?? 0}</td>
                    <td className="text-center num">{s.currentStock} / {s.minStock}</td>
                    <td className="text-center">
                      {s.currentStock <= 0 ? <span className="pill pill-danger"><i className="bi bi-x-octagon-fill"></i>Hết hàng</span>
                        : s.shelfLow ? <span className="pill pill-warning"><i className="bi bi-arrow-up"></i>Cần lên kệ</span>
                        : s.lowStock ? <span className="pill pill-warning"><i className="bi bi-exclamation-triangle-fill"></i>Tồn thấp</span>
                        : <span className="pill pill-success"><i className="bi bi-check-circle-fill"></i>Đủ hàng</span>}
                    </td>
                  </tr>
                ))}
                {stock.length === 0 && <tr><td colSpan={5}><EmptyState icon="bi-clipboard-check" title="Không có mặt hàng" /></td></tr>}
              </tbody>
            </Table>
          )}
        </div>
      </Card>

      <BatchDetailModal product={batchTarget} onHide={() => setBatchTarget(null)} />
    </div>
  )
}

/** Modal xem CHI TIẾT CÁC LÔ của 1 sản phẩm: HSD, tồn kho/kệ theo lô. */
function BatchDetailModal({ product, onHide }) {
  const toast = useToast()
  const [batches, setBatches] = useState([])
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    if (!product) return
    setLoading(true)
    inventoryApi.batches(product.productId)
      .then(setBatches).catch((e) => toast.error(errMsg(e))).finally(() => setLoading(false))
  }, [product])

  if (!product) return null
  return (
    <Modal show={!!product} onHide={onHide} centered>
      <Modal.Header closeButton><Modal.Title>Lô hàng · {product.name}</Modal.Title></Modal.Header>
      <Modal.Body>
        <div className="small text-muted2 mb-2">Bán theo <b>FIFO</b> — lô cận hạn (trên cùng) xuất trước.</div>
        {loading ? <Loading /> : (
          <Table size="sm" hover className="mb-0">
            <thead><tr><th>HSD</th><th className="text-center">Nhập</th><th className="text-center">Kho</th><th className="text-center">Kệ</th><th className="text-center">Còn</th></tr></thead>
            <tbody>
              {batches.map((b) => (
                <tr key={b.batchId}>
                  <td><ExpiryPill days={b.daysLeft} date={b.expiryDate} /></td>
                  <td className="text-center num text-muted2">{b.quantityIn}</td>
                  <td className="text-center num">{b.inWarehouse}</td>
                  <td className="text-center num text-success fw-semibold">{b.onShelf}</td>
                  <td className="text-center num fw-semibold">{b.quantityRemaining}</td>
                </tr>
              ))}
              {batches.length === 0 && <tr><td colSpan={5}><EmptyState icon="bi-box" title="Sản phẩm không còn lô tồn" /></td></tr>}
            </tbody>
          </Table>
        )}
      </Modal.Body>
    </Modal>
  )
}
