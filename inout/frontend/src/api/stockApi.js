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


