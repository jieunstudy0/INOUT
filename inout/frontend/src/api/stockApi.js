import client, { unwrap } from './apiClient';

export function getList(params) {
  return unwrap(client.get('/admin/stocks', { errorContext: '재고 목록을 불러오지 못했습니다.', params }));
}

export function getHistory(itemId, page = 0, size = 20) {
  return unwrap(
    client.get(`/admin/stocks/${itemId}/history`, { errorContext: '재고 이력을 불러오지 못했습니다.', params: { page, size } })
  );
}

export function getLowStockAlerts() {
  return unwrap(client.get('/admin/stocks/alerts/low-stock', { errorContext: '저재고 알림을 불러오지 못했습니다.' }));
}

export function adjustStock(itemId, adjustedQuantity, reason) {
  return unwrap(client.patch(`/admin/stocks/${itemId}/adjust`, { adjustedQuantity, reason }));
}

export function registerStock(data) {
  return unwrap(client.post('/admin/stocks', data));
}



/** 상품 상세 (편집 화면). page·size는 이력 페이지 */
export function getStockDetail(itemId, page = 0, size = 20) {
  return unwrap(client.get(`/admin/stocks/${itemId}`, { errorContext: '상품 정보를 불러오지 못했습니다.', params: { page, size } }));
}

export function updateStock(itemId, data) {
  return unwrap(client.put(`/admin/stocks/${itemId}`, data));
}

export function deactivateStock(itemId) {
  return unwrap(client.delete(`/admin/stocks/${itemId}`));
}

export function receiveStock(itemId, quantity, memo) {
  return unwrap(client.post('/admin/stocks/receive', { itemId, quantity, memo }));
}

export function getCategories() {
  return unwrap(client.get('/admin/stocks/categories', { errorContext: '카테고리를 불러오지 못했습니다.' }));
}

/** 본사 사진 업로드 → 저장 경로(/uploads/...) */
export function uploadAdminImage(file) {
  const formData = new FormData();
  formData.append('file', file);
  return unwrap(client.post('/admin/images/upload', formData, { headers: { 'Content-Type': 'multipart/form-data' } }));
}
