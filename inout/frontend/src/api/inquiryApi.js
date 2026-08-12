import client, { unwrap } from './apiClient';

// ── 공통 ───────────────────────────────────────────────────────────────────────
export function getInquiryDetail(inquiryId) {
  return unwrap(client.get(`/inquiry/${inquiryId}`));
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
  return client.get(`/inquiry/${inquiryId}/download`, { responseType: 'blob' });
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
  return unwrap(client.get('/emp/inquiries/to-admin', { params: { page, size } }));
}

export function getEmpInquiriesToOwner(page = 0, size = 10) {
  return unwrap(client.get('/emp/inquiries/to-owner', { params: { page, size } }));
}

// ── OWNER 탭별 ─────────────────────────────────────────────────────────────────
export function getOwnerInquiriesFromStaff(page = 0, size = 10) {
  return unwrap(client.get('/owner/inquiries/from-staff', { params: { page, size } }));
}

export function getOwnerInquiriesToAdmin(page = 0, size = 10) {
  return unwrap(client.get('/owner/inquiries/to-admin', { params: { page, size } }));
}

// ── ADMIN 탭별 ─────────────────────────────────────────────────────────────────
export function getAdminInquiriesFromOwners(page = 0, size = 10) {
  return unwrap(client.get('/admin/inquiries/from-owners', { params: { page, size } }));
}

export function getAdminInquiriesFromEmployees(page = 0, size = 10) {
  return unwrap(client.get('/admin/inquiries/from-employees', { params: { page, size } }));
}

// 하위 호환: 기존 전체 목록 조회 (deprecated — 탭별 조회로 대체됨)
export function getInquiryList(page = 0, size = 10) {
  return unwrap(client.get('/inquiry', { params: { page, size } }));
}