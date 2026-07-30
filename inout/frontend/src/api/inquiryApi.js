import client, { unwrap } from './apiClient';

export function getInquiryList(page = 0, size = 10) {
  return unwrap(client.get('/inquiry', { params: { page, size } }));
}

export function getInquiryDetail(inquiryId) {
  return unwrap(client.get(`/inquiry/${inquiryId}`));
}

export function createInquiry(formData) {
  return unwrap(
    client.post('/inquiry', formData, {
      headers: {
        'Content-Type': 'multipart/form-data', 
      },
    })
  );
}

export function deleteInquiry(inquiryId) {
  return unwrap(client.delete(`/inquiry/${inquiryId}`));
}

export function createComment(inquiryId, data) {
  return unwrap(client.post(`/inquiry/${inquiryId}/comments`, data));
}

export function updateComment(inquiryId, commentId, content) {
  return unwrap(client.put(`/inquiry/${inquiryId}/comments/${commentId}`, { content }));
}

export function deleteComment(inquiryId, commentId) {
  return unwrap(client.delete(`/inquiry/${inquiryId}/comments/${commentId}`));
}

/** 문의 첨부파일 다운로드 (blob) */
export function downloadInquiryFile(inquiryId) {
  return client.get(`/inquiry/${inquiryId}/download`, { responseType: 'blob' });
}