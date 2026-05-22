import client, { unwrap } from './client'

/** Ca làm việc, bán hàng, hóa đơn, thanh toán (FR4, FR5, FR-A). */
export const shiftApi = {
  current: () => client.get('/shifts/current').then(unwrap),
  open: (openingCash) => client.post('/shifts/open', { openingCash }).then(unwrap),
  close: (id, closingCash) => client.post(`/shifts/${id}/close`, { closingCash }).then(unwrap),
}

export const invoiceApi = {
  create: (body) => client.post('/invoices', body).then(unwrap),
  list: (params) => client.get('/invoices', { params }).then(unwrap),
  get: (id) => client.get(`/invoices/${id}`).then(unwrap),
  cancel: (id) => client.post(`/invoices/${id}/cancel`).then(unwrap),
  pdfUrl: (id) => `/api/invoices/${id}/pdf`,
}

export const paymentApi = {
  qr: (invoiceId) => client.get('/payments/qr', { params: { invoiceId } }).then(unwrap),
  status: (invoiceId) => client.get(`/payments/${invoiceId}/status`).then(unwrap),
  web2mSync: () => client.post('/payments/web2m/sync').then(unwrap),
}
