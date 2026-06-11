import client, { unwrap } from './apiClient';

export function getMyProfile() {
  return unwrap(client.get('/user/profile'));
}

export function updateMyProfile(data) {
  return unwrap(client.put('/user/profile', data));
}

export function updateMyPassword(data) {
  return unwrap(client.patch('/user/profile/password', data));
}