import client, { unwrap } from './apiClient';

export function unlockUser(userId) {
  return unwrap(client.patch(`/admin/users/${userId}/unlock`));
}