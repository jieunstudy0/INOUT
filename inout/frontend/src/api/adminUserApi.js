import client, { unwrap } from './apiClient';

export function getUserList(params) {
  return unwrap(client.get('/admin/users', { params }));
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