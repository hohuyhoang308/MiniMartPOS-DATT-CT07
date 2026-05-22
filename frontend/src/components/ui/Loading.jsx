import { Spinner } from 'react-bootstrap'

export default function Loading({ label = 'Đang tải…' }) {
  return (
    <div className="text-center text-muted2 py-5">
      <Spinner style={{ color: 'var(--brand-600)' }} />
      <div className="mt-2 small">{label}</div>
    </div>
  )
}

/** Hàng skeleton cho bảng khi đang tải. */
export function SkeletonRows({ rows = 5, cols = 4 }) {
  return (
    <tbody>
      {Array.from({ length: rows }).map((_, r) => (
        <tr key={r}>
          {Array.from({ length: cols }).map((__, c) => (
            <td key={c}><div className="skeleton" style={{ height: 14, width: `${50 + ((c * 17) % 40)}%` }} /></td>
          ))}
        </tr>
      ))}
    </tbody>
  )
}
