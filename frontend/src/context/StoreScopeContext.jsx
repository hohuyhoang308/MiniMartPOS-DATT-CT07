import { createContext, useContext, useEffect, useState } from 'react'
import { storeApi } from '../api/misc'
import { SCOPE_KEY } from '../api/client'
import { useAuth } from './AuthContext'

const StoreScopeContext = createContext(null)

/**
 * Phạm vi CHI NHÁNH cho quản trị viên toàn chuỗi (ADMIN).
 *
 * <p>ADMIN không gắn cửa hàng nên mặc định xem TOÀN CHUỖI. Provider này cho phép ADMIN chọn 1 chi nhánh
 * để "đi sâu" xem số liệu/vận hành của riêng cửa hàng đó. Lựa chọn được lưu vào localStorage và được
 * {@code client.js} đính vào header {@code X-Store-Id} ở MỌI request → backend (StoreContext) tự lọc.</p>
 *
 * <p>MANAGER/STAFF gắn cố định 1 cửa hàng (qua token) nên không dùng tới ngữ cảnh này.</p>
 */
export function StoreScopeProvider({ children }) {
  const { isAdmin, isAuthenticated, loading } = useAuth()
  const [stores, setStores] = useState([])
  const [scopeStoreId, setScope] = useState(() => localStorage.getItem(SCOPE_KEY) || '')

  // Nạp danh sách chi nhánh cho bộ chọn (chỉ ADMIN mới gọi được /stores).
  useEffect(() => {
    // CHƯA xác định xong phiên đăng nhập (AuthContext đang đọc token) → KHÔNG đụng vào scope.
    // Tránh race: lúc loading, isAdmin tạm = false → nhánh "else" bên dưới sẽ xoá nhầm chi nhánh ADMIN
    // đã chọn khi refresh cứng trang. Chỉ xử lý sau khi auth đã sẵn sàng.
    if (loading) return
    if (isAdmin) {
      storeApi.list().then((list) => {
        setStores(list)
        // Chi nhánh đang chọn không còn (bị xóa/đổi) → quay về toàn chuỗi để khỏi gửi header "treo".
        setScope((cur) => {
          if (cur && !list.some((s) => String(s.id) === String(cur))) {
            localStorage.removeItem(SCOPE_KEY)
            return ''
          }
          return cur
        })
      }).catch(() => setStores([]))
    } else {
      setStores([])
      // Người dùng gắn cửa hàng (hoặc đăng xuất) → xoá phạm vi để không gửi header thừa.
      if (scopeStoreId) { localStorage.removeItem(SCOPE_KEY); setScope('') }
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isAdmin, isAuthenticated, loading])

  /** Đổi chi nhánh đang xem ('' = toàn chuỗi). Ghi localStorage NGAY để request kế tiếp đính đúng header. */
  function setScopeStoreId(id) {
    const v = id || ''
    if (v) localStorage.setItem(SCOPE_KEY, v)
    else localStorage.removeItem(SCOPE_KEY)
    setScope(v)
  }

  const current = stores.find((s) => String(s.id) === String(scopeStoreId))

  const value = {
    stores,
    scopeStoreId,                 // '' = toàn chuỗi
    setScopeStoreId,
    scopeStoreName: current?.name || null,
    isChainScope: !scopeStoreId,  // true = đang xem toàn chuỗi
  }

  return <StoreScopeContext.Provider value={value}>{children}</StoreScopeContext.Provider>
}

export function useStoreScope() {
  const ctx = useContext(StoreScopeContext)
  if (!ctx) throw new Error('useStoreScope phải dùng trong StoreScopeProvider')
  return ctx
}
