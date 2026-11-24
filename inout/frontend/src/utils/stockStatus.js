/** 재고 수량으로 상태를 정한다: 0이면 품절, 안전재고 이하면 부족 */
export function stockTone(qty, min) {
  if (qty <= 0) return 'out';
  if (qty <= min) return 'low';
  return 'normal';
}

/** 상품 카드 상태 표시 (연한 바탕 + 같은 계열 글자) */
export function stockBadge(qty, min) {
  const tone = stockTone(qty, min);
  if (tone === 'out') return { label: '품절', tone: 'red' };
  if (tone === 'low') return { label: '부족', tone: 'orange' };
  return { label: '정상', tone: 'teal' };
}

export const BADGE_TONE_CLS = {
  navy: 'bg-indigo-50 text-indigo-600',
  teal: 'bg-emerald-100 text-emerald-600',
  orange: 'bg-amber-100 text-amber-600',
  red: 'bg-rose-50 text-rose-600',
  gray: 'bg-slate-100 text-slate-500',
};
