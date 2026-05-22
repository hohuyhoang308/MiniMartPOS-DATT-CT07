import client, { unwrap } from './client'

/** Sản phẩm, danh mục, đơn vị, nhà cung cấp (FR2, FR3). */
export const productApi = {
  list: (params) => client.get('/products', { params }).then(unwrap),
  get: (id) => client.get(`/products/${id}`).then(unwrap),
  byBarcode: (code) => client.get(`/products/barcode/${code}`).then(unwrap),
  create: (body) => client.post('/products', body).then(unwrap),
  update: (id, body) => client.put(`/products/${id}`, body).then(unwrap),
  remove: (id) => client.delete(`/products/${id}`),
}

export const categoryApi = {
  list: () => client.get('/categories').then(unwrap),
  create: (body) => client.post('/categories', body).then(unwrap),
  update: (id, body) => client.put(`/categories/${id}`, body).then(unwrap),
  remove: (id) => client.delete(`/categories/${id}`),
}

export const unitApi = {
  list: () => client.get('/units').then(unwrap),
  create: (body) => client.post('/units', body).then(unwrap),
  update: (id, body) => client.put(`/units/${id}`, body).then(unwrap),
  remove: (id) => client.delete(`/units/${id}`),
}

export const supplierApi = {
  list: () => client.get('/suppliers').then(unwrap),
  create: (body) => client.post('/suppliers', body).then(unwrap),
  update: (id, body) => client.put(`/suppliers/${id}`, body).then(unwrap),
  remove: (id) => client.delete(`/suppliers/${id}`),
}

export const receiptApi = {
  list: () => client.get('/goods-receipts').then(unwrap),
  get: (id) => client.get(`/goods-receipts/${id}`).then(unwrap),
  create: (body) => client.post('/goods-receipts', body).then(unwrap),
}
