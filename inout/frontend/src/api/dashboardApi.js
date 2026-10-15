import client, { unwrap } from './apiClient';


export function getDashboardSummary() {
  return unwrap(client.get('/dashboard/summary', { errorContext: '대시보드를 불러오지 못했습니다.' }));
}
