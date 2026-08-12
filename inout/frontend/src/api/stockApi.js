import client, { unwrap } from './apiClient';

export function getList(params) {
  return unwrap(client.get('/admin/stocks', { params }));
}

export function getHistory(itemId, page = 0, size = 20) {
  return unwrap(
    client.get(`/admin/stocks/${itemId}/history`, { params: { page, size } })
  );
}

export function getLowStockAlerts() {
  return unwrap(client.get('/admin/stocks/alerts/low-stock'));
}

export function adjustStock(itemId, adjustedQuantity, reason) {
  return unwrap(client.patch(`/admin/stocks/${itemId}/adjust`, { adjustedQuantity, reason }));
}

export function registerStock(data) {
  return unwrap(client.post('/admin/stocks', data));
}


