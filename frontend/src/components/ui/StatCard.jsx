import { Card } from 'react-bootstrap'

/** Thẻ chỉ số với chip icon gradient. chip: emerald|sky|amber|violet|rose */
export default function StatCard({ icon, chip = 'emerald', label, value, hint }) {
  return (
    <Card className="stat-card border-0 h-100">
      <Card.Body className="d-flex align-items-center gap-3">
        <div className={`stat-chip chip-${chip}`}>
          <i className={`bi ${icon}`}></i>
        </div>
        <div className="min-w-0">
          <div className="stat-label">{label}</div>
          <div className="stat-value">{value}</div>
          {hint && <div className="small text-muted2 mt-1">{hint}</div>}
        </div>
      </Card.Body>
    </Card>
  )
}
