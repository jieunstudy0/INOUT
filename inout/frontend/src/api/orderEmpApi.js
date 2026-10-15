import client, { unwrap } from './apiClient';

export function getMyOrderHistory() {
  return unwrap(client.get('/emp/orders', { errorContext: '발주 목록을 불러오지 못했습니다.' }));
}

export function getEmpOrderDetail(orderId) {
  return unwrap(client.get(`/emp/orders/${orderId}`, { errorContext: '발주 상세를 불러오지 못했습니다.' }));
}

export function cancelEmpOrder(orderId) {
  return unwrap(client.patch(`/emp/orders/${orderId}/cancel`));
}