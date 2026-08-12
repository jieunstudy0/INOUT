import client, { unwrap } from './apiClient';

export function getVacationList(params) {
  return unwrap(client.get('/owner/vacation', { params }));
}

export function getVacationDetail(leaveId) {
  return unwrap(client.get(`/owner/vacation/${leaveId}`));
}

export function processVacation(leaveId, data) {
  return unwrap(client.patch(`/owner/vacation/${leaveId}`, data));
}
