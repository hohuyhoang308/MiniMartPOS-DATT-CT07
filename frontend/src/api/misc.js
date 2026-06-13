import client, { unwrap } from './client'

/** Khách hàng & khuyến mãi (FR6, FR7). */
export const customerApi = {
  list: (keyword) => client.get('/customers', { params: { keyword } }).then(unwrap),
  byPhone: (phone) => client.get(`/customers/by-phone/${phone}`).then(unwrap),
  history: (id) => client.get(`/customers/${id}/history`).then(unwrap),
  create: (body) => client.post('/customers', body).then(unwrap),
  update: (id, body) => client.put(`/customers/${id}`, body).then(unwrap),
  remove: (id) => client.delete(`/customers/${id}`),
}

export const promotionApi = {
  list: () => client.get('/promotions').then(unwrap),
  validate: (code, subtotal) => client.post('/promotions/validate', { code, subtotal }).then(unwrap),
  create: (body) => client.post('/promotions', body).then(unwrap),
  update: (id, body) => client.put(`/promotions/${id}`, body).then(unwrap),
  remove: (id) => client.delete(`/promotions/${id}`),
}

/** Kho, dashboard, báo cáo (FR8, FR9). */
export const inventoryApi = {
  stock: () => client.get('/inventory/stock').then(unwrap),
  lowStock: () => client.get('/inventory/low-stock').then(unwrap),
  expiring: () => client.get('/inventory/expiring').then(unwrap),
  suggestions: () => client.get('/inventory/suggestions').then(unwrap),
  batches: (productId) => client.get(`/inventory/batches/${productId}`).then(unwrap),
  abcXyz: () => client.get('/inventory/abc-xyz').then(unwrap),
}

/** Kệ vật lý (FR8): xem/lên kệ (mọi vai trò) + CRUD kệ (quản lý). */
export const shelfApi = {
  list: () => client.get('/shelves').then(unwrap),
  inventory: (id) => client.get(`/shelves/${id}/inventory`).then(unwrap),
  transfer: (productId, shelfId, quantity) => client.post('/shelves/transfer', { productId, shelfId, quantity }).then(unwrap),
  returnToWarehouse: (batchId, quantity) => client.post('/shelves/return', { batchId, quantity }).then(unwrap),
  create: (body) => client.post('/shelves', body).then(unwrap),
  update: (id, body) => client.put(`/shelves/${id}`, body).then(unwrap),
  remove: (id) => client.delete(`/shelves/${id}`),
}

export const dashboardApi = {
  get: () => client.get('/dashboard').then(unwrap),
}

/** Nhật ký kiểm toán (chỉ ADMIN). */
export const auditApi = {
  recent: () => client.get('/audit').then(unwrap),
}

export const reportApi = {
  revenue: (from, to, groupBy = 'DAY') =>
    client.get('/reports/revenue', { params: { from, to, groupBy } }).then(unwrap),
  shifts: () => client.get('/reports/shifts').then(unwrap),
  exportUrl: (from, to, groupBy = 'DAY') =>
    `/api/reports/export?type=excel&from=${from}&to=${to}&groupBy=${groupBy}`,
}

/** Tài khoản & cấu hình (UC02, UC20, UC24). */
export const userApi = {
  list: () => client.get('/users').then(unwrap),
  create: (body) => client.post('/users', body).then(unwrap),
  update: (id, body) => client.put(`/users/${id}`, body).then(unwrap),
  resetPassword: (id, newPassword) => client.put(`/users/${id}/reset-password`, { newPassword }),
  lock: (id) => client.delete(`/users/${id}`),
}

/** Cấu hình theo TỪNG cửa hàng — admin truyền storeId để chọn cửa hàng cần cấu hình. */
export const storeConfigApi = {
  get: (storeId) => client.get('/store-config', { params: { storeId } }).then(unwrap),
  update: (storeId, body) => client.put('/store-config', body, { params: { storeId } }).then(unwrap),
}

/** Chi nhánh (đa chuỗi) — chỉ CHAIN_ADMIN. */
export const storeApi = {
  list: () => client.get('/stores').then(unwrap),
  create: (body) => client.post('/stores', body).then(unwrap),
  update: (id, body) => client.put(`/stores/${id}`, body).then(unwrap),
}

/** Tích hợp WEB2M & Telegram (FR-A6) — theo từng cửa hàng (storeId). */
export const integrationApi = {
  recipients: (storeId) => client.get('/integrations/telegram/recipients', { params: { storeId } }).then(unwrap),
  addRecipient: (storeId, body) => client.post('/integrations/telegram/recipients', body, { params: { storeId } }).then(unwrap),
  removeRecipient: (id) => client.delete(`/integrations/telegram/recipients/${id}`),
  telegramTest: (storeId, chatId, text) => client.post('/integrations/telegram/test', { chatId, text }, { params: { storeId } }).then(unwrap),
  web2mTest: (storeId, apiUrl) => client.post('/integrations/web2m/test', { apiUrl }, { params: { storeId } }).then(unwrap),
}
