import client, { unwrap } from './apiClient';

export function getUserList(params) {
  return unwrap(client.get('/admin/users', { errorContext: '회원 목록을 불러오지 못했습니다.', params }));
}

export function unlockUserAccount(userId) {
  return unwrap(client.patch(`/admin/employees/${userId}/unlock`));
}

export function updateUserByAdmin(userId, data) {
  return unwrap(client.put(`/admin/users/${userId}`, data));
}

export function sendPasswordResetEmail(data) {
  return unwrap(client.post('/auth/password/reset', data));
}
export function getUserDetail(userId) {
  return unwrap(client.get(`/admin/users/${userId}`, { errorContext: '회원 정보를 불러오지 못했습니다.' }));
}

/** 이름·연락처·사진 */
export function updateUserBasicInfo(userId, data) {
  return unwrap(client.put(`/admin/users/${userId}/basic-info`, data));
}

export function getStores() {
  return unwrap(client.get('/admin/users/stores', { errorContext: '매장 목록을 불러오지 못했습니다.' }));
}

/** 회원 이메일로 비밀번호 재설정 링크 발송 (본사) */
export function sendPasswordResetByAdmin(userId) {
  return unwrap(client.post(`/admin/users/${userId}/reset-password`));
}
