import { useCallback, useEffect, useRef, useState } from 'react'
import { errMsg } from '../api/client'
import { useToast } from '../context/ToastContext'

/**
 * Tải một danh sách 1 lần khi mount: gom sẵn state data + loading + báo lỗi qua toast.
 * Trả { data, loading, reload, setData } — dùng `reload` để làm tươi sau khi thêm/sửa/xóa.
 *
 * An toàn với fetcher viết inline (vd `() => api.list(q)`): fetcher & toast được giữ trong ref nên
 * `reload` là tham chiếu ổn định và effect chỉ chạy đúng MỘT lần khi mount, không refetch vô hạn.
 * Muốn tự tải lại khi tham số đổi thì gọi `reload()` trong effect riêng của trang.
 */
export function useList(fetcher, initial = []) {
  const toast = useToast()
  const [data, setData] = useState(initial)
  const [loading, setLoading] = useState(true)
  const ref = useRef({ fetcher, toast })
  ref.current = { fetcher, toast }
  const reqIdRef = useRef(0) // chống "stale response": chỉ nhận kết quả của lần gọi MỚI NHẤT

  const reload = useCallback(async () => {
    const reqId = ++reqIdRef.current
    setLoading(true)
    try {
      const d = await ref.current.fetcher()
      if (reqId === reqIdRef.current) setData(d)
    } catch (e) {
      if (reqId === reqIdRef.current) ref.current.toast.error(errMsg(e))
    } finally {
      if (reqId === reqIdRef.current) setLoading(false)
    }
  }, [])

  useEffect(() => { reload() }, [reload])
  return { data, loading, reload, setData }
}
