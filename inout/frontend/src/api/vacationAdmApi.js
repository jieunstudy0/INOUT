import client, { unwrap } from './apiClient';

/** 본사 연차 모니터링 (조회 전용 — 승인/반려 API 없음) */
export function getVacationList(params) {
  return unwrap(client.get('/admin/vacation', { params }));
}

export function getVacationDetail(leaveId) {
  return unwrap(client.get(`/admin/vacation/${leaveId}`));
}
