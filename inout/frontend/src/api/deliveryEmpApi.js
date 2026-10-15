import client, { unwrap } from './apiClient';

export function getEmpDeliveryList({ status, page = 0, size = 10 } = {}) {
  const params = { page, size };
  if (status) params.status = status;
  return unwrap(client.get('/emp/deliveries', { errorContext: '배송 목록을 불러오지 못했습니다.', params }));
}
