import client, { unwrap } from './apiClient';

export function getDeliveryList({ status, page = 0, size = 10 } = {}) {
  const params = { page, size };
  if (status) params.status = status;
  return unwrap(client.get('/admin/deliveries', { params }));
}


export function getDeliveryByOrder(orderId) {
  return unwrap(client.get(`/admin/deliveries/orders/${orderId}`));
}


export function startShipping(orderId, trackingNumber) {
  return unwrap(client.patch(`/admin/deliveries/orders/${orderId}/start`, { trackingNumber }));
}


export function completeDelivery(orderId) {
  return unwrap(client.patch(`/admin/deliveries/orders/${orderId}/complete`));
}
