import { createContext, useContext, useEffect, useState } from 'react'

const ThemeContext = createContext({ theme: 'light', toggleTheme: () => {} })

/**
 * Quản lý giao diện Sáng/Tối. Đặt thuộc tính data-bs-theme lên <html> để Bootstrap 5.3
 * tự đổi màu mọi component; CSS design-system tự override biến màu theo [data-bs-theme="dark"].
 * Lưu lựa chọn vào localStorage (nhớ qua các lần mở).
 */
export function ThemeProvider({ children }) {
  const [theme, setTheme] = useState(() => localStorage.getItem('theme') || 'light')

  useEffect(() => {
    document.documentElement.setAttribute('data-bs-theme', theme)
    localStorage.setItem('theme', theme)
  }, [theme])

  const toggleTheme = () => setTheme((t) => (t === 'dark' ? 'light' : 'dark'))

  return <ThemeContext.Provider value={{ theme, toggleTheme }}>{children}</ThemeContext.Provider>
}

export const useTheme = () => useContext(ThemeContext)
