import client, { unwrap } from './apiClient';

export function getOwnerDashboardSummary() {
  return unwrap(client.get('/owner/dashboard/summary'));
}
