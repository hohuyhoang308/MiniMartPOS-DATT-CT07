import { createContext, useContext, useEffect, useState } from 'react'
import { authApi } from '../api/auth'
import { AUTH_KEY } from '../api/client'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const raw = localStorage.getItem(AUTH_KEY)
    if (raw) {
      try {
        setUser(JSON.parse(raw))
      } catch {
        localStorage.removeItem(AUTH_KEY)
      }
    }
    setLoading(false)
  }, [])

  async function login(username, password) {
    const data = await authApi.login(username, password)
    const session = {
      token: data.token,
      userId: data.userId,
      username: data.username,
      fullName: data.fullName,
      role: data.role,
      storeId: data.storeId,       // null nếu là ADMIN toàn chuỗi
      storeName: data.storeName,   // tên cửa hàng (MANAGER/STAFF); null nếu ADMIN
    }
    localStorage.setItem(AUTH_KEY, JSON.stringify(session))
    setUser(session)
    return session
  }

  function logout() {
    localStorage.removeItem(AUTH_KEY)
    setUser(null)
  }

  const value = {
    user,
    loading,
    login,
    logout,
    // ADMIN = quản trị viên toàn chuỗi (không gắn cửa hàng); MANAGER/STAFF gắn 1 cửa hàng (qua token).
    isAdmin: user?.role === 'ADMIN',
    isAuthenticated: !!user,
    hasRole: (...roles) => !!user && roles.includes(user.role),
  }

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth phải dùng trong AuthProvider')
  return ctx
}
