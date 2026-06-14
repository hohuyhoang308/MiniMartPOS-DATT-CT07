import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Alert, Button, Card, Form, Spinner } from 'react-bootstrap'
import { useAuth } from '../../context/AuthContext'
import { errMsg } from '../../api/client'

export default function Login() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const [username, setUsername] = useState('admin')
  const [password, setPassword] = useState('123456')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  async function handleSubmit(e) {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      const session = await login(username.trim(), password)
      navigate(session.role === 'STAFF' ? '/pos' : '/dashboard', { replace: true })
    } catch (err) {
      setError(errMsg(err, 'Đăng nhập không thành công. Vui lòng kiểm tra lại tên đăng nhập và mật khẩu'))
    } finally {
      setLoading(false)
    }
  }

  const quick = (u) => { setUsername(u); setPassword('123456') }

  return (
    <div className="login-wrap">
      <Card className="login-card fade-up">
        <Card.Body className="p-4 p-md-5">
          <div className="text-center mb-4">
            <div className="login-logo mb-3"><i className="bi bi-shop-window"></i></div>
            <h3 className="fw-bold mb-1" style={{ letterSpacing: '-.03em' }}>MiniMart POS</h3>
            <p className="text-muted2 mb-0 small">Phần mềm bán hàng cho cửa hàng tiện lợi</p>
          </div>

          {error && <Alert variant="danger" className="py-2 small">{error}</Alert>}

          <Form onSubmit={handleSubmit}>
            <Form.Group className="mb-3">
              <Form.Label>Tên đăng nhập</Form.Label>
              <div className="input-group">
                <span className="input-group-text"><i className="bi bi-person"></i></span>
                <Form.Control value={username} onChange={(e) => setUsername(e.target.value)} autoFocus />
              </div>
            </Form.Group>
            <Form.Group className="mb-4">
              <Form.Label>Mật khẩu</Form.Label>
              <div className="input-group">
                <span className="input-group-text"><i className="bi bi-lock"></i></span>
                <Form.Control type="password" value={password} onChange={(e) => setPassword(e.target.value)} />
              </div>
            </Form.Group>
            <Button type="submit" className="w-100 py-2" disabled={loading}>
              {loading ? <Spinner size="sm" /> : <><i className="bi bi-box-arrow-in-right me-2"></i>Đăng nhập</>}
            </Button>
          </Form>

          <div className="text-center mt-4">
            <div className="text-muted2 small mb-2">Tài khoản demo (mật khẩu <b>123456</b>)</div>
            <div className="d-flex flex-wrap gap-2 justify-content-center">
              {[
                { u: 'admin', hint: 'Quản trị toàn chuỗi' },
                { u: 'manager', hint: 'Quản lý CH1' },
                { u: 'staff', hint: 'Nhân viên CH1' },
                { u: 'manager2', hint: 'Quản lý CH2' },
                { u: 'staff2', hint: 'Nhân viên CH2' },
              ].map(({ u, hint }) => (
                <Button key={u} size="sm" variant="light" title={hint} onClick={() => quick(u)}>{u}</Button>
              ))}
            </div>
          </div>
        </Card.Body>
      </Card>
    </div>
  )
}
