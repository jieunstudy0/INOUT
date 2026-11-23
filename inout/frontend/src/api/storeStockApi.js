import client, { unwrap } from './apiClient';

/**
 * 매장 재고 API. 직원(emp)과 점주(owner)가 같은 화면 부품을 쓰도록 role을 받는다.
 * role: 'emp' | 'owner'
 */
const base = (role) => `/${role}/store-stocks`;

export function getStoreStocks(role) {
  return unwrap(client.get(base(role), { errorContext: '매장 재고를 불러오지 못했습니다.' }));
}

export function getStoreStockHistory(role, storeStockId) {
  return unwrap(client.get(`${base(role)}/${storeStockId}/history`, { errorContext: '재고 이력을 불러오지 못했습니다.' }));
}

export function getPendingReceipts(role) {
  return unwrap(client.get(`${base(role)}/receipts`, { errorContext: '수령 대기 배송을 불러오지 못했습니다.' }));
}

/** lines: [{ orderDetailId, receivedQty }] */
export function receiveDelivery(role, orderId, lines) {
  return unwrap(client.post(`${base(role)}/receipts/${orderId}`, { lines }));
}

// ── 직원 전용 ────────────────────────────────────────────────────────
export function consumeStoreStock(itemId, quantity, memo) {
  return unwrap(client.post('/emp/store-stocks/use', { itemId, quantity, memo }));
}

// ── 점주 전용 ────────────────────────────────────────────────────────
export function getStoreCatalog() {
  return unwrap(client.get('/owner/store-stocks/catalog', { errorContext: '상품 목록을 불러오지 못했습니다.' }));
}

export function adjustStoreStock(itemId, actualQuantity, reason) {
  return unwrap(client.post('/owner/store-stocks/adjust', { itemId, actualQuantity, reason }));
}

export function updateStoreStockSettings(storeStockId, minStockLevel, location) {
  return unwrap(client.patch(`/owner/store-stocks/${storeStockId}/settings`, { minStockLevel, location }));
}
