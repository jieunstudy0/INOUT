import client, { unwrap } from './apiClient';

/** 매장 예치금 잔액·거래 내역 조회 (직원 — 조회 전용) */
export function getMyDepositHistory(page = 0, size = 10) {
  return unwrap(client.get('/emp/deposit', { errorContext: '예치금 내역을 불러오지 못했습니다.', params: { page, size } }));
}
