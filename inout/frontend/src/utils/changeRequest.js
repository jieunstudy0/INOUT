/** 수정 요청 종류. handler: 처리하는 쪽 (OWNER = 소속 매장 점주, HQ = 본사) — 서버 ChangeRequestCategory와 같다. */
export const REQUEST_CATEGORIES = [
  { value: 'DELIVERY_ISSUE', label: '배송 이상 (덜 옴·파손·오배송)', handler: 'HQ' },
  { value: 'ITEM_INFO', label: '본사 상품 정보 오류', handler: 'HQ' },
  { value: 'STOCK_CORRECTION', label: '매장 재고 정정', handler: 'OWNER' },
  { value: 'ORDER_CHANGE', label: '발주 수정·취소', handler: 'OWNER' },
  { value: 'LEAVE_CHANGE', label: '연차 변경·취소', handler: 'OWNER' },
  { value: 'PROFILE_CHANGE', label: '내 정보 수정', handler: 'OWNER' },
  { value: 'OTHER', label: '기타 매장 요청', handler: 'OWNER' },
];

export const REQUEST_STATUS_META = {
  REQUESTED: { label: '접수', cls: 'bg-sky-50 text-sky-700 border-sky-200' },
  IN_PROGRESS: { label: '처리 중', cls: 'bg-amber-50 text-amber-700 border-amber-200' },
  DONE: { label: '완료', cls: 'bg-emerald-50 text-emerald-700 border-emerald-200' },
  REJECTED: { label: '반려', cls: 'bg-rose-50 text-rose-700 border-rose-200' },
};

/** role: 'emp' | 'owner' — 점주는 본사에 보내는 종류만 고를 수 있다. */
export function categoriesFor(role) {
  return role === 'owner' ? REQUEST_CATEGORIES.filter((c) => c.handler === 'HQ') : REQUEST_CATEGORIES;
}
