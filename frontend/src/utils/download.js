/** Tải một Blob về máy dưới tên file cho trước (Excel/PDF...). */
export function downloadBlob(blob, filename) {
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  a.click()
  URL.revokeObjectURL(url)
}

/** Mở một Blob ở tab mới (vd xem trước PDF phiếu lương). */
export function openBlob(blob) {
  window.open(URL.createObjectURL(blob), '_blank')
}
