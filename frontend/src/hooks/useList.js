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

  const reload = useCallback(async () => {
    setLoading(true)
    try { setData(await ref.current.fetcher()) }
    catch (e) { ref.current.toast.error(errMsg(e)) }
    finally { setLoading(false) }
  }, [])

  useEffect(() => { reload() }, [reload])
  return { data, loading, reload, setData }
}
