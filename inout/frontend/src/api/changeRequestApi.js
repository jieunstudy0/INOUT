import client, { unwrap } from './apiClient';

/**
 * 수정 요청 남기기. request는 JSON 조각으로, photo는 선택 파일로 함께 보낸다.
 * request: { category, refType, refId, title, content }
 */
export function createChangeRequest(request, photo) {
  const formData = new FormData();
  formData.append('request', new Blob([JSON.stringify(request)], { type: 'application/json' }));
  if (photo) formData.append('photo', photo);
  return unwrap(client.post('/change-requests', formData, { headers: { 'Content-Type': 'multipart/form-data' } }));
}

export function getMyChangeRequests(page = 0) {
  return unwrap(client.get('/change-requests/mine', { errorContext: '보낸 요청을 불러오지 못했습니다.', params: { page } }));
}

export function getChangeRequestInbox(status, page = 0) {
  return unwrap(client.get('/change-requests/inbox', {
    errorContext: '받은 요청을 불러오지 못했습니다.',
    params: { status: status || undefined, page },
  }));
}

export function changeRequestStatus(requestId, status, message) {
  return unwrap(client.patch(`/change-requests/${requestId}/status`, { status, message }));
}
