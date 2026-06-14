import axios from 'axios'

export const AUTH_KEY = 'pos_auth'
/** Khoá localStorage giữ chi nhánh ADMIN đang xem ('' = toàn chuỗi). Dùng chung với StoreScopeContext. */
export const SCOPE_KEY = 'pos_store_scope'

/** Axios instance: gọi /api (Vite proxy sang backend 8080), tự đính JWT, xử lý 401. */
const client = axios.create({
  baseURL: '/api',
  headers: { 'Content-Type': 'application/json' },
})

// Request: đính token JWT + chi nhánh đang xem. Phạm vi cửa hàng (đa cửa hàng) do BACKEND quyết định:
// MANAGER/STAFF luôn theo cửa hàng của họ (header bị bỏ qua); ADMIN → theo chi nhánh đã chọn ở
// StoreScopeContext (header X-Store-Id), bỏ trống = toàn chuỗi. Cấu hình theo cửa hàng vẫn truyền storeId tường minh.
client.interceptors.request.use((config) => {
  const raw = localStorage.getItem(AUTH_KEY)
  if (raw) {
    try {
      const { token } = JSON.parse(raw)
      if (token) config.headers.Authorization = `Bearer ${token}`
    } catch {
      /* bỏ qua dữ liệu hỏng */
    }
  }
  // Chi nhánh ADMIN đang "đi sâu" — đính header để backend lọc theo cửa hàng đó (rỗng = toàn chuỗi).
  const scope = localStorage.getItem(SCOPE_KEY)
  if (scope) config.headers['X-Store-Id'] = scope
  return config
})

// Response: hết hạn/không hợp lệ token → xóa phiên & về trang đăng nhập
client.interceptors.response.use(
  (res) => res,
  (error) => {
    const status = error.response?.status
    const url = error.config?.url || ''
    if (status === 401 && !url.includes('/auth/login')) {
      localStorage.removeItem(AUTH_KEY)
      if (window.location.pathname !== '/login') window.location.href = '/login'
    }
    return Promise.reject(error)
  },
)

/** Lấy message lỗi gọn từ ApiResponse của backend. */
export function errMsg(error, fallback = 'Đã có lỗi xảy ra') {
  return error.response?.data?.message || error.message || fallback
}

/** Bóc trường data trong ApiResponse { success, message, data }. */
export function unwrap(res) {
  return res.data?.data
}

export default client
