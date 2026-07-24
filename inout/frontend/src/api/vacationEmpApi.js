import client, { unwrap } from './apiClient';

export function submitLeave(data) {
  return unwrap(client.post('/emp/vacation', data));
}

export function getMyLeaveList(params) {
  return unwrap(client.get('/emp/vacation', { params }));
}

export function getMyLeaveDetail(leaveId) {
  return unwrap(client.get(`/emp/vacation/${leaveId}`));
}
