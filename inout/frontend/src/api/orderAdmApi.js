import client, { unwrap } from './apiClient';


function getList(status) {
  const params = status ? { status } : {};
  return unwrap(client.get('/admin/orders', { params }));
}


export function getListByStatuses(statuses) {
  if (!Array.isArray(statuses) || statuses.length === 0) return getList(null);
  if (statuses.length === 1) return getList(statuses[0]);
  return Promise.all(statuses.map((s) => getList(s))).then((results) => {
    const merged = results.reduce((acc, r) => acc.concat(Array.isArray(r) ? r : []), []);
    merged.sort((a, b) => new Date(b.requestDate) - new Date(a.requestDate));
    return merged;
  });
}

export function getDetail(orderId) {
  return unwrap(client.get(`/admin/orders/${orderId}`));
}


export function bulkApprove(orderIds) {
  return unwrap(client.post('/admin/orders/bulk-approve', { orderIds }));
}


export function processItems(orderId, items) {
  return unwrap(client.patch(`/admin/orders/${orderId}/process`, { items }));
}
