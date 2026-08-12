import client, { unwrap } from './apiClient';

export function getOwnerOrders(params = {}) {
  return unwrap(client.get('/owner/orders', { params }));
}

export function getOwnerOrderDetail(orderId) {
  return unwrap(client.get(`/owner/orders/${orderId}`));
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
