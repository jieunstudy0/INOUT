/** 레이아웃 헤더 메타(예치금·문의·연차 등) 즉시 갱신용 커스텀 이벤트 */

export const HEADER_REFRESH_EVENT = 'inout:header-refresh';

export function dispatchHeaderRefresh(detail = {}) {
  window.dispatchEvent(new CustomEvent(HEADER_REFRESH_EVENT, { detail }));
}

export function subscribeHeaderRefresh(handler) {
  const listener = (event) => handler(event?.detail || {});
  window.addEventListener(HEADER_REFRESH_EVENT, listener);
  return () => window.removeEventListener(HEADER_REFRESH_EVENT, listener);
}

export function formatWon(value) {
  return `${Number(value || 0).toLocaleString('ko-KR')}원`;
}

export function parseJwtPayload() {
  try {
    const token = localStorage.getItem('accessToken');
    if (!token) return null;
    return JSON.parse(atob(token.split('.')[1]));
  } catch {
    return null;
  }
}

export function resolveRoleFromPayload(payload) {
  const roles = payload?.roles || payload?.auth || '';
  const s = typeof roles === 'string' ? roles : JSON.stringify(roles);
  if (s.includes('ADMIN')) return 'ADMIN';
  if (s.includes('OWNER')) return 'OWNER';
  return 'EMPLOYEE';
}

/** 승인된 연차 일수를 차감해 잔여 연차를 추정 (기본 부여 15일).
 *  @deprecated 서버 `remainingLeaveDays` / `GET /emp/vacation/remaining` 사용 */
export function estimateRemainingLeaveDays(leaves = [], defaultQuota = 15) {
  const used = leaves
    .filter((l) => (l.status || l.leaveStatus) === 'APPROVED')
    .reduce((sum, leave) => {
      if (!leave.startDate || !leave.endDate) return sum;
      const start = new Date(leave.startDate);
      const end = new Date(leave.endDate);
      const days = Math.floor((end - start) / (1000 * 60 * 60 * 24)) + 1;
      return sum + Math.max(days, 0);
    }, 0);
  return Math.max(0, defaultQuota - used);
}
