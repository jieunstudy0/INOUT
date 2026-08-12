import client, { unwrap } from './apiClient';

export function getMyOrderHistory() {
  return unwrap(client.get('/emp/orders'));
}

export function getEmpOrderDetail(orderId) {
  return unwrap(client.get(`/emp/orders/${orderId}`));
}

export function cancelEmpOrder(orderId) {
  return unwrap(client.patch(`/emp/orders/${orderId}/cancel`));
}