import client, { unwrap } from './apiClient';

// 전체 예치금 내역 및 요약 정보 조회
export function getAdminDepositList(params) {
  return unwrap(client.get('/admin/deposits', { params }));
}

// 관리자 수동 충전 API
export function adminChargeDeposit(data) {
  return unwrap(client.post('/admin/deposits/charge', data));
}

// 셀렉트 박스용 가맹점 목록 조회 API
export function getFranchiseeUserList() {
  return unwrap(client.get('/admin/deposits/franchisees'));
}