import client, { unwrap } from './apiClient';

export function getVacationList(params) {
  return unwrap(client.get('/admin/vacation', { params }));
}

export function getVacationDetail(leaveId) {
  return unwrap(client.get(`/admin/vacation/${leaveId}`));
}

export function processVacation(leaveId, data) {
  return unwrap(client.patch(`/admin/vacation/${leaveId}`, data));
}
