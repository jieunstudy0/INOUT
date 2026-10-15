import client, { unwrap } from './apiClient';

export function getOwnerDepositHistory(page = 0, size = 10) {
  return unwrap(client.get('/owner/deposit', { errorContext: '예치금 내역을 불러오지 못했습니다.', params: { page, size } }));
}

export function requestOwnerCharge(data) {
  return unwrap(client.post('/owner/charges', data));
}

export function getOwnerChargeRequests() {
  return unwrap(client.get('/owner/charges', { errorContext: '충전 신청 내역을 불러오지 못했습니다.' }));
}
