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

/** Xuất hủy / điều chỉnh giảm tồn (FR8 — kiểm kê & hao hụt) — ADMIN & MANAGER. */
export const stockAdjustmentApi = {
  list: () => client.get('/stock-adjustments').then(unwrap),
  create: (body) => client.post('/stock-adjustments', body).then(unwrap),
}

/** Điều chuyển kho giữa các chi nhánh (FR8) — ADMIN & MANAGER. */
export const transferApi = {
  list: () => client.get('/stock-transfers').then(unwrap),
  create: (body) => client.post('/stock-transfers', body).then(unwrap),
  ship: (id) => client.post(`/stock-transfers/${id}/ship`).then(unwrap),
  receive: (id) => client.post(`/stock-transfers/${id}/receive`).then(unwrap),
  cancel: (id, reason) => client.post(`/stock-transfers/${id}/cancel`, { reason }).then(unwrap),
}

/** Giá bán riêng theo chi nhánh (override giá gốc) — ADMIN & MANAGER, theo cửa hàng đang xem. */
export const storePriceApi = {
  list: () => client.get('/store-prices').then(unwrap),
  set: (productId, salePrice) => client.put('/store-prices', { productId, salePrice }).then(unwrap),
  clear: (productId) => client.delete(`/store-prices/${productId}`),
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
  /** So sánh KPI giữa các chi nhánh (toàn chuỗi) — chỉ ADMIN. */
  storeComparison: () => client.get('/dashboard/stores').then(unwrap),
}

/** Nhật ký kiểm toán — ADMIN (toàn chuỗi) & MANAGER (chỉ cửa hàng của mình, backend tự lọc). */
export const auditApi = {
  recent: () => client.get('/audit').then(unwrap),
}

export const reportApi = {
  revenue: (from, to, groupBy = 'DAY') =>
    client.get('/reports/revenue', { params: { from, to, groupBy } }).then(unwrap),
  shifts: () => client.get('/reports/shifts').then(unwrap),
  employees: (from, to) => client.get('/reports/employees', { params: { from, to } }).then(unwrap),
  products: (from, to) => client.get('/reports/products', { params: { from, to } }).then(unwrap),
  dailyRollup: (from, to) => client.get('/reports/daily-rollup', { params: { from, to } }).then(unwrap),
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

/**
 * Lương & bảng công (module Lương). Phạm vi chi nhánh do backend chốt theo X-Store-Id.
 * Quản lý: cấu hình lương + tính/chốt kỳ + thưởng/phạt. Mọi vai trò: phiếu lương của tôi.
 */
export const payrollApi = {
  payProfiles: () => client.get('/payroll/pay-profiles').then(unwrap),
  setPayProfile: (userId, body) => client.put(`/payroll/pay-profiles/${userId}`, body).then(unwrap),
  periods: () => client.get('/payroll/periods').then(unwrap),
  compute: (month) => client.post('/payroll/periods/compute', { month }).then(unwrap),
  period: (id) => client.get(`/payroll/periods/${id}`).then(unwrap),
  submit: (id) => client.post(`/payroll/periods/${id}/submit`).then(unwrap),
  approve: (id) => client.post(`/payroll/periods/${id}/approve`).then(unwrap),
  reject: (id, reason) => client.post(`/payroll/periods/${id}/reject`, { reason }).then(unwrap),
  pay: (id) => client.post(`/payroll/periods/${id}/pay`).then(unwrap),
  addAdjustment: (payslipId, body) => client.post(`/payroll/payslips/${payslipId}/adjustments`, body).then(unwrap),
  removeAdjustment: (id) => client.delete(`/payroll/adjustments/${id}`).then(unwrap),
  myPayslips: () => client.get('/payroll/my-payslips').then(unwrap),
  // Chấm công thủ công & nghỉ phép
  attendance: (month) => client.get('/payroll/attendance', { params: { month } }).then(unwrap),
  addAttendance: (body) => client.post('/payroll/attendance', body).then(unwrap),
  removeAttendance: (id) => client.delete(`/payroll/attendance/${id}`),
  // Xuất file (blob): bảng lương Excel cả kỳ, phiếu lương PDF cá nhân
  exportPeriod: (id) => client.get(`/payroll/periods/${id}/export`, { responseType: 'blob' }).then((r) => r.data),
  payslipPdf: (id) => client.get(`/payroll/payslips/${id}/pdf`, { responseType: 'blob' }).then((r) => r.data),
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
