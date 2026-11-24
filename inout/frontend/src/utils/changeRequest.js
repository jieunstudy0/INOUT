/** 수정 요청 종류. handler: 처리하는 쪽 (OWNER = 소속 매장 점주, HQ = 본사) — 서버 ChangeRequestCategory와 같다. */
export const REQUEST_CATEGORIES = [
  { value: 'DELIVERY_ISSUE', label: '배송 이상 (수량 부족·파손·오배송·기타)', handler: 'HQ' },
  { value: 'ITEM_INFO', label: '본사 상품 정보 오류', handler: 'HQ' },
  { value: 'STOCK_CORRECTION', label: '매장 재고 정정', handler: 'OWNER' },
  { value: 'ORDER_CHANGE', label: '발주 수정·취소', handler: 'OWNER' },
  { value: 'LEAVE_CHANGE', label: '연차 변경·취소', handler: 'OWNER' },
  { value: 'PROFILE_CHANGE', label: '내 정보 수정', handler: 'OWNER' },
  { value: 'OTHER', label: '기타 매장 요청', handler: 'OWNER' },
];

/**
 * 배송 이상의 세부 종류 — 서버 DeliveryIssueType과 같다.
 * template: 내용 칸에 미리 넣어 두는 안내 (무엇을 적으면 본사가 바로 처리할 수 있는지)
 */
export const DELIVERY_ISSUE_TYPES = [
  { value: 'SHORTAGE', label: '수량 부족', hint: '발주보다 덜 왔어요', template: '덜 온 품목·수량:\n' },
  { value: 'DAMAGED', label: '파손', hint: '깨지거나 상했어요', template: '파손된 품목·수량:\n상태: ' },
  { value: 'WRONG_ITEM', label: '오배송', hint: '다른 상품이 왔어요', template: '받은 상품:\n주문한 상품: ' },
  { value: 'OTHER', label: '기타', hint: '포장·유통기한·지연 등', template: '상황: ' },
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
