import client, { unwrap } from './apiClient';

export function getVacationList(params) {
  return unwrap(client.get('/owner/vacation', { errorContext: '연차 신청 목록을 불러오지 못했습니다.', params }));
}

export function getVacationDetail(leaveId) {
  return unwrap(client.get(`/owner/vacation/${leaveId}`, { errorContext: '연차 신청 상세를 불러오지 못했습니다.' }));
}

export function processVacation(leaveId, data) {
  return unwrap(client.patch(`/owner/vacation/${leaveId}`, data));
}
