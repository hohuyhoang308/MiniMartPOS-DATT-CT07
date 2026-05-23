import { useEffect, useState } from 'react'
import { Modal } from 'react-bootstrap'

/** Máy tính bỏ túi cho thu ngân (cộng/trừ/nhân/chia/%). Hỗ trợ bàn phím. */
export default function Calculator({ show, onHide }) {
  const [display, setDisplay] = useState('0')
  const [prev, setPrev] = useState(null)
  const [op, setOp] = useState(null)
  const [fresh, setFresh] = useState(true)

  function inputDigit(d) {
    if (fresh) { setDisplay(d); setFresh(false) }
    else setDisplay(display.length < 14 ? (display === '0' ? d : display + d) : display)
  }
  function inputDot() {
    if (fresh) { setDisplay('0.'); setFresh(false) }
    else if (!display.includes('.')) setDisplay(display + '.')
  }
  function clearAll() { setDisplay('0'); setPrev(null); setOp(null); setFresh(true) }
  function backspace() { setDisplay(display.length > 1 ? display.slice(0, -1) : '0') }
  function percent() { setDisplay(String(parseFloat(display) / 100)) }
  function toggleSign() { setDisplay(String(parseFloat(display) * -1)) }

  function compute(a, b, operator) {
    switch (operator) {
      case '+': return a + b
      case '-': return a - b
      case '×': return a * b
      case '÷': return b === 0 ? 0 : a / b
      default: return b
    }
  }
  function chooseOp(nextOp) {
    const cur = parseFloat(display)
    if (prev !== null && op && !fresh) {
      const r = compute(prev, cur, op)
      setDisplay(formatNum(r)); setPrev(r)
    } else setPrev(cur)
    setOp(nextOp); setFresh(true)
  }
  function equals() {
    if (op && prev !== null) {
      const r = compute(prev, parseFloat(display), op)
      setDisplay(formatNum(r)); setPrev(null); setOp(null); setFresh(true)
    }
  }
  function formatNum(n) {
    if (!isFinite(n)) return '0'
    return String(Math.round(n * 1e6) / 1e6)
  }

  // Bàn phím
  useEffect(() => {
    if (!show) return
    function onKey(e) {
      const k = e.key
      if (k >= '0' && k <= '9') inputDigit(k)
      else if (k === '.') inputDot()
      else if (k === '+') chooseOp('+')
      else if (k === '-') chooseOp('-')
      else if (k === '*') chooseOp('×')
      else if (k === '/') { e.preventDefault(); chooseOp('÷') }
      else if (k === 'Enter' || k === '=') { e.preventDefault(); equals() }
      else if (k === 'Backspace') backspace()
      else if (k === 'Escape') clearAll()
      else if (k === '%') percent()
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  })

  const KEYS = [
    ['C', 'backspace', '%', '÷'],
    ['7', '8', '9', '×'],
    ['4', '5', '6', '-'],
    ['1', '2', '3', '+'],
    ['±', '0', '.', '='],
  ]

  function press(k) {
    if (k === 'C') clearAll()
    else if (k === 'backspace') backspace()
    else if (k === '%') percent()
    else if (k === '±') toggleSign()
    else if (k === '=') equals()
    else if (['+', '-', '×', '÷'].includes(k)) chooseOp(k)
    else if (k === '.') inputDot()
    else inputDigit(k)
  }

  const prettyDisplay = (() => {
    const n = parseFloat(display)
    if (display.includes('.') || !isFinite(n)) return display
    return n.toLocaleString('vi-VN')
  })()

  return (
    <Modal show={show} onHide={onHide} centered size="sm">
      <Modal.Header closeButton><Modal.Title className="fs-6"><i className="bi bi-calculator me-2"></i>Máy tính</Modal.Title></Modal.Header>
      <Modal.Body className="p-3">
        <div className="rounded p-3 mb-3 text-end" style={{ background: 'var(--ink-900)', color: '#fff' }}>
          <div className="text-muted2 small" style={{ minHeight: 18 }}>{op ? `${prev?.toLocaleString('vi-VN')} ${op}` : ''}</div>
          <div className="num" style={{ fontSize: '2rem', fontWeight: 700, wordBreak: 'break-all' }}>{prettyDisplay}</div>
        </div>
        <div className="d-grid gap-2" style={{ gridTemplateColumns: 'repeat(4, 1fr)' }}>
          {KEYS.flat().map((k) => {
            const isOp = ['÷', '×', '-', '+', '='].includes(k)
            const isFn = ['C', 'backspace', '%', '±'].includes(k)
            return (
              <button key={k} type="button" onClick={() => press(k)}
                className={`btn ${k === '=' ? 'btn-primary' : isOp ? 'btn-soft' : isFn ? 'btn-light' : 'btn-light'}`}
                style={{ height: 50, fontSize: '1.1rem', fontWeight: 600 }}>
                {k === 'backspace' ? <i className="bi bi-backspace"></i> : k}
              </button>
            )
          })}
        </div>
      </Modal.Body>
    </Modal>
  )
}
