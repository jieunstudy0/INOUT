import client, { unwrap } from './apiClient';

export function getOwnerDeliveryList({ status, page = 0, size = 10 } = {}) {
  return unwrap(client.get('/owner/deliveries', {
    params: { status, page, size },
  }));
}
