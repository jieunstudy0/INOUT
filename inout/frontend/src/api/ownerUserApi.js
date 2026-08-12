import client, { unwrap } from './apiClient';

export function getOwnerUsers(params) {
  return unwrap(client.get('/owner/users', { params }));
}

export function createOwnerEmployee(data) {
  return unwrap(client.post('/owner/users', data));
}

export function updateOwnerEmployee(id, data) {
  return unwrap(client.put(`/owner/users/${id}`, data));
}

/** 직원 계정 상태 변경 — ACTIVE / ON_LEAVE / RESIGNED */
export function updateOwnerEmployeeStatus(id, status) {
  return unwrap(client.patch(`/owner/employees/${id}/status`, { status }));
}

/** 1일 예치금 사용 한도 설정 (null = 무제한) */
export function updateOwnerEmployeeDepositLimit(id, dailyDepositLimit) {
  return unwrap(client.patch(`/owner/employees/${id}/deposit-limit`, { dailyDepositLimit }));
}

export function unlockOwnerEmployee(id) {
  return unwrap(client.patch(`/owner/users/${id}/unlock`));
}

export function resetOwnerEmployeePassword(id) {
  return unwrap(client.post(`/owner/users/${id}/reset-password`));
}
