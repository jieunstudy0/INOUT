import client, { unwrap } from './apiClient';

export function getOwnerDashboardSummary() {
  return unwrap(client.get('/owner/dashboard/summary', { errorContext: '대시보드를 불러오지 못했습니다.' }));
}
