import client, { unwrap } from './apiClient';

export function getOwnerDeliveryList({ status, page = 0, size = 10 } = {}) {
  return unwrap(client.get('/owner/deliveries', {
    errorContext: '배송 목록을 불러오지 못했습니다.',
    params: { status, page, size },
  }));
}
