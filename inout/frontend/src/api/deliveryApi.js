import client, { unwrap } from './apiClient';

export function getDeliveryList({ status, page = 0, size = 10 } = {}) {
  const params = { page, size };
  if (status) params.status = status;
  return unwrap(client.get('/admin/deliveries', { params }));
}

export function getDeliveryByOrder(orderId) {
  return unwrap(client.get(`/admin/deliveries/orders/${orderId}`));
}

/** 택배사 연동 Mock 운송장 발급 (약 1초 지연) */
export function generateWaybill(deliveryId) {
  return unwrap(client.post(`/admin/deliveries/${deliveryId}/generate-waybill`));
}

export function startShipping(orderId, trackingNumber) {
  return unwrap(client.patch(`/admin/deliveries/orders/${orderId}/start`, { trackingNumber }));
}

export function completeDelivery(orderId) {
  return unwrap(client.patch(`/admin/deliveries/orders/${orderId}/complete`));
}

/** 배송 조회 프록시 (실패 시 서버 Mock Fallback) */
export function trackDelivery({ carrier, trackingNumber }) {
  return unwrap(client.get('/deliveries/tracking', {
    params: { carrier: carrier || 'CJ대한통운', trackingNumber },
  }));
}
