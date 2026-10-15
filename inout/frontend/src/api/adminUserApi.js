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