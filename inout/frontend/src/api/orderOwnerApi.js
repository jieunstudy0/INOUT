import client, { unwrap } from './apiClient';

export function getOwnerOrders(params = {}) {
  return unwrap(client.get('/owner/orders', { errorContext: '발주 목록을 불러오지 못했습니다.', params }));
}

export function getOwnerOrderDetail(orderId) {
  return unwrap(client.get(`/owner/orders/${orderId}`, { errorContext: '발주 상세를 불러오지 못했습니다.' }));
}

export function createOwnerOrder(body) {
  return unwrap(client.post('/owner/orders', body));
}

export function modifyAndApproveOwnerOrder(orderId, body) {
  return unwrap(client.patch(`/owner/orders/${orderId}/modify-and-approve`, body));
}

export function rejectOwnerDraft(orderId, body = {}) {
  return unwrap(client.patch(`/owner/orders/${orderId}/reject`, body));
}
