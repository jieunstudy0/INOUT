import client, { unwrap } from './apiClient';

// 전체 예치금 내역 및 요약 정보 조회
export function getAdminDepositList(params) {
  return unwrap(client.get('/admin/deposits', { params }));
}

// 관리자 수동 충전 API (본사 직접 지급)
export function adminChargeDeposit(data) {
  return unwrap(client.post('/admin/deposits/charge', data));
}

// 셀렉트 박스용 가맹점 목록 조회 API
export function getFranchiseeUserList() {
  return unwrap(client.get('/admin/deposits/franchisees'));
}

/** 승인 대기 중인 충전 신청 목록 (OWNER 신청분) */
export function getPendingChargeRequests() {
  return unwrap(client.get('/admin/charges/pending'));
}

/** 충전 신청 승인 → 매장 예치금 반영 */
export function approveChargeRequest(chargeId) {
  return unwrap(client.patch(`/admin/charges/${chargeId}/approve`));
}

/** 충전 신청 반려 */
export function rejectChargeRequest(chargeId, reason) {
  return unwrap(client.patch(`/admin/charges/${chargeId}/reject`, { reason: reason || '본사 검토 후 반려' }));
}
