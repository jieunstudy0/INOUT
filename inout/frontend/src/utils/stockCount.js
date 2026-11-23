/** 재고 실사 화면(직원·본사)이 함께 쓰는 표시용 값 */
export const STOCK_COUNT_STATUS = {
  OPEN: { label: '작성 중', cls: 'bg-sky-50 text-sky-700 border-sky-200' },
  SUBMITTED: { label: '승인 대기', cls: 'bg-amber-50 text-amber-700 border-amber-200' },
  APPLIED: { label: '반영 완료', cls: 'bg-emerald-50 text-emerald-700 border-emerald-200' },
};

export const REASON_TEXT = { LOW_STOCK: '재고 부족', RECENT_USE: '최근 사용' };

export function formatDateTime(value) {
  if (!value) return '-';
  return new Date(value).toLocaleString('ko-KR', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' });
}
