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

export function unlockOwnerEmployee(id) {
  return unwrap(client.patch(`/owner/users/${id}/unlock`));
}

export function resetOwnerEmployeePassword(id) {
  return unwrap(client.post(`/owner/users/${id}/reset-password`));
}
