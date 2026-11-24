import client, { unwrap } from './apiClient';

// ── 직원: 실사표 출력 · 사진 읽기 · 제출 ──────────────────────────────────────
export function getStockCountSheets() {
  return unwrap(client.get('/emp/stock-counts', { errorContext: '실사표 목록을 불러오지 못했습니다.' }));
}

export function createStockCountSheet() {
  return unwrap(client.post('/emp/stock-counts'));
}

export function getStockCountSheet(sheetId) {
  return unwrap(client.get(`/emp/stock-counts/${sheetId}`, { errorContext: '실사표를 불러오지 못했습니다.' }));
}

/** 펜으로 적은 영수증 사진을 올리면 AI가 실재고를 읽어 채운 실사표를 돌려준다. */
export function scanStockCountPhoto(sheetId, file) {
  const formData = new FormData();
  formData.append('photo', file);
  return unwrap(
    client.post(`/emp/stock-counts/${sheetId}/scan`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      timeout: 90_000,
    })
  );
}

/** lines: [{ lineId, countedQty(null이면 세지 않음) }] */
export function submitStockCount(sheetId, lines) {
  return unwrap(client.post(`/emp/stock-counts/${sheetId}/submit`, { lines }));
}

// ── 점주: 확인 · 승인 · 반려 (매장 재고 실사는 소속 매장 점주가 승인한다) ─────────────────────────────────────────────────
export function getOwnerStockCountSheets(status) {
  return unwrap(
    client.get('/owner/stock-counts', {
      errorContext: '실사표 목록을 불러오지 못했습니다.',
      params: { status: status || undefined },
    })
  );
}

export function getOwnerStockCountSheet(sheetId) {
  return unwrap(client.get(`/owner/stock-counts/${sheetId}`, { errorContext: '실사표를 불러오지 못했습니다.' }));
}

export function approveStockCount(sheetId) {
  return unwrap(client.post(`/owner/stock-counts/${sheetId}/approve`));
}

export function rejectStockCount(sheetId, reason) {
  return unwrap(client.post(`/owner/stock-counts/${sheetId}/reject`, { reason }));
}
