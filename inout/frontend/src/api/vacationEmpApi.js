import client, { unwrap } from './apiClient';

export function submitLeave(data) {
  return unwrap(client.post('/emp/vacation', data));
}

export function getMyLeaveList(params) {
  return unwrap(client.get('/emp/vacation', { errorContext: '연차 신청 목록을 불러오지 못했습니다.', params }));
}

export function getMyLeaveDetail(leaveId) {
  return unwrap(client.get(`/emp/vacation/${leaveId}`, { errorContext: '연차 신청 상세를 불러오지 못했습니다.' }));
}

export function getRemainingLeaveDays() {
  return unwrap(client.get('/emp/vacation/remaining', { errorContext: '잔여 연차를 불러오지 못했습니다.' }));
}
