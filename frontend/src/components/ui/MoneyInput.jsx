import { Form } from 'react-bootstrap'

/**
 * Ô nhập TIỀN (VND): hiển thị có dấu phân cách nghìn (vd 200.000) nhưng trả về SỐ nguyên đồng.
 * Tránh fat-finger (gõ nhầm 2000000) và bỏ phần thập phân (VND không có hào).
 * props: value (number|''), onChange(number), ...rest truyền xuống Form.Control.
 */
export default function MoneyInput({ value, onChange, ...rest }) {
  const display = value === '' || value == null ? '' : Number(value).toLocaleString('vi-VN')
  return (
    <Form.Control
      type="text"
      inputMode="numeric"
      value={display}
      onChange={(e) => {
        const digits = e.target.value.replace(/\D/g, '') // chỉ giữ chữ số
        onChange(digits === '' ? '' : Number(digits))
      }}
      {...rest}
    />
  )
}
