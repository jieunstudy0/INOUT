import client, { unwrap } from './apiClient';

export function getEmpDashboardSummary() {
  return unwrap(client.get('/emp/dashboard/summary')); 
}