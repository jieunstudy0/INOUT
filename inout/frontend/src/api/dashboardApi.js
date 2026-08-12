import client, { unwrap } from './apiClient';


export function getDashboardSummary() {
  return unwrap(client.get('/dashboard/summary'));
}
