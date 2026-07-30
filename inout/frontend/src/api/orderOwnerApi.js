import client, { unwrap } from './apiClient';

export function getOwnerOrders(params = {}) {
  return unwrap(client.get('/owner/orders', { params }));
}

export function getOwnerOrderDetail(orderId) {
  return unwrap(client.get(`/owner/orders/${orderId}`));
}
