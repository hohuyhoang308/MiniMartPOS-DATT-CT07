import client, { unwrap } from './client'

/** Ca làm việc, bán hàng, hóa đơn, thanh toán (FR4, FR5, FR-A). */
export const shiftApi = {
  current: () => client.get('/shifts/current').then(unwrap),
  suggestedOpening: () => client.get('/shifts/suggested-opening').then(unwrap),
  list: () => client.get('/shifts').then(unwrap),
  open: (openingCash) => client.post('/shifts/open', { openingCash }).then(unwrap),
  close: (id, closingCash) => client.post(`/shifts/${id}/close`, { closingCash }).then(unwrap),
}

export const invoiceApi = {
  create: (body) => client.post('/invoices', body).then(unwrap),
  list: (params) => client.get('/invoices', { params }).then(unwrap),
  get: (id) => client.get(`/invoices/${id}`).then(unwrap),
  cancel: (id, reason) => client.post(`/invoices/${id}/cancel`, { reason }).then(unwrap),
  pdfUrl: (id) => `/api/invoices/${id}/pdf`,
}

export const paymentApi = {
  // QR đã trả kèm khi tạo hóa đơn; FE chỉ cần poll trạng thái thanh toán.
  status: (invoiceId) => client.get(`/payments/${invoiceId}/status`).then(unwrap),
  // Thu ngân xác nhận đã nhận tiền QR (khi chưa khớp tự động qua WEB2M) → hoàn tất hóa đơn.
  confirm: (invoiceId) => client.post(`/payments/${invoiceId}/confirm`).then(unwrap),
}

export const returnApi = {
  returnable: (invoiceId) => client.get(`/returns/invoice/${invoiceId}/returnable`).then(unwrap),
  byInvoice: (invoiceId) => client.get(`/returns/invoice/${invoiceId}`).then(unwrap),
  create: (body) => client.post('/returns', body).then(unwrap),
}
