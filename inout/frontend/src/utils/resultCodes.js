/**
 * 백엔드 ErrorCode(resultCode) 중 화면이 직접 대응해야 하는 것들.
 * 문구 대신 코드로 분기하므로 백엔드 문구가 바뀌어도 동작이 유지된다.
 */

/** 다른 사용자가 먼저 처리했거나 재고가 바뀌어 화면 데이터가 낡았음을 뜻한다 → 화면을 다시 불러온다 */
const STALE_DATA_CODES = new Set([
  'CONCURRENCY_ERROR',
  'ALREADY_PROCESSED',
  'ALREADY_PROCESSED_DETAIL',
  'INVALID_ORDER_STATUS',
  'STOCK_SHORTAGE',
]);

export function isStaleDataError(err) {
  return STALE_DATA_CODES.has(err?.resultCode);
}

/** 비밀번호 재설정 링크를 더 이상 쓸 수 없음 → 재설정 메일을 다시 받아야 한다 */
export function isUnusableResetLink(err) {
  return err?.resultCode === 'EXPIRED_LINK' || err?.resultCode === 'INVALID_LINK';
}
