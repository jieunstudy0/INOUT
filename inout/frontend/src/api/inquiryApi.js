import client, { unwrap } from './apiClient';

// ── 공통 ───────────────────────────────────────────────────────────────────────
export function getInquiryDetail(inquiryId) {
  return unwrap(client.get(`/inquiry/${inquiryId}`, { errorContext: '문의 상세를 불러오지 못했습니다.' }));
}

export function createInquiry(formData) {
  return unwrap(
    client.post('/inquiry', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
  );
}

export function deleteInquiry(inquiryId) {
  return unwrap(client.delete(`/inquiry/${inquiryId}`));
}

export function downloadInquiryFile(inquiryId) {
  return client.get(`/inquiry/${inquiryId}/download`, { errorContext: '첨부파일을 내려받지 못했습니다.', responseType: 'blob' });
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

// ── EMP 탭별 ───────────────────────────────────────────────────────────────────
export function getEmpInquiriesToAdmin(page = 0, size = 10) {
  return unwrap(client.get('/emp/inquiries/to-admin', { errorContext: '문의 목록을 불러오지 못했습니다.', params: { page, size } }));
}

export function getEmpInquiriesToOwner(page = 0, size = 10) {
  return unwrap(client.get('/emp/inquiries/to-owner', { errorContext: '문의 목록을 불러오지 못했습니다.', params: { page, size } }));
}

// ── OWNER 탭별 ─────────────────────────────────────────────────────────────────
export function getOwnerInquiriesFromStaff(page = 0, size = 10) {
  return unwrap(client.get('/owner/inquiries/from-staff', { errorContext: '문의 목록을 불러오지 못했습니다.', params: { page, size } }));
}

export function getOwnerInquiriesToAdmin(page = 0, size = 10) {
  return unwrap(client.get('/owner/inquiries/to-admin', { errorContext: '문의 목록을 불러오지 못했습니다.', params: { page, size } }));
}

// ── ADMIN 탭별 ─────────────────────────────────────────────────────────────────
export function getAdminInquiriesFromOwners(page = 0, size = 10) {
  return unwrap(client.get('/admin/inquiries/from-owners', { errorContext: '문의 목록을 불러오지 못했습니다.', params: { page, size } }));
}

export function getAdminInquiriesFromEmployees(page = 0, size = 10) {
  return unwrap(client.get('/admin/inquiries/from-employees', { errorContext: '문의 목록을 불러오지 못했습니다.', params: { page, size } }));
}

// 하위 호환: 기존 전체 목록 조회 (deprecated — 탭별 조회로 대체됨)
export function getInquiryList(page = 0, size = 10) {
  return unwrap(client.get('/inquiry', { errorContext: '문의 목록을 불러오지 못했습니다.', params: { page, size } }));
}