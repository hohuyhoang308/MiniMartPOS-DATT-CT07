import { useEffect, useMemo, useState } from 'react'
import { Card, Col, Nav, Row, Table } from 'react-bootstrap'
import {
  Bar, BarChart, CartesianGrid, Cell, ResponsiveContainer, Tooltip, XAxis, YAxis,
} from 'recharts'
import PageHeader from '../../components/ui/PageHeader'
import StatCard from '../../components/ui/StatCard'
import EmptyState from '../../components/ui/EmptyState'
import Loading from '../../components/ui/Loading'
import { inventoryApi } from '../../api/misc'
import { useToast } from '../../context/ToastContext'
import { errMsg } from '../../api/client'

export default function Inventory() {
  const toast = useToast()
  const [stock, setStock] = useState([])
  const [expiring, setExpiring] = useState([])
  const [loading, setLoading] = useState(true)
  const [tab, setTab] = useState('all')

  useEffect(() => {
    Promise.all([inventoryApi.stock(), inventoryApi.expiring()])
      .then(([s, e]) => { setStock(s); setExpiring(e) })
      .catch((e) => toast.error(errMsg(e)))
      .finally(() => setLoading(false))
  }, [])

  const low = useMemo(() => stock.filter((s) => s.lowStock), [stock])
  const chartData = useMemo(
    () => [...stock].sort((a, b) => a.currentStock - b.currentStock).slice(0, 8)
      .map((s) => ({ name: s.name.length > 16 ? s.name.slice(0, 16) + '…' : s.name, stock: s.currentStock, min: s.minStock })),
    [stock],
  )

  if (loading) return <Loading />

  const rows = tab === 'low' ? low : stock

  return (
    <div>
      <PageHeader title="Tồn kho & cảnh báo" subtitle="Theo dõi số lượng tồn và hạn sử dụng theo lô" />

      <Row className="g-3 mb-3 stagger">
        <Col md={4}><StatCard icon="bi-boxes" chip="sky" label="Tổng mặt hàng" value={stock.length} /></Col>
        <Col md={4}><StatCard icon="bi-exclamation-triangle-fill" chip="amber" label="Tồn thấp / hết hàng" value={low.length} /></Col>
        <Col md={4}><StatCard icon="bi-calendar-x-fill" chip="rose" label="Lô cận/quá HSD (30 ngày)" value={expiring.length} /></Col>
      </Row>

      <Row className="g-3">
        <Col lg={5}>
          <Card className="border-0 h-100">
            <Card.Body>
              <Card.Title className="fs-6 mb-3">8 mặt hàng tồn thấp nhất</Card.Title>
              {chartData.length === 0 ? <EmptyState title="Chưa có dữ liệu" /> : (
                <ResponsiveContainer width="100%" height={300}>
                  <BarChart data={chartData} layout="vertical" margin={{ left: 8 }}>
                    <CartesianGrid strokeDasharray="3 3" horizontal={false} />
                    <XAxis type="number" allowDecimals={false} />
                    <YAxis type="category" dataKey="name" width={110} tick={{ fontSize: 11 }} />
                    <Tooltip />
                    <Bar dataKey="stock" radius={[0, 6, 6, 0]} barSize={16}>
                      {chartData.map((d, i) => (
                        <Cell key={i} fill={d.stock <= 0 ? '#f43f5e' : d.stock <= d.min ? '#f59e0b' : '#10b981'} />
                      ))}
                    </Bar>
                  </BarChart>
                </ResponsiveContainer>
              )}
            </Card.Body>
          </Card>
        </Col>

        <Col lg={7}>
          <Card className="border-0 h-100">
            <Card.Body className="pb-0">
              <Nav variant="pills" activeKey={tab} onSelect={setTab} className="mb-3 gap-2">
                <Nav.Item><Nav.Link eventKey="all">Tất cả ({stock.length})</Nav.Link></Nav.Item>
                <Nav.Item><Nav.Link eventKey="low">Tồn thấp ({low.length})</Nav.Link></Nav.Item>
                <Nav.Item><Nav.Link eventKey="expiring">Cận HSD ({expiring.length})</Nav.Link></Nav.Item>
              </Nav>
            </Card.Body>
            <div style={{ maxHeight: 360, overflowY: 'auto' }}>
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
              ) : (
                <Table hover className="mb-0">
                  <thead><tr><th>Sản phẩm</th><th>Mã vạch</th><th className="text-center">Tồn / Tối thiểu</th><th className="text-center">Trạng thái</th></tr></thead>
                  <tbody>
                    {rows.map((s) => (
                      <tr key={s.productId}>
                        <td className="fw-semibold">{s.name}</td>
                        <td className="text-muted2 small">{s.barcode}</td>
                        <td className="text-center num">{s.currentStock} / {s.minStock}</td>
                        <td className="text-center">
                          {s.currentStock <= 0 ? <span className="pill pill-danger"><i className="bi bi-x-octagon-fill"></i>Hết hàng</span>
                            : s.lowStock ? <span className="pill pill-warning"><i className="bi bi-exclamation-triangle-fill"></i>Tồn thấp</span>
                            : <span className="pill pill-success"><i className="bi bi-check-circle-fill"></i>Đủ hàng</span>}
                        </td>
                      </tr>
                    ))}
                    {rows.length === 0 && <tr><td colSpan={4}><EmptyState icon="bi-clipboard-check" title="Không có mặt hàng" /></td></tr>}
                  </tbody>
                </Table>
              )}
            </div>
          </Card>
        </Col>
      </Row>
    </div>
  )
}
