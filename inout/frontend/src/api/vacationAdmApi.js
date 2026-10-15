import client, { unwrap } from './apiClient';

/** 본사 연차 모니터링 (조회 전용 — 승인/반려 API 없음) */
export function getVacationList(params) {
  return unwrap(client.get('/admin/vacation', { errorContext: '연차 신청 목록을 불러오지 못했습니다.', params }));
}

export function getVacationDetail(leaveId) {
  return unwrap(client.get(`/admin/vacation/${leaveId}`, { errorContext: '연차 신청 상세를 불러오지 못했습니다.' }));
}
